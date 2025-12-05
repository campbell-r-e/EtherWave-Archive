package com.hamradio.logbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for multi-station contest statistics
 * Provides comprehensive performance metrics per station or GOTA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationStatistics {

    /**
     * Station number (1-1000), null if this is summary or GOTA
     */
    private Integer stationNumber;

    /**
     * Whether this is GOTA (Get On The Air) station
     */
    private Boolean isGota;

    /**
     * Station label for display (e.g., "Station 1", "GOTA", "All")
     */
    private String stationLabel;

    /**
     * Total QSO count for this station
     */
    private Integer qsoCount;

    /**
     * Total points scored by this station
     */
    private Integer points;

    /**
     * QSO rate in contacts per hour
     */
    private Double qsoRate;

    /**
     * Band breakdown: band name -> QSO count
     * Example: {"20m": 15, "40m": 22, "80m": 8}
     */
    private Map<String, Integer> bandBreakdown;

    /**
     * Mode breakdown: mode name -> QSO count
     * Example: {"SSB": 30, "CW": 7, "FT8": 8}
     */
    private Map<String, Integer> modeBreakdown;

    /**
     * Recent QSOs for this station (last 5-10)
     */
    private List<QSOResponse> recentQSOs;

    /**
     * Leaderboard rank (1st, 2nd, 3rd, etc.)
     * Null if not applicable or not enough stations
     */
    private Integer rank;

    /**
     * Operator username(s) assigned to this station
     */
    private List<String> operators;
}
