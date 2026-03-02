package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.DXClusterService;
import com.hamradio.logbook.service.DXClusterService.DXSpot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for live DX cluster spot feeds
 * Proxies DX Summit public API so the frontend never calls external services directly
 */
@RestController
@RequestMapping("/api/dx-cluster")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // TODO: Configure properly in production
public class DXClusterController {

    private final DXClusterService dxClusterService;

    /**
     * GET /api/dx-cluster/spots
     *
     * Returns recent DX spots.
     *
     * @param limit  Number of spots to fetch (default 50, max 200)
     * @param band   Optional band filter — "20m", "40m", etc. Empty for all bands.
     */
    @GetMapping("/spots")
    public ResponseEntity<List<DXSpot>> getSpots(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String band) {
        log.debug("DX cluster spots requested: limit={}, band={}", limit, band);
        List<DXSpot> spots = dxClusterService.getSpots(limit, band);
        return ResponseEntity.ok(spots);
    }
}
