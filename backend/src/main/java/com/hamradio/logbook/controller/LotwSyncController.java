package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.LotwSyncService;
import com.hamradio.logbook.service.LotwSyncService.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST API controller for LoTW (Logbook of the World) confirmation sync
 *
 * POST /api/lotw/sync/{logId}
 * Credentials are accepted per-request and never stored.
 */
@RestController
@RequestMapping("/api/lotw")
@RequiredArgsConstructor
@Slf4j
public class LotwSyncController {

    private final LotwSyncService lotwSyncService;

    public record SyncRequest(
            String lotwCallsign,
            String lotwPassword,
            String since          // ISO date "YYYY-MM-DD" or null for all-time
    ) {}

    /**
     * POST /api/lotw/sync/{logId}
     *
     * Downloads LoTW confirmations and updates lotwRcvd on matching QSOs.
     * Credentials are used only for this request.
     */
    @PostMapping("/sync/{logId}")
    public ResponseEntity<SyncResult> sync(
            @PathVariable Long logId,
            @RequestBody SyncRequest request,
            Authentication auth) {

        if (request.lotwCallsign() == null || request.lotwCallsign().isBlank() ||
            request.lotwPassword() == null || request.lotwPassword().isBlank()) {
            return ResponseEntity.badRequest().body(
                    new SyncResult(0, 0, 0, "LoTW callsign and password are required"));
        }

        LocalDate since = null;
        if (request.since() != null && !request.since().isBlank()) {
            try {
                since = LocalDate.parse(request.since());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                        new SyncResult(0, 0, 0, "Invalid date format — use YYYY-MM-DD"));
            }
        }

        log.info("LoTW sync requested for log {} by {} (callsign: {})",
                logId, auth.getName(), request.lotwCallsign());

        SyncResult result = lotwSyncService.sync(
                logId, auth.getName(), request.lotwCallsign(), request.lotwPassword(), since);

        return ResponseEntity.ok(result);
    }
}
