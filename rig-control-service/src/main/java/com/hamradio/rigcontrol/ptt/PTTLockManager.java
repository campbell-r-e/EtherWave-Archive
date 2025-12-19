package com.hamradio.rigcontrol.ptt;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages exclusive PTT (Push-To-Talk) access for multiple clients.
 * Only one client can hold PTT at a time (First-Come-First-Served).
 * Automatically releases PTT when client disconnects.
 */
@Component
@Slf4j
public class PTTLockManager {

    private final ReentrantLock lock = new ReentrantLock();

    @Getter
    private String pttOwner = null; // Client ID currently holding PTT

    @Getter
    private long pttAcquiredAt = 0; // Timestamp when PTT was acquired

    /**
     * Attempt to acquire PTT for a client.
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
                return true;
            } else if (pttOwner.equals(clientId)) {
                // Client already owns PTT
                log.debug("Client {} already owns PTT", clientId);
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
                return true; // No PTT active, nothing to release
            } else if (pttOwner.equals(clientId)) {
                long duration = System.currentTimeMillis() - pttAcquiredAt;
                log.info("PTT released by client {}: held for {}ms", clientId, duration);
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
}
