package com.hamradio.rigcontrol.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.rigcontrol.service.RigService;
import com.hamradio.rigcontrol.dto.RigStatus;
import com.hamradio.rigcontrol.ptt.PTTLockManager;
import com.hamradio.rigcontrol.websocket.RigEventsHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Outbound WebSocket client that connects this rig service to the EtherWave Archive
 * backend station gateway (/ws/station-gateway).
 *
 * When cloud.relay.gateway-url is set the rig service registers itself as a remote
 * station so that backend users can control it over the internet without opening
 * inbound firewall ports on the station PC.
 *
 * Protocol (rig service -> gateway):
 *   {"type": "register", "stationId": 1, "apiKey": "..."}
 *   {"type": "heartbeat", "stationId": 1}
 *   {"type": "response",  "id": "req-1", "success": true, "result": {...}, "message": "..."}
 *   {"type": "status",    "stationId": 1, "status": {...}}
 *
 * Protocol (gateway -> rig service):
 *   {"id": "req-1", "command": "setFrequency", "params": {"hz": 14250000}}
 */
@Component
@Slf4j
public class CloudRelayClient {

    @Value("${cloud.relay.gateway-url:}")
    private String gatewayUrl;

    @Value("${cloud.relay.station-id:0}")
    private long stationId;

    @Value("${cloud.relay.api-key:}")
    private String apiKey;

    @Value("${cloud.relay.status-interval-ms:1000}")
    private long statusIntervalMs;

    @Value("${cloud.relay.heartbeat-interval-ms:30000}")
    private long heartbeatIntervalMs;

    @Value("${cloud.relay.reconnect-delay-ms:5000}")
    private long reconnectDelayMs;

    private final RigService rigService;
    private final PTTLockManager pttLockManager;
    private final RigEventsHandler eventsHandler;
    private final ObjectMapper objectMapper;

    private final StandardWebSocketClient wsClient = new StandardWebSocketClient();
    private volatile WebSocketSession gatewaySession;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    private ScheduledExecutorService scheduler;

    public CloudRelayClient(RigService rigService, PTTLockManager pttLockManager,
                            RigEventsHandler eventsHandler, ObjectMapper objectMapper) {
        this.rigService = rigService;
        this.pttLockManager = pttLockManager;
        this.eventsHandler = eventsHandler;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            log.info("Cloud relay disabled (cloud.relay.gateway-url not set)");
            return;
        }
        if (stationId <= 0) {
            log.warn("Cloud relay: cloud.relay.station-id not set — relay disabled");
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Cloud relay: cloud.relay.api-key not set — relay disabled");
            return;
        }

