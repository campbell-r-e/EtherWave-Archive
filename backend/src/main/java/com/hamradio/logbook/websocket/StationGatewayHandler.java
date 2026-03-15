package com.hamradio.logbook.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket handler for the station gateway endpoint (/ws/station-gateway).
 *
 * Remote rig services connect here to register as controllable stations.
 * The protocol is JSON-framed messages (not STOMP).
 *
 * Inbound message types (rig service -> gateway):
 *   {"type":"register",  "stationId":1, "apiKey":"..."}
 *   {"type":"heartbeat", "stationId":1}
 *   {"type":"response",  "id":"gw-1", "success":true, "result":{...}, "message":"..."}
 *   {"type":"status",    "stationId":1, "status":{...}}
 *
 * Outbound message types (gateway -> rig service):
 *   {"type":"registered"}
 *   {"type":"error", "message":"..."}
 *   {"id":"gw-1", "command":"setFrequency", "params":{"hz":14250000}}
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StationGatewayHandler extends TextWebSocketHandler {

    private final StationGatewayRegistry registry;
    private final StationRepository stationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /** session ID -> authenticated stationId */
    private final Map<String, Long> sessionToStation = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Connection lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Station gateway: new connection from {} (session={})",
                session.getRemoteAddress(), session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long stationId = sessionToStation.remove(session.getId());
        if (stationId != null) {
            registry.unregister(stationId);
            log.info("Station gateway: station {} disconnected (session={}, status={})",
                    stationId, session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Station gateway: transport error for session {}", session.getId(), exception);
        Long stationId = sessionToStation.remove(session.getId());
        if (stationId != null) {
            registry.unregister(stationId);
        }
    }

    // -------------------------------------------------------------------------
    // Message handling
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> msg;
        try {
            msg = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception e) {
            log.warn("Station gateway: malformed JSON from session {}: {}", session.getId(), e.getMessage());
            sendError(session, "Invalid JSON");
            return;
        }

        String type = (String) msg.get("type");

        if ("register".equals(type)) {
            handleRegistration(session, msg);
        } else if ("heartbeat".equals(type)) {
            handleHeartbeat(session);
        } else if ("response".equals(type)) {
            handleCommandResponse(session, msg);
        } else if ("status".equals(type)) {
            handleStatusUpdate(session, msg);
        } else {
            log.debug("Station gateway: unknown message type '{}' from session {}", type, session.getId());
        }
    }

    // -------------------------------------------------------------------------
    // Individual message handlers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void handleRegistration(WebSocketSession session, Map<String, Object> msg) {
        Object stationIdRaw = msg.get("stationId");
        String incomingKey = (String) msg.get("apiKey");

        if (stationIdRaw == null || incomingKey == null) {
            log.warn("Station gateway: registration missing stationId or apiKey from session {}", session.getId());
            sendError(session, "Registration requires stationId and apiKey");
            closeSession(session);
            return;
        }

        long stationId = ((Number) stationIdRaw).longValue();

        Optional<Station> stationOpt = stationRepository.findById(stationId);
        if (stationOpt.isEmpty()) {
            log.warn("Station gateway: unknown stationId {} from session {}", stationId, session.getId());
            sendError(session, "Unknown station");
            closeSession(session);
            return;
        }

        Station station = stationOpt.get();

        if (!Boolean.TRUE.equals(station.getRemoteStation())) {
            log.warn("Station gateway: station {} is not configured as remote", stationId);
            sendError(session, "Station is not configured as a remote station");
            closeSession(session);
            return;
        }

        if (station.getApiKeyHash() == null || !passwordEncoder.matches(incomingKey, station.getApiKeyHash())) {
            log.warn("Station gateway: invalid API key for station {} (session={})", stationId, session.getId());
            sendError(session, "Invalid API key");
            closeSession(session);
            return;
        }

        sessionToStation.put(session.getId(), stationId);
        registry.register(stationId, session);

        sendAck(session, "registered");
        log.info("Station gateway: station {} '{}' registered (session={})",
                stationId, station.getStationName(), session.getId());
    }

    private void handleHeartbeat(WebSocketSession session) {
        Long stationId = sessionToStation.get(session.getId());
        if (stationId != null) {
            log.debug("Station gateway: heartbeat from station {}", stationId);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCommandResponse(WebSocketSession session, Map<String, Object> msg) {
        String requestId = (String) msg.get("id");
        if (requestId == null) {
            log.warn("Station gateway: response missing id from session {}", session.getId());
            return;
        }
        registry.completeRequest(requestId, msg);
    }

    @SuppressWarnings("unchecked")
    private void handleStatusUpdate(WebSocketSession session, Map<String, Object> msg) {
        Long stationId = sessionToStation.get(session.getId());
        if (stationId == null) {
            return;
        }
        Object status = msg.get("status");
        if (status != null) {
            messagingTemplate.convertAndSend("/topic/rig/status/" + stationId, status);
            log.debug("Station gateway: forwarded status update for station {}", stationId);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendAck(WebSocketSession session, String type) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", type));
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("Station gateway: failed to send ack to session {}", session.getId(), e);
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", "error", "message", errorMessage));
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.warn("Station gateway: failed to send error to session {}", session.getId(), e);
        }
    }

    private void closeSession(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (Exception e) {
            log.warn("Station gateway: error closing session {}", session.getId(), e);
        }
    }
}
