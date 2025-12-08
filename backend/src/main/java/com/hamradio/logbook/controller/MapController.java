package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.MapDataService;
import com.hamradio.logbook.service.MapDataService.MapFilters;
import com.hamradio.logbook.service.GridCoverageService;
import com.hamradio.logbook.service.HeatmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST API controller for map data endpoints
 * Provides QSO location data, clustering, grid coverage, and heatmap functionality
 */
@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // TODO: Configure properly in production
public class MapController {

    private final MapDataService mapDataService;
    private final GridCoverageService gridCoverageService;
    private final HeatmapService heatmapService;

    /**
     * Get QSO location data with adaptive clustering
     *
     * GET /api/maps/qsos/{logId}?zoom=10&band=20M&mode=SSB&station=1
     */
    @GetMapping("/qsos/{logId}")
    public ResponseEntity<MapDataService.MapDataResponse> getQSOLocations(
            @PathVariable Long logId,
            @RequestParam(required = true) int zoom,
            @RequestParam(required = false) String bounds,
            @RequestParam(required = false, defaultValue = "10000") int clusterThreshold,
            @RequestParam(required = false) Integer pixelRadius,
            // Filters
            @RequestParam(required = false) String band,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer station,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String dxcc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean confirmed,
            @RequestParam(required = false) String continent,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String exchange
    ) {
        log.info("GET /api/maps/qsos/{} - zoom: {}, filters applied: {}", logId, zoom, hasFilters(band, mode, station));

        // Build filters
        MapFilters filters = MapFilters.builder()
                .band(band)
                .mode(mode)
                .station(station)
                .operator(operator)
                .dxcc(dxcc)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .confirmed(confirmed)
                .continent(continent)
                .state(state)
                .exchange(exchange)
                .build();

        MapDataService.MapDataResponse response = mapDataService.getQSOLocations(logId, zoom, filters, bounds);

        return ResponseEntity.ok(response);
    }

    /**
     * Get worked grid squares for coverage map
     *
     * GET /api/maps/grids/{logId}?precision=6&includeNeighbors=true
     */
    @GetMapping("/grids/{logId}")
    public ResponseEntity<GridCoverageService.GridCoverageResponse> getGridCoverage(
            @PathVariable Long logId,
            @RequestParam(required = false) Integer precision,
            @RequestParam(required = false, defaultValue = "true") boolean includeNeighbors,
            // Filters (same as QSO endpoint)
            @RequestParam(required = false) String band,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer station,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String dxcc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean confirmed,
            @RequestParam(required = false) String continent,
            @RequestParam(required = false) String state
    ) {
        log.info("GET /api/maps/grids/{} - precision: {}, includeNeighbors: {}", logId, precision, includeNeighbors);

        // TODO: Apply filters to grid coverage
        GridCoverageService.GridCoverageResponse response = gridCoverageService.getGridCoverage(logId, precision, includeNeighbors);

        return ResponseEntity.ok(response);
    }