        log.info("Cloud relay enabled — gateway: {}, stationId: {}", gatewayUrl, stationId);
        running.set(true);

        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "cloud-relay");
            t.setDaemon(true);
            return t;
        });

        // Initial connection attempt
        scheduler.execute(this::connect);

        // Status forwarding
        scheduler.scheduleAtFixedRate(this::forwardStatus, statusIntervalMs, statusIntervalMs, TimeUnit.MILLISECONDS);

        // Heartbeat
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
    }

    // -------------------------------------------------------------------------
    // Connection management
    // -------------------------------------------------------------------------

    private void connect() {
        if (!running.get()) {
            return;
        }

        try {
            log.info("Cloud relay: connecting to {}", gatewayUrl);
            GatewayHandler handler = new GatewayHandler();
            wsClient.execute(handler, gatewayUrl).whenComplete((session, error) -> {
                if (error != null) {
                    log.warn("Cloud relay: connection failed — {}", error.getMessage());
                    scheduleReconnect();
                } else {
                    gatewaySession = session;
                    log.info("Cloud relay: connected — sending registration");
                    sendRegistration(session);
                }
            });
        } catch (Exception e) {
            log.error("Cloud relay: connect error", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (running.get()) {
            log.info("Cloud relay: reconnecting in {}ms", reconnectDelayMs);
            scheduler.schedule(this::connect, reconnectDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    // -------------------------------------------------------------------------
    // Outgoing messages
    // -------------------------------------------------------------------------

    private void sendRegistration(WebSocketSession session) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "register");
            msg.put("stationId", stationId);
            msg.put("apiKey", apiKey);
            send(session, msg);
        } catch (Exception e) {
            log.error("Cloud relay: failed to send registration", e);
        }
    }

    private void sendHeartbeat() {
        WebSocketSession session = gatewaySession;
        if (session == null || !session.isOpen() || !registered.get()) {
            return;
        }
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "heartbeat");
            msg.put("stationId", stationId);
            send(session, msg);
        } catch (Exception e) {
            log.warn("Cloud relay: heartbeat failed — {}", e.getMessage());
        }
    }

    private void forwardStatus() {
        WebSocketSession session = gatewaySession;
        if (session == null || !session.isOpen() || !registered.get()) {
            return;
        }
        try {
            RigStatus status = rigService.getRigStatus();
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "status");
            msg.put("stationId", stationId);
            msg.put("status", status);
            send(session, msg);
        } catch (Exception e) {
            log.warn("Cloud relay: status forward failed — {}", e.getMessage());
        }
    }

    /**
     * Send a command response back to the gateway.
     */
    private void sendCommandResponse(WebSocketSession session, String requestId,
                                      boolean success, Map<String, Object> result, String message) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "response");
            msg.put("id", requestId != null ? requestId : "");
            msg.put("success", success);
            msg.put("result", result != null ? result : Map.of());
            msg.put("message", message);
            send(session, msg);
        } catch (Exception e) {
            log.error("Cloud relay: failed to send command response", e);
        }
    }

    private void send(WebSocketSession session, Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        synchronized (session) {
            session.sendMessage(new TextMessage(json));
        }
    }

    // -------------------------------------------------------------------------
    // Incoming command dispatch
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void dispatchCommand(WebSocketSession session, Map<String, Object> msg) {
        String requestId = (String) msg.get("id");
        String command = (String) msg.get("command");
        Map<String, Object> params = msg.containsKey("params") ? (Map<String, Object>) msg.get("params") : Map.of();

        log.debug("Cloud relay: dispatching command={} id={}", command, requestId);

        try {
            switch (command) {
                case "setFrequency" -> {
                    long hz = ((Number) params.get("hz")).longValue();
                    rigService.setFrequency(hz).thenAccept(success ->
                        sendCommandResponse(session, requestId, success,
                                Map.of("frequency", hz),
                                success ? "Frequency set" : "Failed to set frequency")
                    );
                }
                case "setMode" -> {
                    String mode = (String) params.get("mode");
                    int bandwidth = params.containsKey("bandwidth") ?
                            ((Number) params.get("bandwidth")).intValue() : 0;
                    rigService.setMode(mode, bandwidth).thenAccept(success ->
                        sendCommandResponse(session, requestId, success,
                                Map.of("mode", mode),
                                success ? "Mode set" : "Failed to set mode")
                    );
                }
                case "setPTT" -> {
                    boolean enable = (Boolean) params.get("enable");
                    String clientId = "relay-gateway";
                    rigService.setPTT(enable, clientId).thenAccept(result -> {
                        sendCommandResponse(session, requestId, result.success(),
                                Map.of("ptt", enable), result.message());
                        if (result.success()) {
                            eventsHandler.broadcastEvent(enable
                                    ? RigEventsHandler.RigEvent.pttActivated(clientId)
                                    : RigEventsHandler.RigEvent.pttReleased(clientId));
                        }
                    });
                }
                case "getStatus" -> {
                    RigStatus status = rigService.getRigStatus();
                    sendCommandResponse(session, requestId, true, Map.of("status", status), "Status retrieved");
                }
                default -> sendCommandResponse(session, requestId, false, null, "Unknown command: " + command);
            }
        } catch (Exception e) {
            log.error("Cloud relay: error dispatching command {}", command, e);
            sendCommandResponse(session, requestId, false, null, "Dispatch error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @PreDestroy
    public void shutdown() {
        running.set(false);
        registered.set(false);

        WebSocketSession session = gatewaySession;
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.GOING_AWAY);
            } catch (Exception e) {
                log.warn("Cloud relay: error closing gateway session", e);
            }
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Returns true if the relay is active and registered with the gateway.
     */
    public boolean isConnected() {
        WebSocketSession session = gatewaySession;
        return session != null && session.isOpen() && registered.get();
    }

    // -------------------------------------------------------------------------
    // Inner WebSocket handler
    // -------------------------------------------------------------------------

    private class GatewayHandler extends TextWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("Cloud relay: WebSocket connection established");
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            try {
                Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);
                String type = (String) msg.get("type");

                if ("registered".equals(type)) {
                    registered.set(true);
                    log.info("Cloud relay: registered with gateway as station {}", stationId);
                } else if ("error".equals(type)) {
                    log.error("Cloud relay: gateway error — {}", msg.get("message"));
                    registered.set(false);
                } else if (msg.containsKey("command")) {
                    // Incoming rig command from gateway
                    dispatchCommand(session, msg);
                } else {
                    log.debug("Cloud relay: unhandled message type={}", type);
                }
            } catch (Exception e) {
                log.error("Cloud relay: error handling gateway message", e);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.warn("Cloud relay: connection closed — {}", status);
            gatewaySession = null;
            registered.set(false);
            scheduleReconnect();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("Cloud relay: transport error", exception);
            registered.set(false);
        }
    }
}
