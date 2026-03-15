package com.hamradio.rigcontrol.controller;

import com.hamradio.rigcontrol.audit.CommandAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint for inspecting the rig command audit log.
 *
 * GET /api/rig/audit?limit=50
 *   Returns the most recent audit entries.
 *   limit=0 (default) returns all buffered entries.
 */
@RestController
@RequestMapping("/api/rig/audit")
@RequiredArgsConstructor
public class RigAuditController {

    private final CommandAuditLog auditLog;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLog(
            @RequestParam(defaultValue = "0") int limit) {
        List<CommandAuditLog.AuditEntry> entries = auditLog.getRecent(limit);
        return ResponseEntity.ok(Map.of(
                "count", entries.size(),
                "total", auditLog.size(),
                "entries", entries
        ));
    }
}
