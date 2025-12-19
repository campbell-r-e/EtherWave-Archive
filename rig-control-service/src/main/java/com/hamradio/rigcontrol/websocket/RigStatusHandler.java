package com.hamradio.rigcontrol.websocket;

import com.hamradio.rigcontrol.polling.RigStatusPoller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for /ws/rig/status endpoint.
 * Broadcasts rig status updates every 100ms to all connected clients.
 * One-way communication: server -> clients only.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RigStatusHandler extends TextWebSocketHandler {

    private final RigStatusPoller statusPoller;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Status WebSocket connected: {}", session.getId());
        statusPoller.subscribe(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Status WebSocket disconnected: {} ({})", session.getId(), status);
        statusPoller.unsubscribe(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Status WebSocket error for session {}", session.getId(), exception);
        statusPoller.unsubscribe(session);
    }
}
