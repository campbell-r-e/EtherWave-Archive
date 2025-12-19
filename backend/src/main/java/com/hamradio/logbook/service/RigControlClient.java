package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket client for connecting to the rig control service.
 * Manages per-user connections and forwards commands/status updates.
 */
@Service
@Slf4j
public class RigControlClient {

    @Value("${rig.control.service.url:ws://localhost:8081}")
    private String rigControlServiceUrl;

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final StandardWebSocketClient webSocketClient;

    // Map of stationId -> WebSocket sessions
    private final Map<Long, StationConnection> activeConnections = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public RigControlClient(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.webSocketClient = new StandardWebSocketClient();
    }

    /**
     * Connect a station to the rig control service
     */
    public CompletableFuture<Boolean> connect(Long stationId, String stationName, String host, Integer port) {
        // Close existing connection if any
        disconnect(stationId);

        CompletableFuture<Boolean> connectionFuture = new CompletableFuture<>();

        try {
            String baseUrl = host != null && port != null
                ? String.format("ws://%s:%d", host, port)
                : rigControlServiceUrl;

            StationConnection connection = new StationConnection(stationId, stationName);

            // Connect to command WebSocket
            URI commandUri = new URI(baseUrl + "/ws/rig/command?clientName=" + stationName);
            webSocketClient.execute(
                connection.commandHandler,
                commandUri.toString()
            ).whenComplete((session, error) -> {
                if (error != null) {
                    log.error("Failed to connect command WebSocket for station {}: {}",
                            stationId, error.getMessage());
                    connectionFuture.complete(false);
                    return;
                }
                connection.commandSession = session;
                log.info("Connected command WebSocket for station {}", stationId);
            });

            // Connect to status WebSocket
            URI statusUri = new URI(baseUrl + "/ws/rig/status");
            webSocketClient.execute(
                connection.statusHandler,
                statusUri.toString()
            ).whenComplete((session, error) -> {
                if (error != null) {
                    log.error("Failed to connect status WebSocket for station {}: {}",
                            stationId, error.getMessage());
                }
                connection.statusSession = session;
                log.info("Connected status WebSocket for station {}", stationId);
            });

            // Connect to events WebSocket
            URI eventsUri = new URI(baseUrl + "/ws/rig/events");
            webSocketClient.execute(
                connection.eventsHandler,
                eventsUri.toString()
            ).whenComplete((session, error) -> {
                if (error != null) {
                    log.error("Failed to connect events WebSocket for station {}: {}",
                            stationId, error.getMessage());
                }
                connection.eventsSession = session;
                log.info("Connected events WebSocket for station {}", stationId);
            });

            activeConnections.put(stationId, connection);
            connectionFuture.complete(true);

        } catch (Exception e) {
            log.error("Error connecting to rig control service for station {}: {}",
                    stationId, e.getMessage(), e);
            connectionFuture.complete(false);
        }

        return connectionFuture;
    }

    /**
     * Disconnect a station from the rig control service
     */
    public void disconnect(Long stationId) {
        StationConnection connection = activeConnections.remove(stationId);
        if (connection != null) {
            connection.close();
            log.info("Disconnected station {} from rig control service", stationId);
        }
    }

    /**
     * Send a command to the rig control service
     */
    public CompletableFuture<Map<String, Object>> sendCommand(Long stationId, String command, Map<String, Object> params) {
        StationConnection connection = activeConnections.get(stationId);
        if (connection == null || connection.commandSession == null || !connection.commandSession.isOpen()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Station not connected to rig control service")
            );
        }

        CompletableFuture<Map<String, Object>> responseFuture = new CompletableFuture<>();
        String requestId = "req-" + requestCounter.incrementAndGet();

        try {
            Map<String, Object> request = Map.of(
                "id", requestId,
                "command", command,
                "params", params
            );

            // Store the future for this request
            connection.pendingRequests.put(requestId, responseFuture);

            // Send the command
            String json = objectMapper.writeValueAsString(request);
            connection.commandSession.sendMessage(new TextMessage(json));

            log.debug("Sent command {} to station {}: {}", command, stationId, json);

            // Timeout after 5 seconds
            responseFuture.orTimeout(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Error sending command to station {}: {}", stationId, e.getMessage(), e);
            responseFuture.completeExceptionally(e);
        }

        return responseFuture;
    }

    /**
     * Check if a station is connected
     */
    public boolean isConnected(Long stationId) {
        StationConnection connection = activeConnections.get(stationId);
        return connection != null &&
               connection.commandSession != null &&
               connection.commandSession.isOpen();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RigControlClient, closing {} connections", activeConnections.size());
        activeConnections.values().forEach(StationConnection::close);
        activeConnections.clear();
    }

    /**
     * Represents a connection to the rig control service for a specific station
     */
    private class StationConnection {
        final Long stationId;
        final String stationName;

        WebSocketSession commandSession;
        WebSocketSession statusSession;
        WebSocketSession eventsSession;

        final TextWebSocketHandler commandHandler;
        final TextWebSocketHandler statusHandler;
        final TextWebSocketHandler eventsHandler;

        final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();

        StationConnection(Long stationId, String stationName) {
            this.stationId = stationId;
            this.stationName = stationName;

            // Command handler - receives responses to commands
            this.commandHandler = new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> response = objectMapper.readValue(message.getPayload(), Map.class);
                        String requestId = (String) response.get("id");

                        // Complete the pending request
                        CompletableFuture<Map<String, Object>> future = pendingRequests.remove(requestId);
                        if (future != null) {
                            future.complete(response);
                        }

                        log.debug("Received command response for station {}: {}", stationId, message.getPayload());

                    } catch (Exception e) {
                        log.error("Error handling command response for station {}: {}", stationId, e.getMessage(), e);
                    }
                }
            };

            // Status handler - receives real-time status updates
            this.statusHandler = new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    try {
                        // Forward status to frontend via WebSocket
                        messagingTemplate.convertAndSend("/topic/rig/status/" + stationId, message.getPayload());

                        log.debug("Forwarded status update for station {}", stationId);

                    } catch (Exception e) {
                        log.error("Error handling status update for station {}: {}", stationId, e.getMessage(), e);
                    }
                }
            };

            // Events handler - receives events (PTT, connections, etc)
            this.eventsHandler = new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    try {
                        // Forward events to frontend via WebSocket
                        messagingTemplate.convertAndSend("/topic/rig/events/" + stationId, message.getPayload());

                        log.debug("Forwarded event for station {}", stationId);

                    } catch (Exception e) {
                        log.error("Error handling event for station {}: {}", stationId, e.getMessage(), e);
                    }
                }
            };
        }

        void close() {
            try {
                if (commandSession != null && commandSession.isOpen()) {
                    commandSession.close();
                }
                if (statusSession != null && statusSession.isOpen()) {
                    statusSession.close();
                }
                if (eventsSession != null && eventsSession.isOpen()) {
                    eventsSession.close();
                }
            } catch (Exception e) {
                log.warn("Error closing connections for station {}: {}", stationId, e.getMessage());
            }
        }
    }
}
