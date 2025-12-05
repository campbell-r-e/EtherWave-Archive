package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.StationStatistics;
import com.hamradio.logbook.dto.StationStatsSummary;
import com.hamradio.logbook.service.StationStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for multi-station contest statistics
 * Provides per-station metrics, leaderboards, and comprehensive summaries
 */
@RestController
@RequestMapping("/api/logs/{logId}/stats")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class StationStatisticsController {

    private final StationStatisticsService stationStatisticsService;

    /**
     * Get statistics for all stations in a log
     * @param logId The log ID
     * @return List of station statistics including leaderboard rankings
     */
    @GetMapping("/stations")
    public ResponseEntity<List<StationStatistics>> getAllStationStats(
            @PathVariable Long logId,
            Authentication authentication) {

        log.info("Getting all station statistics for log: {}", logId);

        List<StationStatistics> stats = stationStatisticsService.getAllStationStats(logId);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get statistics for a specific station
     * @param logId The log ID
     * @param stationNumber The station number (1-1000)
     * @return Station statistics
     */
    @GetMapping("/stations/{stationNumber}")
    public ResponseEntity<StationStatistics> getStationStats(
            @PathVariable Long logId,
            @PathVariable Integer stationNumber,
            Authentication authentication) {

        log.info("Getting statistics for station {} in log: {}", stationNumber, logId);

        StationStatistics stats = stationStatisticsService.getStationStats(logId, stationNumber);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get statistics for GOTA station
     * @param logId The log ID
     * @return GOTA station statistics
     */
    @GetMapping("/gota")
    public ResponseEntity<StationStatistics> getGotaStats(
            @PathVariable Long logId,
            Authentication authentication) {

        log.info("Getting GOTA statistics for log: {}", logId);

        StationStatistics stats = stationStatisticsService.getGotaStats(logId);

        return ResponseEntity.ok(stats);
    }

    /**
     * Get comprehensive summary with main/GOTA separation
     * Used for score summary display and leaderboard
     * @param logId The log ID
     * @return Complete station statistics summary
     */
    @GetMapping("/summary")
    public ResponseEntity<StationStatsSummary> getStationSummary(
            @PathVariable Long logId,
            Authentication authentication) {

        log.info("Getting station summary for log: {}", logId);

        StationStatsSummary summary = stationStatisticsService.getStationSummary(logId);

        return ResponseEntity.ok(summary);
    }
}
