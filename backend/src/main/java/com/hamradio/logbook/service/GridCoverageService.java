package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.MaidenheadGrid;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.MaidenheadGridRepository;
import com.hamradio.logbook.repository.QSORepository;
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
 * Service for grid square coverage calculations
 * Manages Maidenhead grid statistics per log
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GridCoverageService {

    private final MaidenheadGridRepository gridRepository;
    private final QSORepository qsoRepository;
    private final LogRepository logRepository;
    private final MaidenheadConverter maidenheadConverter;
    private final MapDataService mapDataService;

    /**
     * Get grid coverage for a log
     *
     * @param logId Log ID
     * @param precision Grid precision (2, 4, 6, or 8) - auto-detect if null
     * @param includeNeighbors Include neighboring grids for visualization
     * @param filters Optional filters to apply to QSOs
     * @return Grid coverage response
     */
    @Transactional(readOnly = true)
    public GridCoverageResponse getGridCoverage(Long logId, Integer precision, boolean includeNeighbors, MapDataService.MapFilters filters) {
        log.info("Getting grid coverage for log {} with precision {}", logId, precision);

        // Get filtered QSOs with grid squares
        List<QSO> qsos = mapDataService.getFilteredQSOs(logId, filters).stream()
                .filter(qso -> qso.getGridSquare() != null && !qso.getGridSquare().isBlank())
                .collect(Collectors.toList());

        // Auto-detect precision if not specified
        if (precision == null) {
            precision = maidenheadConverter.detectPrecision(qsos.size());
            log.info("Auto-detected precision: {} for {} QSOs", precision, qsos.size());
        }

        // Normalize all grid squares to specified precision
        Map<String, List<QSO>> qsosByGrid = new HashMap<>();
        for (QSO qso : qsos) {
            String normalizedGrid = normalizeGrid(qso.getGridSquare(), precision);
            if (normalizedGrid != null) {
                qsosByGrid.computeIfAbsent(normalizedGrid, k -> new ArrayList<>()).add(qso);
            }
        }

        // Build grid data
        List<GridData> grids = new ArrayList<>();
        for (Map.Entry<String, List<QSO>> entry : qsosByGrid.entrySet()) {
            String grid = entry.getKey();
            List<QSO> gridQSOs = entry.getValue();

            grids.add(buildGridData(grid, gridQSOs, precision));
        }

        // Add neighboring grids if requested
        if (includeNeighbors) {
            Set<String> workedGrids = new HashSet<>(qsosByGrid.keySet());
            Set<String> neighbors = getNeighboringGrids(workedGrids, precision);

            for (String neighbor : neighbors) {
                if (!workedGrids.contains(neighbor)) {
                    grids.add(buildEmptyGridData(neighbor, precision));
                }
            }
        }

        return GridCoverageResponse.builder()
                .grids(grids)
                .metadata(GridMetadata.builder()
                        .totalGrids(grids.size())
                        .workedGrids(qsosByGrid.size())
                        .precision(precision)
                        .autoDetected(precision == null)
                        .build())
                .build();
    }

    /**
     * Update or create grid statistics for a log
     */
    @Transactional
    public void updateGridStatistics(Long logId) {
        log.info("Updating grid statistics for log {}", logId);

        Log logEntity = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found: " + logId));

        // Get all QSOs with grids
        List<QSO> qsos = qsoRepository.findAllByLogId(logId).stream()
                .filter(qso -> qso.getGridSquare() != null && !qso.getGridSquare().isBlank())
                .collect(Collectors.toList());

        // Detect precision
        int precision = maidenheadConverter.detectPrecision(qsos.size());

        // Group by grid
        Map<String, List<QSO>> qsosByGrid = qsos.stream()
                .collect(Collectors.groupingBy(qso -> normalizeGrid(qso.getGridSquare(), precision)));

        // Update or create grid entries
        for (Map.Entry<String, List<QSO>> entry : qsosByGrid.entrySet()) {
            String grid = entry.getKey();
            List<QSO> gridQSOs = entry.getValue();

            if (grid == null) continue;

            Optional<MaidenheadGrid> existing = gridRepository.findByGridAndLogIdAndPrecision(grid, logId, precision);

            if (existing.isPresent()) {
                updateGridEntity(existing.get(), gridQSOs);
            } else {
                createGridEntity(logEntity, grid, gridQSOs, precision);
            }
        }

        log.info("Updated {} grid entries for log {}", qsosByGrid.size(), logId);
    }

    /**
     * Normalize grid square to specified precision
     */
    private String normalizeGrid(String grid, int precision) {
        if (grid == null || grid.isBlank()) {
            return null;
        }

        // Ensure grid is uppercase
        grid = grid.toUpperCase().trim();

        // Truncate to desired precision
        if (grid.length() > precision) {
            return grid.substring(0, precision);
        } else if (grid.length() == precision) {
            return grid;
        } else {
            // Grid is too short - invalid
            log.warn("Grid square too short: {} (expected at least {} chars)", grid, precision);
            return null;
        }
    }

    /**
     * Build grid data from QSOs
     */
    private GridData buildGridData(String grid, List<QSO> qsos, int precision) {
        MaidenheadConverter.GridLocation location = maidenheadConverter.fromMaidenhead(grid);
        MaidenheadConverter.GridBounds bounds = maidenheadConverter.getBounds(grid);

        // Count unique bands and modes
        Set<String> bands = qsos.stream()
                .map(QSO::getBand)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> modes = qsos.stream()
                .map(QSO::getMode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Get first and last QSO dates
        Optional<QSO> firstQSO = qsos.stream()
                .min(Comparator.comparing(QSO::getQsoDate));
        Optional<QSO> lastQSO = qsos.stream()
                .max(Comparator.comparing(QSO::getQsoDate));

        return GridData.builder()
                .grid(grid)
                .precision(precision)
                .centerLat(location.getLat())
                .centerLon(location.getLon())
                .bounds(BoundsData.builder()
                        .minLat(bounds.getMinLat())
                        .maxLat(bounds.getMaxLat())
                        .minLon(bounds.getMinLon())
                        .maxLon(bounds.getMaxLon())
                        .build())
                .qsoCount(qsos.size())
                .bandCount(bands.size())
                .modeCount(modes.size())
                .firstQso(firstQSO.map(QSO::getCreatedAt).orElse(null))
                .lastQso(lastQSO.map(QSO::getCreatedAt).orElse(null))
                .build();
    }

    /**
     * Build empty grid data for neighbors
     */
    private GridData buildEmptyGridData(String grid, int precision) {
        MaidenheadConverter.GridLocation location = maidenheadConverter.fromMaidenhead(grid);
        MaidenheadConverter.GridBounds bounds = maidenheadConverter.getBounds(grid);

        return GridData.builder()
                .grid(grid)
                .precision(precision)
                .centerLat(location.getLat())
                .centerLon(location.getLon())
                .bounds(BoundsData.builder()
                        .minLat(bounds.getMinLat())
                        .maxLat(bounds.getMaxLat())
                        .minLon(bounds.getMinLon())
                        .maxLon(bounds.getMaxLon())
                        .build())
                .qsoCount(0)
                .bandCount(0)
                .modeCount(0)
                .build();
    }

    /**
     * Get neighboring grids (8 surrounding grids)
     */
    private Set<String> getNeighboringGrids(Set<String> workedGrids, int precision) {
        Set<String> neighbors = new HashSet<>();

        for (String grid : workedGrids) {
            try {
                // Get center of grid
                MaidenheadConverter.GridLocation location = maidenheadConverter.fromMaidenhead(grid);

                // Calculate grid size
                double gridSize = getGridSizeDegrees(precision);

                // Generate 8 neighboring grid centers (N, NE, E, SE, S, SW, W, NW)
                double[][] offsets = {
                    {0, gridSize},       // N
                    {gridSize, gridSize}, // NE
                    {gridSize, 0},       // E
                    {gridSize, -gridSize}, // SE
                    {0, -gridSize},      // S
                    {-gridSize, -gridSize}, // SW
                    {-gridSize, 0},      // W
                    {-gridSize, gridSize}  // NW
                };

                for (double[] offset : offsets) {
                    BigDecimal neighborLat = location.getLat().add(BigDecimal.valueOf(offset[1]));
                    BigDecimal neighborLon = location.getLon().add(BigDecimal.valueOf(offset[0]));

                    // Convert back to grid
                    String neighborGrid = maidenheadConverter.toMaidenhead(neighborLat, neighborLon, precision);
                    neighbors.add(neighborGrid);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate neighbors for grid {}: {}", grid, e.getMessage());
            }
        }

        return neighbors;
    }

    /**
     * Get grid size in degrees for given precision
     */
    private double getGridSizeDegrees(int precision) {
        return switch (precision) {
            case 2 -> 10.0;           // Field (10° latitude)
            case 4 -> 1.0;            // Square (1° latitude)
            case 6 -> 2.5 / 60.0;     // Subsquare (2.5' latitude)
            case 8 -> 15.0 / 3600.0;  // Extended (15" latitude)
            default -> 1.0;
        };
    }

    /**
     * Update existing grid entity with QSO statistics
     */
    private void updateGridEntity(MaidenheadGrid grid, List<QSO> qsos) {
        grid.setQsoCount(qsos.size());

        Set<String> bands = qsos.stream()
                .map(QSO::getBand)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        grid.setBandCount(bands.size());

        Set<String> modes = qsos.stream()
                .map(QSO::getMode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        grid.setModeCount(modes.size());

        qsos.stream()
                .min(Comparator.comparing(QSO::getQsoDate))
                .ifPresent(qso -> grid.setFirstQsoDate(qso.getCreatedAt()));

        qsos.stream()
                .max(Comparator.comparing(QSO::getQsoDate))
                .ifPresent(qso -> grid.setLastQsoDate(qso.getCreatedAt()));

        gridRepository.save(grid);
    }

    /**
     * Create new grid entity
     */
    private void createGridEntity(Log log, String grid, List<QSO> qsos, int precision) {
        MaidenheadConverter.GridLocation location = maidenheadConverter.fromMaidenhead(grid);
        MaidenheadConverter.GridBounds bounds = maidenheadConverter.getBounds(grid);

        Set<String> bands = qsos.stream()
                .map(QSO::getBand)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> modes = qsos.stream()
                .map(QSO::getMode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Optional<QSO> firstQSO = qsos.stream().min(Comparator.comparing(QSO::getQsoDate));
        Optional<QSO> lastQSO = qsos.stream().max(Comparator.comparing(QSO::getQsoDate));

        MaidenheadGrid gridEntity = MaidenheadGrid.builder()
                .grid(grid)
                .precision(precision)
                .centerLat(location.getLat())
                .centerLon(location.getLon())
                .minLat(bounds.getMinLat())
                .maxLat(bounds.getMaxLat())
                .minLon(bounds.getMinLon())
                .maxLon(bounds.getMaxLon())
                .log(log)
                .qsoCount(qsos.size())
                .bandCount(bands.size())
                .modeCount(modes.size())
                .firstQsoDate(firstQSO.map(QSO::getCreatedAt).orElse(null))
                .lastQsoDate(lastQSO.map(QSO::getCreatedAt).orElse(null))
                .build();

        gridRepository.save(gridEntity);
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class GridCoverageResponse {
        private List<GridData> grids;
        private GridMetadata metadata;
    }

    @Data
    @Builder
    public static class GridData {
        private String grid;
        private Integer precision;
        private BigDecimal centerLat;
        private BigDecimal centerLon;
        private BoundsData bounds;
        private Integer qsoCount;
        private Integer bandCount;
        private Integer modeCount;
        private java.time.LocalDateTime firstQso;
        private java.time.LocalDateTime lastQso;
    }

    @Data
    @Builder
    public static class BoundsData {
        private BigDecimal minLat;
        private BigDecimal maxLat;
        private BigDecimal minLon;
        private BigDecimal maxLon;
    }

    @Data
    @Builder
    public static class GridMetadata {
        private Integer totalGrids;
        private Integer workedGrids;
        private Integer precision;
        private Boolean autoDetected;
    }
}