    /**
     * Get heatmap density data
     *
     * GET /api/maps/heatmap/{logId}
     */
    @GetMapping("/heatmap/{logId}")
    public ResponseEntity<HeatmapService.HeatmapResponse> getHeatmapData(
            @PathVariable Long logId,
            @RequestParam(required = false, defaultValue = "false") boolean gridBased,
            @RequestParam(required = false, defaultValue = "4") int gridPrecision,
            // Filters (same as QSO endpoint)
            @RequestParam(required = false) String band,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) Integer station,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String dxcc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean confirmed,
            @RequestParam(required = false) String continent,
            @RequestParam(required = false) String state
    ) {
        log.info("GET /api/maps/heatmap/{} - gridBased: {}, precision: {}", logId, gridBased, gridPrecision);

        // Build filters
        MapFilters filters = MapFilters.builder()
                .band(band)
                .mode(mode)
                .station(station)
                .operator(operator)
                .dxcc(dxcc)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .confirmed(confirmed)
                .continent(continent)
                .state(state)
                .build();

        HeatmapService.HeatmapResponse response;
        if (gridBased) {
            response = heatmapService.getGridBasedHeatmap(logId, filters, gridPrecision);
        } else {
            response = heatmapService.getHeatmapData(logId, filters);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get contest-specific overlay data
     *
     * GET /api/maps/contest-overlays/{logId}
     */
    @GetMapping("/contest-overlays/{logId}")
    public ResponseEntity<?> getContestOverlays(@PathVariable Long logId) {
        log.info("GET /api/maps/contest-overlays/{}", logId);

        // TODO: Implement contest overlay service
        return ResponseEntity.ok().body("{\"message\": \"Contest overlay endpoint - implementation pending\"}");
    }

    /**
     * Export map data in various formats
     *
     * POST /api/maps/export/{logId}
     */
    @PostMapping("/export/{logId}")
    public ResponseEntity<?> exportMapData(
            @PathVariable Long logId,
            @RequestBody ExportRequest request
    ) {
        log.info("POST /api/maps/export/{} - format: {}", logId, request.getFormat());

        // TODO: Implement export service
        return ResponseEntity.ok().body("{\"message\": \"Export endpoint - implementation pending\"}");
    }

    /**
     * Get cached distance calculation for QSO
     *
     * GET /api/maps/distance/{logId}/{qsoId}
     */
    @GetMapping("/distance/{logId}/{qsoId}")
    public ResponseEntity<?> getDistance(
            @PathVariable Long logId,
            @PathVariable Long qsoId
    ) {
        log.info("GET /api/maps/distance/{}/{}", logId, qsoId);

        // TODO: Implement distance retrieval
        return ResponseEntity.ok().body("{\"message\": \"Distance endpoint - implementation pending\"}");
    }

    /**
     * Set station location (operator position)
     *
     * PUT /api/maps/location/station/{stationId}
     */
    @PutMapping("/location/station/{stationId}")
    public ResponseEntity<?> setStationLocation(
            @PathVariable Long stationId,
            @RequestBody LocationRequest request
    ) {
        log.info("PUT /api/maps/location/station/{} - lat: {}, lon: {}", stationId, request.getLatitude(), request.getLongitude());

        // TODO: Implement station location update
        return ResponseEntity.ok().body("{\"message\": \"Station location update - implementation pending\"}");
    }

    /**
     * Set user default location (fallback)
     *
     * PUT /api/maps/location/user
     */
    @PutMapping("/location/user")
    public ResponseEntity<?> setUserLocation(@RequestBody LocationRequest request) {
        log.info("PUT /api/maps/location/user - lat: {}, lon: {}", request.getLatitude(), request.getLongitude());

        // TODO: Implement user location update
        return ResponseEntity.ok().body("{\"message\": \"User location update - implementation pending\"}");
    }

    /**
     * Set temporary session location (override for current session)
     *
     * POST /api/maps/location/session/{logId}
     */
    @PostMapping("/location/session/{logId}")
    public ResponseEntity<?> setSessionLocation(
            @PathVariable Long logId,
            @RequestBody LocationRequest request
    ) {
        log.info("POST /api/maps/location/session/{} - lat: {}, lon: {}", logId, request.getLatitude(), request.getLongitude());

        // TODO: Implement session location
        return ResponseEntity.ok().body("{\"message\": \"Session location - implementation pending\"}");
    }

    // ===== Helper Methods =====

    private boolean hasFilters(String band, String mode, Integer station) {
        return band != null || mode != null || station != null;
    }

    // ===== Request/Response DTOs =====

    @lombok.Data
    public static class ExportRequest {
        private String format; // PNG, SVG, CSV, ADIF, KML, GEOJSON
        private MapFilters filters;
        private boolean includeMetadata;
        private boolean includeStyles;
    }

    @lombok.Data
    public static class LocationRequest {
        private Double latitude;
        private Double longitude;
        private String grid; // Optional, can be auto-calculated
        private String locationName;
    }
}
