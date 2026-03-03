package com.hamradio.logbook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches live DX spots from the DX Summit public API.
 * Endpoint: http://www.dxsummit.fi/api/v1/spots
 * Returns spots with callsign, frequency, band, mode, comment, timestamp.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DXClusterService {

    private static final String DX_SUMMIT_URL =
            "http://www.dxsummit.fi/api/v1/spots?limit=%d";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public record DXSpot(
            String spotter,       // spotting station callsign
            String dxCallsign,    // DX station callsign
            double frequency,     // frequency in kHz
            String band,          // band label e.g. "20m"
            String mode,          // mode or empty
            String comment,       // spotter comment
            String time           // ISO-8601 timestamp
    ) {}

    /**
     * Fetch recent DX spots, optionally filtered by band.
     *
     * @param limit  max spots to return (1–200)
     * @param band   if non-null/blank, filter to this band (e.g. "20m")
     */
    public List<DXSpot> getSpots(int limit, String band) {
        int capped = Math.max(1, Math.min(200, limit));
        String url = String.format(DX_SUMMIT_URL, capped);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("DX Summit returned status {}", response.getStatusCode());
                return Collections.emptyList();
            }

            List<Map<String, Object>> raw = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<>() {}
            );

            return raw.stream()
                    .map(this::mapToSpot)
                    .filter(spot -> band == null || band.isBlank() || band.equalsIgnoreCase(spot.band()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to fetch DX cluster spots: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private DXSpot mapToSpot(Map<String, Object> raw) {
        return new DXSpot(
                getString(raw, "callsign"),
                getString(raw, "dx_callsign"),
                getDouble(raw, "frequency"),
                getString(raw, "band"),
                getString(raw, "mode"),
                getString(raw, "info"),
                getString(raw, "time")
        );
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
}
