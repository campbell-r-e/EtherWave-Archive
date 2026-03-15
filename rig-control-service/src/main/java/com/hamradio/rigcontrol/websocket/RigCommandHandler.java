package com.hamradio.rigcontrol.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.rigcontrol.audit.CommandAuditLog;
import com.hamradio.rigcontrol.dispatcher.RigCommandDispatcher;
import com.hamradio.rigcontrol.ptt.PTTLockManager;
import com.hamradio.rigcontrol.service.RigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket handler for /ws/rig/command endpoint.
 * Handles bidirectional command/response communication.
 *
 * Message format:
 * Client -> Server:
 * {
 *   "id": "req-12345",
 *   "command": "setFrequency",
 *   "params": {"hz": 14250000}
 * }
 *
 * Server -> Client:
 * {
 *   "id": "req-12345",
 *   "success": true,
 *   "result": {...},
 *   "message": "..."
 * }
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RigCommandHandler extends TextWebSocketHandler {

    private final RigService rigService;
    private final PTTLockManager pttLockManager;
    private final RigEventsHandler eventsHandler;
    private final CommandAuditLog auditLog;
    private final ObjectMapper objectMapper;

    private final Map<String, String> sessionToClientId = new ConcurrentHashMap<>();
    private final AtomicInteger clientCounter = new AtomicInteger(0);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Extract client name from query parameter or assign auto-ID
        String clientId = extractClientId(session);
        sessionToClientId.put(session.getId(), clientId);

        log.info("Command WebSocket connected: session={}, client={}", session.getId(), clientId);
        eventsHandler.broadcastEvent(RigEventsHandler.RigEvent.clientConnected(clientId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String clientId = sessionToClientId.get(session.getId());
        if (clientId == null) {
            log.warn("Unknown session: {}", session.getId());
            return;
        }

        try {
            // Parse command request
            @SuppressWarnings("unchecked")
            Map<String, Object> request = objectMapper.readValue(message.getPayload(), Map.class);

            String requestId = (String) request.get("id");
            String command = (String) request.get("command");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) request.get("params");

            log.debug("Command from {}: {}", clientId, command);

            // Execute command and send response
            executeCommand(session, clientId, requestId, command, params);

        } catch (Exception e) {
            log.error("Error handling command from {}", clientId, e);
            sendErrorResponse(session, null, "Invalid command format: " + e.getMessage());
        }
    }

    /**
     * Execute command based on type
     */
    private void executeCommand(WebSocketSession session, String clientId, String requestId,
                                 String command, Map<String, Object> params) {
        try {
            switch (command) {
                case "setFrequency" -> {
                    long hz = ((Number) params.get("hz")).longValue();
                    rigService.setFrequency(hz).thenAccept(success -> {
                        sendResponse(session, requestId, success, Map.of("frequency", hz),
                                success ? "Frequency set" : "Failed to set frequency");
                        auditLog.record(clientId, "setFrequency", "hz=" + hz, success);
                    });
                }
                case "setMode" -> {
                    String mode = (String) params.get("mode");
                    int bandwidth = params.containsKey("bandwidth") ?
                            ((Number) params.get("bandwidth")).intValue() : 0;
                    rigService.setMode(mode, bandwidth).thenAccept(success -> {
                        sendResponse(session, requestId, success, Map.of("mode", mode),
                                success ? "Mode set" : "Failed to set mode");
                        auditLog.record(clientId, "setMode", "mode=" + mode + " bw=" + bandwidth, success);
                    });
                }
                case "setPTT" -> {
                    boolean enable = (Boolean) params.get("enable");
                    rigService.setPTT(enable, clientId).thenAccept(result -> {
                        sendResponse(session, requestId, result.success(), Map.of("ptt", enable), result.message());
                        auditLog.record(clientId, "setPTT", "enable=" + enable, result.success());

                        // Broadcast PTT events
                        if (result.success()) {
                            if (enable) {
                                eventsHandler.broadcastEvent(RigEventsHandler.RigEvent.pttActivated(clientId));
                            } else {
                                eventsHandler.broadcastEvent(RigEventsHandler.RigEvent.pttReleased(clientId));
                            }
                        } else if (enable) {
                            // PTT denied
                            eventsHandler.broadcastEvent(RigEventsHandler.RigEvent.pttDenied(
                                    clientId, pttLockManager.getPttOwner()));
                        }
                    });
                }
                case "getStatus" -> {
                    // Force immediate status read (not from cache)
                    var status = rigService.getRigStatus();
                    sendResponse(session, requestId, true, Map.of("status", status), "Status retrieved");
                    auditLog.record(clientId, "getStatus", null, true);
                }
                default -> sendErrorResponse(session, requestId, "Unknown command: " + command);
            }

        } catch (Exception e) {
            log.error("Error executing command: {}", command, e);
            sendErrorResponse(session, requestId, "Command execution error: " + e.getMessage());
        }
    }

    /**
     * Send success/error response to client
     */
    private void sendResponse(WebSocketSession session, String requestId, boolean success,
                               Map<String, Object> result, String message) {
        try {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", requestId != null ? requestId : "");
            response.put("success", success);
            response.put("result", result);
            response.put("message", message);

            String json = objectMapper.writeValueAsString(response);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("Error sending response", e);
        }
    }

    /**
     * Send error response to client
     */
    private void sendErrorResponse(WebSocketSession session, String requestId, String errorMessage) {
        Map<String, Object> emptyResult = new java.util.HashMap<>();
        sendResponse(session, requestId, false, emptyResult, errorMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String clientId = sessionToClientId.remove(session.getId());
        if (clientId == null) {
            return;
        }

        log.info("Command WebSocket disconnected: session={}, client={}, status={}",
                session.getId(), clientId, status);

        // Auto-release PTT if client held it (safety mechanism)
        boolean hadPTT = pttLockManager.clientOwnsPTT(clientId);
        if (hadPTT) {
            pttLockManager.forceReleasePTT(clientId);
            log.warn("Auto-released PTT for disconnected client: {}", clientId);
        }

        eventsHandler.broadcastEvent(RigEventsHandler.RigEvent.clientDisconnected(clientId, hadPTT));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String clientId = sessionToClientId.get(session.getId());
        log.error("Command WebSocket error for client {}", clientId, exception);

        // Auto-release PTT on error
        if (clientId != null && pttLockManager.clientOwnsPTT(clientId)) {
            pttLockManager.forceReleasePTT(clientId);
        }
    }

    /**
     * Extract client ID from query parameter or assign auto-ID
     * URL format: /ws/rig/command?clientName=Logbook
     */
    private String extractClientId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) {
                return "Client-" + clientCounter.incrementAndGet();
            }

            String clientName = UriComponentsBuilder.fromUri(uri)
                    .build()
                    .getQueryParams()
                    .getFirst("clientName");

            if (clientName != null && !clientName.isBlank()) {
                return clientName.trim();
            }

        } catch (Exception e) {
            log.warn("Error extracting client name", e);
        }

        return "Client-" + clientCounter.incrementAndGet();
    }
}
