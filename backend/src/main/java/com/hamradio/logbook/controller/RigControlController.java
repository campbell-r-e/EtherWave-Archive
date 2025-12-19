package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.RigCommandRequest;
import com.hamradio.logbook.dto.RigCommandResponse;
import com.hamradio.logbook.dto.RigConnectionRequest;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.StationRepository;
import com.hamradio.logbook.service.RigControlClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for rig control operations.
 * Allows users to connect to and control rigs via the rig control service.
 */
@RestController
@RequestMapping("/api/rig-control")
@RequiredArgsConstructor
@Slf4j
public class RigControlController {

    private final RigControlClient rigControlClient;
    private final StationRepository stationRepository;

    /**
     * Connect a station to the rig control service
     * POST /api/rig-control/connect
     */
    @PostMapping("/connect")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> connect(@RequestBody RigConnectionRequest request) {
        try {
            Station station = stationRepository.findById(request.getStationId())
                    .orElseThrow(() -> new IllegalArgumentException("Station not found"));

            // Use station's configured rig control settings if not provided
            String host = request.getHost() != null ? request.getHost() : station.getRigControlHost();
            Integer port = request.getPort() != null ? request.getPort() : station.getRigControlPort();

            if (host == null || port == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Rig control host/port not configured"));
            }

            CompletableFuture<Boolean> connectionFuture = rigControlClient.connect(
                    station.getId(),
                    station.getStationName(),
                    host,
                    port
            );

            Boolean connected = connectionFuture.get();

            if (connected) {
                log.info("Station {} connected to rig control service at {}:{}",
                        station.getStationName(), host, port);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Connected to rig control service",
                        "stationId", station.getId(),
                        "stationName", station.getStationName()
                ));
            } else {
                return ResponseEntity.status(500)
                        .body(Map.of("success", false, "message", "Failed to connect to rig control service"));
            }

        } catch (Exception e) {
            log.error("Error connecting to rig control service: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Disconnect a station from the rig control service
     * POST /api/rig-control/disconnect/{stationId}
     */
    @PostMapping("/disconnect/{stationId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable Long stationId) {
        try {
            rigControlClient.disconnect(stationId);
            log.info("Station {} disconnected from rig control service", stationId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Disconnected from rig control service"));
        } catch (Exception e) {
            log.error("Error disconnecting from rig control service: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Send a command to the rig
     * POST /api/rig-control/command/{stationId}
     */
    @PostMapping("/command/{stationId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<RigCommandResponse> sendCommand(
            @PathVariable Long stationId,
            @RequestBody RigCommandRequest request) {
        try {
            if (!rigControlClient.isConnected(stationId)) {
                return ResponseEntity.badRequest().body(
                        RigCommandResponse.builder()
                                .success(false)
                                .message("Station not connected to rig control service")
                                .build()
                );
            }

            CompletableFuture<Map<String, Object>> responseFuture = rigControlClient.sendCommand(
                    stationId,
                    request.getCommand(),
                    request.getParams() != null ? request.getParams() : new HashMap<>()
            );

            Map<String, Object> response = responseFuture.get();

            return ResponseEntity.ok(
                    RigCommandResponse.builder()
                            .success((Boolean) response.get("success"))
                            .message((String) response.get("message"))
                            .result((Map<String, Object>) response.get("result"))
                            .build()
            );

        } catch (Exception e) {
            log.error("Error sending command to rig: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                    RigCommandResponse.builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Set frequency
     * POST /api/rig-control/frequency/{stationId}
     */
    @PostMapping("/frequency/{stationId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<RigCommandResponse> setFrequency(
            @PathVariable Long stationId,
            @RequestParam Long frequencyHz) {
        return sendCommand(stationId, RigCommandRequest.builder()
                .command("setFrequency")
                .params(Map.of("hz", frequencyHz))
                .build());
    }

    /**
     * Set mode
     * POST /api/rig-control/mode/{stationId}
     */
    @PostMapping("/mode/{stationId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<RigCommandResponse> setMode(
            @PathVariable Long stationId,
            @RequestParam String mode,
            @RequestParam(defaultValue = "0") Integer bandwidth) {
        return sendCommand(stationId, RigCommandRequest.builder()
                .command("setMode")
                .params(Map.of("mode", mode, "bandwidth", bandwidth))
                .build());
    }

    /**
     * Set PTT (Push-to-Talk)
     * POST /api/rig-control/ptt/{stationId}
     */
    @PostMapping("/ptt/{stationId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<RigCommandResponse> setPTT(
            @PathVariable Long stationId,
            @RequestParam Boolean enable) {
        return sendCommand(stationId, RigCommandRequest.builder()
                .command("setPTT")
                .params(Map.of("enable", enable))
                .build());
    }

    /**
     * Get current rig status
     * GET /api/rig-control/status/{stationId}
     */
    @GetMapping("/status/{stationId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<RigCommandResponse> getStatus(@PathVariable Long stationId) {
        return sendCommand(stationId, RigCommandRequest.builder()
                .command("getStatus")
                .params(new HashMap<>())
                .build());
    }

    /**
     * Check if a station is connected to the rig control service
     * GET /api/rig-control/connected/{stationId}
     */
    @GetMapping("/connected/{stationId}")
    @PreAuthorize("hasAnyRole('ROLE_USER', 'ROLE_OPERATOR', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> isConnected(@PathVariable Long stationId) {
        boolean connected = rigControlClient.isConnected(stationId);
        return ResponseEntity.ok(Map.of("connected", connected, "stationId", stationId));
    }
}
