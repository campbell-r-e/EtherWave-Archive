package com.hamradio.rigcontrol.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for /ws/rig/events endpoint.
 * Broadcasts event notifications to all connected clients.
 * One-way communication: server -> clients only.
 *
 * Events include:
 * - PTT activation/release
 * - PTT conflicts (denied requests)
 * - Client connections/disconnections
 * - Errors and warnings
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RigEventsHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> eventSubscribers = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        eventSubscribers.add(session);
        log.info("Events WebSocket connected: {} (total: {})", session.getId(), eventSubscribers.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        eventSubscribers.remove(session);
        log.info("Events WebSocket disconnected: {} ({}) (total: {})",
                session.getId(), status, eventSubscribers.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Events WebSocket error for session {}", session.getId(), exception);
        eventSubscribers.remove(session);
    }

    /**
     * Broadcast an event to all connected clients
     */
    public void broadcastEvent(RigEvent event) {
        if (eventSubscribers.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            TextMessage message = new TextMessage(json);

            eventSubscribers.removeIf(session -> {
                if (!session.isOpen()) {
                    return true;
                }

                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                    return false;
                } catch (Exception e) {
                    log.warn("Failed to send event to session {}: {}", session.getId(), e.getMessage());
                    return true;
                }
            });

        } catch (Exception e) {
            log.error("Error broadcasting event", e);
        }
    }

    /**
     * Event payload
     */
    public record RigEvent(LocalDateTime timestamp, String eventType, String clientId, String message) {
        public static RigEvent pttActivated(String clientId) {
            return new RigEvent(LocalDateTime.now(), "ptt_activated", clientId,
                    "Client '" + clientId + "' activated PTT");
        }

        public static RigEvent pttReleased(String clientId) {
            return new RigEvent(LocalDateTime.now(), "ptt_released", clientId,
                    "Client '" + clientId + "' released PTT");
        }

        public static RigEvent pttDenied(String clientId, String owner) {
            return new RigEvent(LocalDateTime.now(), "ptt_denied", clientId,
                    "PTT denied for '" + clientId + "': held by '" + owner + "'");
        }

        public static RigEvent clientConnected(String clientId) {
            return new RigEvent(LocalDateTime.now(), "client_connected", clientId,
                    "Client '" + clientId + "' connected");
        }

        public static RigEvent clientDisconnected(String clientId, boolean hadPTT) {
            var msg = "Client '" + clientId + "' disconnected";
            if (hadPTT) {
                msg += " (PTT auto-released)";
            }
            return new RigEvent(LocalDateTime.now(), "client_disconnected", clientId, msg);
        }

        public static RigEvent error(String clientId, String errorMessage) {
            return new RigEvent(LocalDateTime.now(), "error", clientId, errorMessage);
        }
    }
}
