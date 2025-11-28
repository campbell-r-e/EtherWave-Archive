package com.hamradio.rigcontrol.service;

import com.hamradio.rigcontrol.dto.RigStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service for collecting and sending rig telemetry to the main backend
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryService {

    private final HamlibService hamlibService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telemetry.polling.enabled}")
    private boolean pollingEnabled;

    @Value("${backend.api.url}")
    private String backendUrl;

    @Value("${backend.api.enabled}")
    private boolean backendEnabled;

    @Value("${station.id}")
    private Long stationId;

    private RigStatus lastStatus;

    /**
     * Poll rig status periodically
     * Runs every 2 seconds by default (configurable via telemetry.polling.interval.ms)
     */
    @Scheduled(fixedDelayString = "${telemetry.polling.interval.ms:2000}")
    public void pollRigStatus() {
        if (!pollingEnabled) {
            return;
        }

        try {
            RigStatus currentStatus = hamlibService.getRigStatus();
            lastStatus = currentStatus;

            // Send to backend if enabled and status changed significantly
            if (backendEnabled && hasSignificantChange(lastStatus, currentStatus)) {
                sendTelemetryToBackend(currentStatus);
            }

        } catch (Exception e) {
            log.debug("Error polling rig status: {}", e.getMessage());
        }
    }

    /**
     * Get the last polled status
     */
    public RigStatus getLastStatus() {
        return lastStatus != null ? lastStatus : hamlibService.getRigStatus();
    }

    /**
     * Send telemetry data to main backend
     */
    private void sendTelemetryToBackend(RigStatus status) {
        try {
            String url = backendUrl + "/api/telemetry";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create telemetry payload
            TelemetryPayload payload = new TelemetryPayload();
            payload.stationId = stationId;
            payload.frequencyKhz = status.getFrequencyHz() != null ? status.getFrequencyHz() / 1000 : null;
            payload.mode = status.getMode();
            payload.pttActive = status.getPttActive();
            payload.sMeter = status.getSMeter();
            payload.swr = status.getSwr();

            HttpEntity<TelemetryPayload> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.debug("Sent telemetry to backend: freq={}, mode={}", payload.frequencyKhz, payload.mode);

        } catch (Exception e) {
            log.debug("Failed to send telemetry to backend: {}", e.getMessage());
        }
    }

    /**
     * Check if status has changed significantly (frequency or mode change)
     */
    private boolean hasSignificantChange(RigStatus last, RigStatus current) {
        if (last == null) return true;

        // Frequency changed
        if (!equals(last.getFrequencyHz(), current.getFrequencyHz())) {
            return true;
        }

        // Mode changed
        if (!equals(last.getMode(), current.getMode())) {
            return true;
        }

        // PTT state changed
        if (!equals(last.getPttActive(), current.getPttActive())) {
            return true;
        }

        return false;
    }

    private boolean equals(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    /**
     * Inner class for telemetry payload
     */
    private static class TelemetryPayload {
        public Long stationId;
        public Long frequencyKhz;
        public String mode;
        public Boolean pttActive;
        public Integer sMeter;
        public Double swr;
    }
}
