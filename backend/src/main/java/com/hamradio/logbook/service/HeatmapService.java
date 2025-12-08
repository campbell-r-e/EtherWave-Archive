package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.entity.QSOLocation;
import com.hamradio.logbook.repository.QSOLocationRepository;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.service.MapDataService.MapFilters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for heatmap density calculations
 * Aggregates QSO locations for density visualization
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HeatmapService {

    private final QSOLocationRepository qsoLocationRepository;
    private final QSORepository qsoRepository;

    private static final int HEATMAP_THRESHOLD = 15000; // Max points for performance

    /**
     * Get heatmap density data for a log
     *
     * @param logId Log ID
     * @param filters Optional filters
     * @return Heatmap response with density points
     */
    @Transactional(readOnly = true)
    public HeatmapResponse getHeatmapData(Long logId, MapFilters filters) {
        log.info("Getting heatmap data for log {}", logId);

        // Get filtered QSOs
        List<QSO> qsos = getFilteredQSOs(logId, filters);

        // Get locations for QSOs
        List<HeatmapPoint> points = new ArrayList<>();
        Map<String, Integer> locationCounts = new HashMap<>();

        for (QSO qso : qsos) {
            Optional<QSOLocation> location = qsoLocationRepository.findByQsoId(qso.getId());
            if (location.isPresent() && location.get().getContactLat() != null && location.get().getContactLon() != null) {
                // Create location key for aggregation (round to 2 decimal places)
                String locationKey = String.format("%.2f,%.2f",
                    location.get().getContactLat().doubleValue(),
                    location.get().getContactLon().doubleValue());

                locationCounts.put(locationKey, locationCounts.getOrDefault(locationKey, 0) + 1);
            }
        }

        // Find max intensity for normalization
        int maxIntensity = locationCounts.values().stream()
            .max(Integer::compareTo)
            .orElse(1);

        // Build heatmap points with normalized intensity
        for (Map.Entry<String, Integer> entry : locationCounts.entrySet()) {
            String[] coords = entry.getKey().split(",");
            double lat = Double.parseDouble(coords[0]);
            double lon = Double.parseDouble(coords[1]);
            int count = entry.getValue();

            // Normalize intensity to 0.0-1.0 range
            double intensity = (double) count / maxIntensity;

            points.add(HeatmapPoint.builder()
                .lat(BigDecimal.valueOf(lat))
                .lon(BigDecimal.valueOf(lon))
                .intensity(intensity)
                .count(count)
                .build());
        }

        // Sort by intensity descending
        points.sort((a, b) -> Double.compare(b.getIntensity(), a.getIntensity()));

        // Limit to threshold for performance
        if (points.size() > HEATMAP_THRESHOLD) {
            log.warn("Heatmap has {} points, limiting to {} for performance", points.size(), HEATMAP_THRESHOLD);
            points = points.subList(0, HEATMAP_THRESHOLD);
        }

        return HeatmapResponse.builder()
            .points(points)
            .metadata(HeatmapMetadata.builder()
                .totalPoints(points.size())
                .maxIntensity(maxIntensity)
                .filtered(filters != null)
                .limited(locationCounts.size() > HEATMAP_THRESHOLD)
                .build())
            .build();
    }

    /**
     * Get grid-based heatmap (aggregated by grid square)
     * More performant for large datasets
     */
    @Transactional(readOnly = true)
    public HeatmapResponse getGridBasedHeatmap(Long logId, MapFilters filters, int gridPrecision) {
        log.info("Getting grid-based heatmap for log {} with precision {}", logId, gridPrecision);

        // Get filtered QSOs
        List<QSO> qsos = getFilteredQSOs(logId, filters);

        // Group by grid square
        Map<String, Integer> gridCounts = new HashMap<>();
        for (QSO qso : qsos) {
            if (qso.getGridSquare() != null && !qso.getGridSquare().isBlank()) {
                String grid = normalizeGrid(qso.getGridSquare(), gridPrecision);
                if (grid != null) {
                    gridCounts.put(grid, gridCounts.getOrDefault(grid, 0) + 1);
                }
            }
        }

        // Find max intensity
        int maxIntensity = gridCounts.values().stream()
            .max(Integer::compareTo)
            .orElse(1);

        // Build heatmap points from grid centers
        List<HeatmapPoint> points = new ArrayList<>();
        MaidenheadConverter converter = new MaidenheadConverter();

        for (Map.Entry<String, Integer> entry : gridCounts.entrySet()) {
            try {
                MaidenheadConverter.GridLocation location = converter.fromMaidenhead(entry.getKey());
                int count = entry.getValue();
                double intensity = (double) count / maxIntensity;

                points.add(HeatmapPoint.builder()
                    .lat(location.getLat())
                    .lon(location.getLon())
                    .intensity(intensity)
                    .count(count)
                    .grid(entry.getKey())
                    .build());
            } catch (Exception e) {
                log.warn("Failed to convert grid {} to location: {}", entry.getKey(), e.getMessage());
            }
        }

        return HeatmapResponse.builder()
            .points(points)
            .metadata(HeatmapMetadata.builder()
                .totalPoints(points.size())
                .maxIntensity(maxIntensity)
                .filtered(filters != null)
                .limited(false)
                .gridBased(true)
                .gridPrecision(gridPrecision)
                .build())
            .build();
    }

    /**
     * Get filtered QSOs based on map filters
     */
    private List<QSO> getFilteredQSOs(Long logId, MapFilters filters) {
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        if (filters == null) {
            return qsos;
        }

        return qsos.stream()
            .filter(qso -> filters.getBand() == null || filters.getBand().equalsIgnoreCase(qso.getBand()))
            .filter(qso -> filters.getMode() == null || filters.getMode().equalsIgnoreCase(qso.getMode()))
            .filter(qso -> filters.getStation() == null || filters.getStation().equals(qso.getStationNumber()))
            .filter(qso -> filters.getOperator() == null ||
                (qso.getOperator() != null && filters.getOperator().equalsIgnoreCase(qso.getOperator().getCallsign())))
            .filter(qso -> filters.getState() == null || filters.getState().equalsIgnoreCase(qso.getState()))
            .filter(qso -> filters.getConfirmed() == null || filters.getConfirmed() == isConfirmed(qso))
            .filter(qso -> filters.getDateFrom() == null || !qso.getQsoDate().isBefore(filters.getDateFrom()))
            .filter(qso -> filters.getDateTo() == null || !qso.getQsoDate().isAfter(filters.getDateTo()))
            .collect(Collectors.toList());
    }

    /**
     * Check if QSO is confirmed
     */
    private boolean isConfirmed(QSO qso) {
        return "Y".equalsIgnoreCase(qso.getQslRcvd()) || "Y".equalsIgnoreCase(qso.getLotwRcvd());
    }

    /**
     * Normalize grid square to specified precision
     */
    private String normalizeGrid(String grid, int precision) {
        if (grid == null || grid.isBlank()) {
            return null;
        }

        grid = grid.toUpperCase().trim();

        if (grid.length() >= precision) {
            return grid.substring(0, precision);
        }

        return null;
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class HeatmapResponse {
        private List<HeatmapPoint> points;
        private HeatmapMetadata metadata;
    }

    @Data
    @Builder
    public static class HeatmapPoint {
        private BigDecimal lat;
        private BigDecimal lon;
        private double intensity; // 0.0 to 1.0
        private int count; // Actual QSO count
        private String grid; // Optional grid square
    }

    @Data
    @Builder
    public static class HeatmapMetadata {
        private int totalPoints;
        private int maxIntensity;
        private boolean filtered;
        private boolean limited; // True if points were limited for performance
        private boolean gridBased; // True if using grid-based aggregation
        private Integer gridPrecision;
    }
}
