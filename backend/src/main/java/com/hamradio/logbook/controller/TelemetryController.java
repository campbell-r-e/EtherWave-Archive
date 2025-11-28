package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.TelemetryRequest;
import com.hamradio.logbook.entity.RigTelemetry;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.RigTelemetryRepository;
import com.hamradio.logbook.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * REST controller for receiving rig telemetry from rig control services
 */
@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class TelemetryController {

    private final RigTelemetryRepository telemetryRepository;
    private final StationRepository stationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Receive telemetry data from rig control service
     * POST /api/telemetry
     */
    @PostMapping
    public ResponseEntity<Void> receiveTelemetry(@RequestBody TelemetryRequest request) {
        try {
            // Find station
            Station station = stationRepository.findById(request.getStationId())
                    .orElseThrow(() -> new IllegalArgumentException("Station not found"));

            // Create telemetry record
            RigTelemetry telemetry = RigTelemetry.builder()
                    .station(station)
                    .frequencyKhz(request.getFrequencyKhz())
                    .mode(request.getMode())
                    .pttActive(request.getPttActive())
                    .sMeter(request.getSMeter())
                    .swr(request.getSwr() != null ? BigDecimal.valueOf(request.getSwr()) : null)
                    .timestamp(LocalDateTime.now())
                    .build();

            telemetryRepository.save(telemetry);

            // Broadcast telemetry via WebSocket
            messagingTemplate.convertAndSend("/topic/telemetry/" + request.getStationId(), request);

            log.debug("Received telemetry from station {}: freq={} kHz, mode={}",
                    station.getStationName(), request.getFrequencyKhz(), request.getMode());

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing telemetry: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get latest telemetry for a station
     * GET /api/telemetry/station/{stationId}/latest
     */
    @GetMapping("/station/{stationId}/latest")
    public ResponseEntity<RigTelemetry> getLatestTelemetry(@PathVariable Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));

        return telemetryRepository.findFirstByStationOrderByTimestampDesc(station)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
