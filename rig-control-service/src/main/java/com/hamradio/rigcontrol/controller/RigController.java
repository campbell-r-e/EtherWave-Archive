package com.hamradio.rigcontrol.controller;

import com.hamradio.rigcontrol.dto.RigStatus;
import com.hamradio.rigcontrol.service.HamlibService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for rig control operations
 */
@RestController
@RequestMapping("/api/rig")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class RigController {

    private final HamlibService hamlibService;

    /**
     * Get current rig status
     * GET /api/rig/status
     */
    @GetMapping("/status")
    public ResponseEntity<RigStatus> getStatus() {
        RigStatus status = hamlibService.getRigStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Set rig frequency
     * POST /api/rig/frequency?hz=14250000
     */
    @PostMapping("/frequency")
    public ResponseEntity<String> setFrequency(@RequestParam long hz) {
        boolean success = hamlibService.setFrequency(hz);
        if (success) {
            log.info("Frequency set to {} Hz", hz);
            return ResponseEntity.ok("Frequency set successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to set frequency");
        }
    }

    /**
     * Set rig mode
     * POST /api/rig/mode?mode=USB&bandwidth=3000
     */
    @PostMapping("/mode")
    public ResponseEntity<String> setMode(
            @RequestParam String mode,
            @RequestParam(defaultValue = "0") int bandwidth) {
        boolean success = hamlibService.setMode(mode, bandwidth);
        if (success) {
            log.info("Mode set to {} (bandwidth: {})", mode, bandwidth);
            return ResponseEntity.ok("Mode set successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to set mode");
        }
    }

    /**
     * Test connection to rigctld
     * GET /api/rig/test
     */
    @GetMapping("/test")
    public ResponseEntity<String> testConnection() {
        boolean connected = hamlibService.testConnection();
        if (connected) {
            return ResponseEntity.ok("Connected to rigctld");
        } else {
            return ResponseEntity.status(503).body("Cannot connect to rigctld");
        }
    }
}
