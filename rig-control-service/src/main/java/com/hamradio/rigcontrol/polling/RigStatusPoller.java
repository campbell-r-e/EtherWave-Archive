package com.hamradio.rigcontrol.polling;

import com.hamradio.rigcontrol.dto.RigStatus;
import com.hamradio.rigcontrol.service.RigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls rig status every 100ms and broadcasts to all connected clients.
 * Implements continuous status broadcasting (Option A from requirements).
 */
@Component
@Slf4j
public class RigStatusPoller {

    private static final long POLL_INTERVAL_MS = 100; // 10 times per second

    private final RigService rigService;
    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> statusSubscribers = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running = false;

    public RigStatusPoller(RigService rigService, ObjectMapper objectMapper) {
        this.rigService = rigService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        log.info("Starting RigStatusPoller with {}ms interval", POLL_INTERVAL_MS);
        running = true;

        scheduler.scheduleAtFixedRate(
                this::pollAndBroadcast,
                0,
                POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Poll rig status and broadcast to all subscribers
     */
    private void pollAndBroadcast() {
        if (!running || statusSubscribers.isEmpty()) {
            return; // Skip polling if no subscribers
        }

        try {
            RigStatus status = rigService.getRigStatus();
            broadcastStatus(status);
        } catch (Exception e) {
            log.error("Error polling rig status", e);
        }
    }

    /**
     * Broadcast status to all connected WebSocket clients
     */
    private void broadcastStatus(RigStatus status) {
        if (statusSubscribers.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(status);
            TextMessage message = new TextMessage(json);

            // Send to all subscribers, remove closed sessions
            statusSubscribers.removeIf(session -> {
                if (!session.isOpen()) {
                    return true; // Remove closed session
                }

                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                    return false; // Keep session
                } catch (Exception e) {
                    log.warn("Failed to send status to session {}: {}", session.getId(), e.getMessage());
                    return true; // Remove failed session
                }
            });

        } catch (Exception e) {
            log.error("Error broadcasting status", e);
        }
    }

    /**
     * Subscribe a WebSocket session to status updates
     */
    public void subscribe(WebSocketSession session) {
        statusSubscribers.add(session);
        log.info("Client {} subscribed to status updates (total: {})", session.getId(), statusSubscribers.size());
    }

    /**
     * Unsubscribe a WebSocket session from status updates
     */
    public void unsubscribe(WebSocketSession session) {
        statusSubscribers.remove(session);
        log.info("Client {} unsubscribed from status updates (total: {})", session.getId(), statusSubscribers.size());
    }

    /**
     * Get current subscriber count
     */
    public int getSubscriberCount() {
        return statusSubscribers.size();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping RigStatusPoller");
        running = false;

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        statusSubscribers.clear();
    }
}
