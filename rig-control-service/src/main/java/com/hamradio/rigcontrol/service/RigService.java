package com.hamradio.rigcontrol.service;

import com.hamradio.rigcontrol.dispatcher.RigCommandDispatcher;
import com.hamradio.rigcontrol.dto.RigStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * High-level rig control service.
 * Provides a clean API for rig operations using the command dispatcher.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RigService {

    private final RigCommandDispatcher dispatcher;

    /**
     * Get current rig status (cached reads for performance)
     */
    public RigStatus getRigStatus() {
        RigStatus.RigStatusBuilder status = RigStatus.builder()
                .timestamp(LocalDateTime.now())
                .connected(false);

        try {
            // Execute reads in parallel for speed
            CompletableFuture<Long> freqFuture = getFrequency();
            CompletableFuture<String> modeFuture = getMode();
            CompletableFuture<Boolean> pttFuture = getPTT();
            CompletableFuture<Integer> sMeterFuture = getSMeter();

            // Wait for all with timeout
            CompletableFuture.allOf(freqFuture, modeFuture, pttFuture, sMeterFuture)
                    .get(100, TimeUnit.MILLISECONDS);

            // Populate status
            status.frequencyHz(freqFuture.get());
            status.mode(modeFuture.get());
            status.pttActive(pttFuture.get());
            status.sMeter(sMeterFuture.get());
            status.connected(true);

        } catch (TimeoutException e) {
            log.warn("Rig status timeout");
            status.error("Timeout getting rig status");
        } catch (Exception e) {
            log.error("Error getting rig status", e);
            status.error(e.getMessage());
        }

        return status.build();
    }

    /**
     * Get frequency (cached read)
     */
    public CompletableFuture<Long> getFrequency() {
        return dispatcher.executeReadCommand("f")
                .thenApply(response -> {
                    String[] lines = response.split("\n");
                    if (lines.length > 0 && !lines[0].startsWith("RPRT")) {
                        return Long.parseLong(lines[0].trim());
                    }
                    return null;
                });
    }

    /**
     * Set frequency (write command)
     */
    public CompletableFuture<Boolean> setFrequency(long frequencyHz) {
        return dispatcher.executeWriteCommand("F " + frequencyHz)
                .thenApply(response -> response.contains("RPRT 0"));
    }

    /**
     * Get mode (cached read)
     */
    public CompletableFuture<String> getMode() {
        return dispatcher.executeReadCommand("m")
                .thenApply(response -> {
                    String[] lines = response.split("\n");
                    if (lines.length > 0 && !lines[0].startsWith("RPRT")) {
                        return lines[0].trim();
                    }
                    return null;
                });
    }

    /**
     * Set mode (write command)
     */
    public CompletableFuture<Boolean> setMode(String mode, int bandwidth) {
        return dispatcher.executeWriteCommand("M " + mode + " " + bandwidth)
                .thenApply(response -> response.contains("RPRT 0"));
    }

    /**
     * Get PTT status (cached read)
     */
    public CompletableFuture<Boolean> getPTT() {
        return dispatcher.executeReadCommand("t")
                .thenApply(response -> {
                    String[] lines = response.split("\n");
                    if (lines.length > 0 && !lines[0].startsWith("RPRT")) {
                        return lines[0].trim().equals("1");
                    }
                    return false;
                });
    }

    /**
     * Set PTT (managed by PTTLockManager)
     */
    public CompletableFuture<RigCommandDispatcher.PTTResult> setPTT(boolean enable, String clientId) {
        return dispatcher.executePTTCommand(enable, clientId);
    }

    /**
     * Get S-meter reading (short cache)
     */
    public CompletableFuture<Integer> getSMeter() {
        return dispatcher.executeReadCommand("\\get_level STRENGTH")
                .thenApply(response -> {
                    String[] lines = response.split("\n");
                    if (lines.length > 0 && !lines[0].startsWith("RPRT")) {
                        try {
                            return Integer.parseInt(lines[0].trim());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                    return null;
                });
    }

    /**
     * Test connection to rigctld
     */
    public boolean testConnection() {
        try {
            Long freq = getFrequency().get(1, TimeUnit.SECONDS);
            return freq != null;
        } catch (Exception e) {
            return false;
        }
    }
}
