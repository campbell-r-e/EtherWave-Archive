package com.hamradio.logbook.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe registry of remote rig service sessions connected via the station gateway.
 *
 * Each remote rig service holds one WebSocket session identified by its stationId.
 * This registry provides the command-send/response-correlation mechanism used by
 * RigControlController when routing commands to remote stations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StationGatewayRegistry {

    private static final long COMMAND_TIMEOUT_SECONDS = 5;

    private final ObjectMapper objectMapper;

    /** stationId -> WebSocketSession of the connected rig service */
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** requestId -> pending CompletableFuture waiting for a command response */
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    // -------------------------------------------------------------------------
    // Session management (called by StationGatewayHandler)
    // -------------------------------------------------------------------------

    public void register(Long stationId, WebSocketSession session) {
        sessions.put(stationId, session);
        log.info("Station {} registered in gateway registry (session={})", stationId, session.getId());
    }

    public void unregister(Long stationId) {
        sessions.remove(stationId);
        log.info("Station {} removed from gateway registry", stationId);
    }

    public boolean isConnected(Long stationId) {
        WebSocketSession session = sessions.get(stationId);
        return session != null && session.isOpen();
    }

    // -------------------------------------------------------------------------
    // Command dispatch (called by RigControlController for remote stations)
    // -------------------------------------------------------------------------

    /**
     * Send a command to a remote rig service and return a future that completes
     * when the response arrives (or times out after {@value COMMAND_TIMEOUT_SECONDS}s).
     */
    public CompletableFuture<Map<String, Object>> sendCommand(Long stationId, String command,
                                                               Map<String, Object> params) {
        WebSocketSession session = sessions.get(stationId);
        if (session == null || !session.isOpen()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Station " + stationId + " not connected via gateway"));
        }

        String requestId = "gw-" + requestCounter.incrementAndGet();
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        future.orTimeout(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
              .exceptionally(e -> {
                  pendingRequests.remove(requestId);
                  return null;
              });

        try {
            Map<String, Object> envelope = Map.of(
                    "id", requestId,
                    "command", command,
                    "params", params != null ? params : Map.of()
            );
            String json = objectMapper.writeValueAsString(envelope);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
            log.debug("Sent gateway command {} (id={}) to station {}", command, requestId, stationId);
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
            log.error("Failed to send gateway command to station {}", stationId, e);
        }

        return future;
    }

    /**
     * Complete a pending command request with the response received from the rig service.
     * Called by StationGatewayHandler when a response message arrives.
     */
    @SuppressWarnings("unchecked")
    public void completeRequest(String requestId, Map<String, Object> response) {
        CompletableFuture<Map<String, Object>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
        } else {
            log.debug("No pending request found for id={} (may have timed out)", requestId);
        }
    }
}
