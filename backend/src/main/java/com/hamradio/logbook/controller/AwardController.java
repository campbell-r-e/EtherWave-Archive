package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.AwardTrackingService;
import com.hamradio.logbook.service.AwardTrackingService.AwardProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for award tracking
 * Provides per-log progress toward DXCC, WAS, and VUCC awards
 */
@RestController
@RequestMapping("/api/awards")
@RequiredArgsConstructor
@Slf4j
public class AwardController {

    private final AwardTrackingService awardTrackingService;

    /**
     * GET /api/awards/{logId}
     * Returns award progress (DXCC, WAS, VUCC) for the specified log.
     * Requires the authenticated user to have access to the log.
     */
    @GetMapping("/{logId}")
    public ResponseEntity<AwardProgress> getAwardProgress(
            @PathVariable Long logId,
            Authentication auth) {
        log.debug("Award progress requested for log {} by {}", logId, auth.getName());
        AwardProgress progress = awardTrackingService.getProgress(logId, auth.getName());
        return ResponseEntity.ok(progress);
    }
}
