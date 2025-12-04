package com.hamradio.logbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous scoring job service for bulk operations
 * Handles background scoring to avoid blocking UI during imports or recalculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringJobService {

    private final ScoringService scoringService;

    /**
     * Recalculate scores for a log asynchronously
     * Used after bulk imports or when user requests recalculation
     *
     * @param logId The log ID to recalculate
     * @return CompletableFuture that completes when scoring is done
     */
    @Async
    public CompletableFuture<Void> recalculateLogScoresAsync(Long logId) {
        log.info("Starting async score recalculation for log {}", logId);

        try {
            scoringService.recalculateLogScores(logId);
            log.info("Async score recalculation completed successfully for log {}", logId);
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Error during async score recalculation for log {}: {}", logId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Recalculate scores synchronously
     * Used when immediate result is needed (e.g., user clicks "Recalculate Scores")
     *
     * @param logId The log ID to recalculate
     */
    public void recalculateLogScoresSync(Long logId) {
        log.info("Starting synchronous score recalculation for log {}", logId);
        scoringService.recalculateLogScores(logId);
        log.info("Synchronous score recalculation completed for log {}", logId);
    }
}
