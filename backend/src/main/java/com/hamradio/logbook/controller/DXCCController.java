package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.DXCCLoaderService;
import com.hamradio.logbook.service.DXCCLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST API controller for DXCC prefix management
 * Provides endpoints for loading CTY.DAT and callsign lookups
 */
@RestController
@RequestMapping("/api/dxcc")
@RequiredArgsConstructor
@Slf4j
public class DXCCController {

    private final DXCCLoaderService dxccLoaderService;
    private final DXCCLookupService dxccLookupService;

    /**
     * Load DXCC data from uploaded CTY.DAT file
     *
     * POST /api/dxcc/load
     */
    @PostMapping("/load")
    public ResponseEntity<DXCCLoaderService.LoadResult> loadFromFile(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Loading DXCC data from uploaded file: {}", file.getOriginalFilename());

        if (!file.getOriginalFilename().toLowerCase().endsWith(".dat")) {
            return ResponseEntity.badRequest().body(
                DXCCLoaderService.LoadResult.builder()
                    .success(false)
                    .error("File must be a .dat file (CTY.DAT format)")
                    .build()
            );
        }

        try {
            DXCCLoaderService.LoadResult result = dxccLoaderService.loadFromInputStream(file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Failed to load DXCC data from file", e);
            return ResponseEntity.internalServerError().body(
                DXCCLoaderService.LoadResult.builder()
                    .success(false)
                    .error("Failed to load file: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Load DXCC data from bundled resource
     *
     * POST /api/dxcc/load-default
     */
    @PostMapping("/load-default")
    public ResponseEntity<DXCCLoaderService.LoadResult> loadDefaultData() {
        log.info("Loading default DXCC data from classpath");

        try {
            // Try to load from classpath resource
            DXCCLoaderService.LoadResult result = dxccLoaderService.loadFromClasspath("data/cty.dat");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to load default DXCC data", e);
            return ResponseEntity.internalServerError().body(
                DXCCLoaderService.LoadResult.builder()
                    .success(false)
                    .error("Failed to load default data: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Check if DXCC database is loaded
     *
     * GET /api/dxcc/status
     */
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {
        boolean isLoaded = dxccLoaderService.isLoaded();
        long prefixCount = dxccLoaderService.getPrefixCount();

        return ResponseEntity.ok(
            StatusResponse.builder()
                .loaded(isLoaded)
                .prefixCount(prefixCount)
                .message(isLoaded ?
                    "DXCC database loaded with " + prefixCount + " prefixes" :
                    "DXCC database not loaded")
                .build()
        );
    }

    /**
     * Look up DXCC information for a callsign
     *
     * GET /api/dxcc/lookup/{callsign}
     */
    @GetMapping("/lookup/{callsign}")
    public ResponseEntity<DXCCLookupService.DXCCInfo> lookupCallsign(
            @PathVariable String callsign
    ) {
        log.info("Looking up DXCC for callsign: {}", callsign);

        DXCCLookupService.DXCCInfo info = dxccLookupService.lookup(callsign);

        if (info == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(info);
    }

    /**
     * Look up DXCC information by DXCC code
     *
     * GET /api/dxcc/code/{dxccCode}
     */
    @GetMapping("/code/{dxccCode}")
    public ResponseEntity<DXCCLookupService.DXCCInfo> lookupByCode(
            @PathVariable Integer dxccCode
    ) {
        log.info("Looking up DXCC code: {}", dxccCode);

        DXCCLookupService.DXCCInfo info = dxccLookupService.lookupByCode(dxccCode);

        if (info == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(info);
    }

    /**
     * Get all DXCC entities for a continent
     *
     * GET /api/dxcc/continent/{continent}
     */
    @GetMapping("/continent/{continent}")
    public ResponseEntity<List<DXCCLookupService.DXCCInfo>> getEntitiesByContinent(
            @PathVariable String continent
    ) {
        log.info("Getting DXCC entities for continent: {}", continent);

        List<DXCCLookupService.DXCCInfo> entities = dxccLookupService.getEntitiesByContinent(continent);

        return ResponseEntity.ok(entities);
    }

    /**
     * Get location for callsign
     *
     * GET /api/dxcc/location/{callsign}
     */
    @GetMapping("/location/{callsign}")
    public ResponseEntity<DXCCLookupService.LocationInfo> getLocationForCallsign(
            @PathVariable String callsign
    ) {
        log.info("Getting location for callsign: {}", callsign);

        DXCCLookupService.LocationInfo location = dxccLookupService.getLocationForCallsign(callsign);

        if (location == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(location);
    }

    // ===== Data Transfer Objects =====

    @lombok.Data
    @lombok.Builder
    public static class StatusResponse {
        private boolean loaded;
        private long prefixCount;
        private String message;
    }
}
