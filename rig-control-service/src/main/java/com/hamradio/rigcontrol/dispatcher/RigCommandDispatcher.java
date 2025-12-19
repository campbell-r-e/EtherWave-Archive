package com.hamradio.rigcontrol.dispatcher;

import com.hamradio.rigcontrol.connection.RigctlConnection;
import com.hamradio.rigcontrol.ptt.PTTLockManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Central command dispatcher for rigctld communication.
 * Provides:
 * - Command serialization (one command at a time to rigctld)
 * - Request coalescing (deduplicate simultaneous identical reads)
 * - Smart caching (50ms for most reads, 20ms for S-meter)
 * - Thread-safe multi-client access
 * - Near-real-time performance (<50ms target)
 */
@Component
@Slf4j
public class RigCommandDispatcher {

    private static final long CACHE_TTL_MS = 50; // 50ms cache for most reads
    private static final long SMETER_CACHE_TTL_MS = 20; // 20ms cache for S-meter (changes rapidly)

    private final RigctlConnection connection;
    private final PTTLockManager pttLockManager;

    // Command queue for serialization
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

    // Cache for read operations
    private final Map<String, CachedValue> readCache = new ConcurrentHashMap<>();

    // In-flight request tracking for coalescing
    private final Map<String, CompletableFuture<String>> inflightRequests = new ConcurrentHashMap<>();

    public RigCommandDispatcher(RigctlConnection connection, PTTLockManager pttLockManager) {
        this.connection = connection;
        this.pttLockManager = pttLockManager;
    }

    @PostConstruct
    public void init() {
        log.info("RigCommandDispatcher initialized with <50ms latency target");
    }

    /**
     * Execute a read command with caching and coalescing
     */
    public CompletableFuture<String> executeReadCommand(String command) {
        // Check cache first
        CachedValue cached = readCache.get(command);
        long cacheTTL = command.contains("STRENGTH") ? SMETER_CACHE_TTL_MS : CACHE_TTL_MS;

        if (cached != null && !cached.isExpired(cacheTTL)) {
            log.debug("Cache hit for command: {}", command);
            return CompletableFuture.completedFuture(cached.value);
        }

        // Check if request is already in-flight (request coalescing)
        CompletableFuture<String> inflight = inflightRequests.get(command);
        if (inflight != null && !inflight.isDone()) {
            log.debug("Coalescing request for command: {}", command);
            return inflight;
        }

        // Execute new request
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                String response = connection.sendCommand(command);
                // Update cache
                readCache.put(command, new CachedValue(response));
                return response;
            } catch (IOException e) {
                log.error("Read command failed: {}", command, e);
                throw new CompletionException(e);
            } finally {
                // Remove from inflight tracking
                inflightRequests.remove(command);
            }
        }, commandExecutor);

        inflightRequests.put(command, future);
        return future;
    }

    /**
     * Execute a write command (state-changing).
     * Write commands bypass cache and go straight to the queue.
     */
    public CompletableFuture<String> executeWriteCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Executing write command: {}", command);
                String response = connection.sendCommand(command);

                // Invalidate related cache entries after write
                invalidateCacheFor(command);

                return response;
            } catch (IOException e) {
                log.error("Write command failed: {}", command, e);
                throw new CompletionException(e);
            }
        }, commandExecutor);
    }

    /**
     * Execute PTT command with lock management
     */
    public CompletableFuture<PTTResult> executePTTCommand(boolean enable, String clientId) {
        return CompletableFuture.supplyAsync(() -> {
            if (enable) {
                // Acquire PTT lock
                if (!pttLockManager.acquirePTT(clientId)) {
                    return new PTTResult(false, "PTT denied: held by " + pttLockManager.getPttOwner());
                }

                // Send PTT command to rig
                try {
                    String response = connection.sendCommand("T 1");
                    if (response.contains("RPRT 0")) {
                        return new PTTResult(true, "PTT activated");
                    } else {
                        // Command failed, release lock
                        pttLockManager.releasePTT(clientId);
                        return new PTTResult(false, "PTT command failed: " + response);
                    }
                } catch (IOException e) {
                    pttLockManager.releasePTT(clientId);
                    log.error("PTT activation failed", e);
                    return new PTTResult(false, "PTT error: " + e.getMessage());
                }

            } else {
                // Release PTT
                if (!pttLockManager.releasePTT(clientId)) {
                    return new PTTResult(false, "PTT release denied: not owned by client");
                }

                try {
                    String response = connection.sendCommand("T 0");
                    if (response.contains("RPRT 0")) {
                        return new PTTResult(true, "PTT released");
                    } else {
                        return new PTTResult(false, "PTT release command failed: " + response);
                    }
                } catch (IOException e) {
                    log.error("PTT release failed", e);
                    return new PTTResult(false, "PTT error: " + e.getMessage());
                }
            }
        }, commandExecutor);
    }

    /**
     * Invalidate cache entries related to a write command
     */
    private void invalidateCacheFor(String command) {
        if (command.startsWith("F ")) {
            readCache.remove("f"); // Invalidate frequency cache
        } else if (command.startsWith("M ")) {
            readCache.remove("m"); // Invalidate mode cache
        } else if (command.startsWith("T ")) {
            readCache.remove("t"); // Invalidate PTT cache
        }
    }

    /**
     * Clear all cached values (useful for testing or reconnection)
     */
    public void clearCache() {
        readCache.clear();
        log.debug("Read cache cleared");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RigCommandDispatcher");
        commandExecutor.shutdown();
        try {
            if (!commandExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                commandExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            commandExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Cached value with timestamp
     */
    private static class CachedValue {
        final String value;
        final long timestamp;

        CachedValue(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    /**
     * PTT command result
     */
    public record PTTResult(boolean success, String message) {}
}
