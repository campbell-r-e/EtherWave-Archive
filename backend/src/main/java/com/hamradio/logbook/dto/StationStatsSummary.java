package com.hamradio.logbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for overall multi-station contest summary
 * Separates main contest stations from GOTA for scoring compliance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationStatsSummary {

    /**
     * Statistics for all main contest stations (non-GOTA)
     */
    private List<StationStatistics> mainStations;

    /**
     * Statistics for GOTA station (if present)
     */
    private StationStatistics gota;

    /**
     * Combined totals for main stations only (GOTA excluded for contest scoring)
     */
    private MainTotal mainTotal;

    /**
     * Overall totals including GOTA (for informational purposes)
     */
    private OverallTotal overallTotal;

    /**
     * Total for main contest stations (GOTA excluded)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainTotal {
        private Integer qsoCount;
        private Integer points;
        private Double qsoRate;
    }

    /**
     * Overall total including GOTA (informational only)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallTotal {
        private Integer qsoCount;
    }
}
