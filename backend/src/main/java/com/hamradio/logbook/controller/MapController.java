package com.hamradio.logbook.controller;

import com.hamradio.logbook.entity.QSOLocation;
import com.hamradio.logbook.service.MapDataService;
import com.hamradio.logbook.service.MapDataService.MapFilters;
import com.hamradio.logbook.service.GridCoverageService;
import com.hamradio.logbook.service.HeatmapService;
import com.hamradio.logbook.service.LocationManagementService;
import com.hamradio.logbook.service.ContestOverlayService;
import com.hamradio.logbook.service.MapExportService;
import com.hamradio.logbook.service.SessionLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

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
    private final LocationManagementService locationManagementService;
    private final ContestOverlayService contestOverlayService;
    private final MapExportService mapExportService;
    private final SessionLocationService sessionLocationService;

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

        GridCoverageService.GridCoverageResponse response = gridCoverageService.getGridCoverage(logId, precision, includeNeighbors, filters);

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
     * GET /api/maps/contest-overlays/{logId}?type=CQ_ZONES
     * GET /api/maps/contest-overlays/{logId}?type=ITU_ZONES
     * GET /api/maps/contest-overlays/{logId}?type=ARRL_SECTIONS
     * GET /api/maps/contest-overlays/{logId}?type=DXCC
     */
    @GetMapping("/contest-overlays/{logId}")
    public ResponseEntity<?> getContestOverlays(
            @PathVariable Long logId,
            @RequestParam(required = false, defaultValue = "CQ_ZONES") String type
    ) {
        log.info("GET /api/maps/contest-overlays/{} - type: {}", logId, type);

        return switch (type.toUpperCase()) {
            case "CQ_ZONES" -> {
                ContestOverlayService.ZoneOverlayResponse response = contestOverlayService.getCQZoneOverlay(logId);
                yield ResponseEntity.ok(response);
            }
            case "ITU_ZONES" -> {
                ContestOverlayService.ZoneOverlayResponse response = contestOverlayService.getITUZoneOverlay(logId);
                yield ResponseEntity.ok(response);
            }
            case "ARRL_SECTIONS" -> {
                ContestOverlayService.SectionOverlayResponse response = contestOverlayService.getARRLSectionOverlay(logId);
                yield ResponseEntity.ok(response);
            }
            case "DXCC" -> {
                ContestOverlayService.DXCCOverlayResponse response = contestOverlayService.getDXCCOverlay(logId);
                yield ResponseEntity.ok(response);
            }
            default -> ResponseEntity.badRequest().body("{\"error\": \"Invalid overlay type. Use: CQ_ZONES, ITU_ZONES, ARRL_SECTIONS, or DXCC\"}");
        };
    }

    /**
     * Get multiplier summary for a contest
     *
     * GET /api/maps/multipliers/{logId}?contest=ARRL_FIELD_DAY
     */
    @GetMapping("/multipliers/{logId}")
    public ResponseEntity<ContestOverlayService.MultiplierSummary> getMultipliers(
            @PathVariable Long logId,
            @RequestParam(required = true) ContestOverlayService.ContestType contest
    ) {
        log.info("GET /api/maps/multipliers/{} - contest: {}", logId, contest);

        ContestOverlayService.MultiplierSummary summary = contestOverlayService.getMultiplierSummary(logId, contest);

        return ResponseEntity.ok(summary);
    }

    /**
     * Export map data in various formats
     *
     * POST /api/maps/export/{logId}
     * Supported formats: GEOJSON, KML, CSV, ADIF
     */
    @PostMapping("/export/{logId}")
    public ResponseEntity<MapExportService.ExportResult> exportMapData(
            @PathVariable Long logId,
            @RequestBody ExportRequest request
    ) {
        log.info("POST /api/maps/export/{} - format: {}", logId, request.getFormat());

        MapFilters filters = request.getFilters();
        if (filters == null) {
            filters = MapFilters.builder().build();
        }

        MapExportService.ExportResult result = switch (request.getFormat().toUpperCase()) {
            case "GEOJSON" -> mapExportService.exportToGeoJSON(logId, filters);
            case "KML" -> mapExportService.exportToKML(logId, filters);
            case "CSV" -> mapExportService.exportToCSV(logId, filters);
            case "ADIF" -> mapExportService.exportToADIF(logId, filters);
            default -> MapExportService.ExportResult.builder()
                .format(request.getFormat())
                .success(false)
                .error("Unsupported format. Use: GEOJSON, KML, CSV, or ADIF")
                .build();
        };

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get cached distance calculation for QSO
     *
     * GET /api/maps/distance/{logId}/{qsoId}
     */
    @GetMapping("/distance/{logId}/{qsoId}")
    public ResponseEntity<DistanceResponse> getDistance(
            @PathVariable Long logId,
            @PathVariable Long qsoId
    ) {
        log.info("GET /api/maps/distance/{}/{}", logId, qsoId);

        QSOLocation location = mapDataService.getQSOLocationByQsoId(qsoId);

        if (location == null) {
            return ResponseEntity.notFound().build();
        }

        DistanceResponse response = DistanceResponse.builder()
            .qsoId(qsoId)
            .distanceKm(location.getDistanceKm())
            .distanceMi(location.getDistanceMi())
            .bearing(location.getBearing())
            .operatorLocation(location.getOperatorLat() != null ?
                DistanceResponse.LocationInfo.builder()
                    .lat(location.getOperatorLat())
                    .lon(location.getOperatorLon())
                    .grid(location.getOperatorGrid())
                    .build() : null)
            .contactLocation(DistanceResponse.LocationInfo.builder()
                .lat(location.getContactLat())
                .lon(location.getContactLon())
                .grid(location.getContactGrid())
                .build())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Set station location (operator position)
     *
     * PUT /api/maps/location/station/{stationId}
     */
    @PutMapping("/location/station/{stationId}")
    public ResponseEntity<LocationManagementService.LocationUpdateResponse> setStationLocation(
            @PathVariable Long stationId,
            @RequestBody LocationRequest request
    ) {
        log.info("PUT /api/maps/location/station/{} - lat: {}, lon: {}", stationId, request.getLatitude(), request.getLongitude());

        LocationManagementService.LocationUpdateResponse response = locationManagementService.updateStationLocation(
            stationId,
            request.getLatitude(),
            request.getLongitude(),
            request.getGrid(),
            request.getLocationName()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Set user default location (fallback)
     *
     * PUT /api/maps/location/user
     */
    @PutMapping("/location/user")
    public ResponseEntity<LocationManagementService.LocationUpdateResponse> setUserLocation(
            Authentication authentication,
            @RequestBody LocationRequest request
    ) {
        log.info("PUT /api/maps/location/user - lat: {}, lon: {}", request.getLatitude(), request.getLongitude());

        LocationManagementService.LocationUpdateResponse response = locationManagementService.updateUserLocation(
            authentication.getName(),
            request.getLatitude(),
            request.getLongitude(),
            request.getGrid()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Set temporary session location (override for current session)
     *
     * POST /api/maps/location/session/{logId}
     * DELETE /api/maps/location/session/{logId} (to clear)
     */
    @PostMapping("/location/session/{logId}")
    public ResponseEntity<SessionLocationService.SessionLocationResponse> setSessionLocation(
            @PathVariable Long logId,
            @RequestBody LocationRequest request
    ) {
        log.info("POST /api/maps/location/session/{} - lat: {}, lon: {}", logId, request.getLatitude(), request.getLongitude());

        SessionLocationService.SessionLocationResponse response = sessionLocationService.setSessionLocation(
            logId,
            request.getLatitude(),
            request.getLongitude(),
            request.getGrid()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Clear session location for a log
     *
     * DELETE /api/maps/location/session/{logId}
     */
    @DeleteMapping("/location/session/{logId}")
    public ResponseEntity<Void> clearSessionLocation(@PathVariable Long logId) {
        log.info("DELETE /api/maps/location/session/{}", logId);
        sessionLocationService.clearSessionLocation(logId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get session location for a log
     *
     * GET /api/maps/location/session/{logId}
     */
    @GetMapping("/location/session/{logId}")
    public ResponseEntity<SessionLocationService.SessionLocation> getSessionLocation(@PathVariable Long logId) {
        log.info("GET /api/maps/location/session/{}", logId);

        SessionLocationService.SessionLocation location = sessionLocationService.getSessionLocation(logId);

        if (location == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(location);
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

    @lombok.Data
    @lombok.Builder
    public static class DistanceResponse {
        private Long qsoId;
        private java.math.BigDecimal distanceKm;
        private java.math.BigDecimal distanceMi;
        private java.math.BigDecimal bearing;
        private LocationInfo operatorLocation;
        private LocationInfo contactLocation;

        @lombok.Data
        @lombok.Builder
        public static class LocationInfo {
            private java.math.BigDecimal lat;
            private java.math.BigDecimal lon;
            private String grid;
        }
    }
}
