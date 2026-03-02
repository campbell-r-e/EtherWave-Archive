package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.PropagationService;
import com.hamradio.logbook.service.PropagationService.PropagationConditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for HF propagation prediction
 * Proxies NOAA solar/geomagnetic data and maps to per-band conditions
 */
@RestController
@RequestMapping("/api/propagation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // TODO: Configure properly in production
public class PropagationController {

    private final PropagationService propagationService;

    /**
     * GET /api/propagation/conditions
     * Returns current band conditions derived from NOAA solar/geomagnetic data.
     * Response is cached for 30 minutes to limit NOAA API calls.
     */
    @GetMapping("/conditions")
    public ResponseEntity<PropagationConditions> getConditions() {
        log.debug("Propagation conditions requested");
        return ResponseEntity.ok(propagationService.getConditions());
    }
}
