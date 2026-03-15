package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.CallsignInfo;
import com.hamradio.logbook.service.CallsignValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for callsign validation and lookup
 */
@RestController
@RequestMapping("/api/callsigns")
@RequiredArgsConstructor
public class CallsignController {

    private final CallsignValidationService validationService;

    /**
     * Lookup callsign information
     * GET /api/callsigns/{callsign}
     */
    @GetMapping("/{callsign}")
    public ResponseEntity<CallsignInfo> lookupCallsign(@PathVariable String callsign) {
        return validationService.lookupCallsign(callsign)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cleanup expired cache entries
     * POST /api/callsigns/cache/cleanup
     */
    @PostMapping("/cache/cleanup")
    public ResponseEntity<Void> cleanupCache() {
        validationService.cleanupExpiredCache();
        return ResponseEntity.ok().build();
    }
}
