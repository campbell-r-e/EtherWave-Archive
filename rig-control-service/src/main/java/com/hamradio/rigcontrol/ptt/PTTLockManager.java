package com.hamradio.rigcontrol.ptt;

import com.hamradio.rigcontrol.websocket.RigEventsHandler;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages exclusive PTT (Push-To-Talk) access for multiple clients.
 * Only one client can hold PTT at a time (First-Come-First-Served).
 * Automatically releases PTT when a client disconnects or when the safety
 * timeout expires (configurable via ptt.safety.timeout.seconds).
 */
@Component
@Slf4j
public class PTTLockManager {

    @Value("${ptt.safety.timeout.seconds:120}")
    private int safetyTimeoutSeconds;

    private final ReentrantLock lock = new ReentrantLock();
    private final RigEventsHandler eventsHandler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ptt-timeout");
        t.setDaemon(true);
        return t;
    });

    @Getter
    private String pttOwner = null;

    @Getter
    private long pttAcquiredAt = 0;

    private volatile ScheduledFuture<?> timeoutTask;

    public PTTLockManager(RigEventsHandler eventsHandler) {
        this.eventsHandler = eventsHandler;
    }

    /**
     * Attempt to acquire PTT for a client.
     * Starts the safety timeout countdown if configured.
     *
     * @param clientId The client requesting PTT
     * @return true if PTT acquired, false if another client holds it
     */
    public boolean acquirePTT(String clientId) {
        lock.lock();
        try {
            if (pttOwner == null) {
                pttOwner = clientId;
                pttAcquiredAt = System.currentTimeMillis();
                log.info("PTT acquired by client: {}", clientId);
                scheduleTimeout(clientId);
                return true;
            } else if (pttOwner.equals(clientId)) {
                log.debug("Client {} already owns PTT", clientId);
                resetTimeout(clientId);
                return true;
            } else {
                log.warn("PTT denied for client {}: currently held by {}", clientId, pttOwner);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Release PTT for a client.
     * Only the PTT owner can release it.
     *
     * @param clientId The client releasing PTT
     * @return true if PTT released, false if client doesn't own it
     */
    public boolean releasePTT(String clientId) {
        lock.lock();
        try {
            if (pttOwner == null) {
                log.debug("PTT release ignored: no active PTT");
                return true;
            } else if (pttOwner.equals(clientId)) {
                long duration = System.currentTimeMillis() - pttAcquiredAt;
                log.info("PTT released by client {}: held for {}ms", clientId, duration);
                cancelTimeout();
                pttOwner = null;
                pttAcquiredAt = 0;
                return true;
            } else {
                log.warn("PTT release denied for client {}: owned by {}", clientId, pttOwner);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Force release PTT (called when client disconnects).
     * This is a safety mechanism to prevent stuck transmit.
     *
     * @param clientId The client that disconnected
     */
    public void forceReleasePTT(String clientId) {
        lock.lock();
        try {
            if (pttOwner != null && pttOwner.equals(clientId)) {
                long duration = System.currentTimeMillis() - pttAcquiredAt;
                log.warn("PTT force-released for disconnected client {}: held for {}ms", clientId, duration);
                cancelTimeout();
                pttOwner = null;
                pttAcquiredAt = 0;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if PTT is currently active
     */
    public boolean isPTTActive() {
        lock.lock();
        try {
            return pttOwner != null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if a specific client owns PTT
     */
    public boolean clientOwnsPTT(String clientId) {
        lock.lock();
        try {
            return pttOwner != null && pttOwner.equals(clientId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get PTT hold duration in milliseconds
     */
    public long getPTTDuration() {
        lock.lock();
        try {
            if (pttOwner == null) {
                return 0;
            }
            return System.currentTimeMillis() - pttAcquiredAt;
        } finally {
            lock.unlock();
        }
    }

    // -------------------------------------------------------------------------
    // Safety timeout
    // -------------------------------------------------------------------------

    private void scheduleTimeout(String clientId) {
        cancelTimeout();
        if (safetyTimeoutSeconds <= 0) {
            return;
        }
        timeoutTask = scheduler.schedule(() -> onTimeout(clientId), safetyTimeoutSeconds, TimeUnit.SECONDS);
        log.debug("PTT safety timeout scheduled: {}s for client {}", safetyTimeoutSeconds, clientId);
    }

    private void resetTimeout(String clientId) {
        if (safetyTimeoutSeconds <= 0) {
            return;
        }
        cancelTimeout();
        timeoutTask = scheduler.schedule(() -> onTimeout(clientId), safetyTimeoutSeconds, TimeUnit.SECONDS);
        log.debug("PTT safety timeout reset: {}s for client {}", safetyTimeoutSeconds, clientId);
    }

    private void cancelTimeout() {
        ScheduledFuture<?> task = timeoutTask;
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }
        timeoutTask = null;
    }

    private void onTimeout(String clientId) {
        lock.lock();
        try {
            if (pttOwner == null || !pttOwner.equals(clientId)) {
                return; // Already released by another path
            }
            long duration = System.currentTimeMillis() - pttAcquiredAt;
            log.warn("PTT safety timeout expired for client {} after {}ms — force-releasing PTT", clientId, duration);
            pttOwner = null;
            pttAcquiredAt = 0;
        } finally {
            lock.unlock();
        }
        eventsHandler.broadcastEvent(RigEventsHandler.RigEvent.pttTimeout(clientId));
    }

    @PreDestroy
    public void shutdown() {
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
