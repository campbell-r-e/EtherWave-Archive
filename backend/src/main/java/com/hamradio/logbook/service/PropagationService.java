package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fetches current solar/geomagnetic indices from NOAA Space Weather Prediction Center
 * and derives HF band condition ratings (poor/fair/good/excellent).
 *
 * Data source: https://services.swpc.noaa.gov/json/solar-geophysical-data.json
 * Updated every 3 hours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropagationService {

    private static final String NOAA_URL =
            "https://services.swpc.noaa.gov/json/solar-geophysical-data.json";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    // Simple cache — avoid hammering NOAA on every page load
    private PropagationConditions cachedConditions;
    private Instant cacheExpiry = Instant.EPOCH;
    private static final long CACHE_TTL_SECONDS = 1800; // 30 minutes

    public enum BandCondition { EXCELLENT, GOOD, FAIR, POOR }

    public record BandStatus(String band, String displayName, BandCondition condition, String description) {}

    public record PropagationConditions(
            int sfi,          // Solar Flux Index (70-300+)
            int kIndex,       // K-index (0-9, geomagnetic)
            int aIndex,       // A-index (0-400)
            String fetchedAt,
            Map<String, BandStatus> bands   // band key → status
    ) {}

    public PropagationConditions getConditions() {
        if (cachedConditions != null && Instant.now().isBefore(cacheExpiry)) {
            return cachedConditions;
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(NOAA_URL, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("NOAA returned status {}", response.getStatusCode());
                return fallback();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            // NOAA returns an array; latest data is the last element
            JsonNode latest = null;
            if (root.isArray() && root.size() > 0) {
                latest = root.get(root.size() - 1);
            } else if (root.isObject()) {
                latest = root;
            }

            if (latest == null) return fallback();

            int sfi    = getInt(latest, "solarflux",     75);
            int kIndex = getInt(latest, "k_index",        3);
            int aIndex = getInt(latest, "a_index",       15);

            PropagationConditions conditions = buildConditions(sfi, kIndex, aIndex);
            cachedConditions = conditions;
            cacheExpiry = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
            return conditions;

        } catch (Exception e) {
            log.error("Failed to fetch propagation data: {}", e.getMessage());
            return fallback();
        }
    }

    private PropagationConditions buildConditions(int sfi, int kIndex, int aIndex) {
        Map<String, BandStatus> bands = new LinkedHashMap<>();

        // Band condition algorithm:
        // - High SFI favours higher bands (10m, 12m, 15m)
        // - Low SFI favours lower bands (40m, 80m, 160m)
        // - High K/A index degrades all bands (geomagnetic storm effect)

        boolean storm = kIndex >= 5 || aIndex >= 30;
        boolean mildDisturbance = kIndex >= 3 || aIndex >= 15;

        // 160m — best at night, poor when disturbed
        bands.put("160m", new BandStatus("160m", "160m", stormDegrade(BandCondition.POOR, storm, mildDisturbance),
                "LF – requires darkness, affected by aurora"));

        // 80m
        bands.put("80m", new BandStatus("80m", "80m",
                mildDisturbance ? BandCondition.POOR : BandCondition.FAIR,
                "Regional nighttime; geomagnetic sensitive"));

        // 40m — generally reliable
        bands.put("40m", new BandStatus("40m", "40m",
                storm ? BandCondition.POOR : mildDisturbance ? BandCondition.FAIR : BandCondition.GOOD,
                "Reliable day/night"));

        // 20m — workhorse band
        BandCondition c20 = sfi >= 150 ? BandCondition.EXCELLENT
                : sfi >= 100 ? BandCondition.GOOD
                : sfi >= 80  ? BandCondition.FAIR
                :              BandCondition.POOR;
        bands.put("20m", new BandStatus("20m", "20m", stormDegrade(c20, storm, mildDisturbance),
                "Primary DX band; SFI dependent"));

        // 17m
        BandCondition c17 = sfi >= 130 ? BandCondition.EXCELLENT
                : sfi >= 100 ? BandCondition.GOOD
                : sfi >= 85  ? BandCondition.FAIR
                :              BandCondition.POOR;
        bands.put("17m", new BandStatus("17m", "17m", stormDegrade(c17, storm, mildDisturbance),
                "Good DX when solar flux is moderate+"));

        // 15m
        BandCondition c15 = sfi >= 150 ? BandCondition.EXCELLENT
                : sfi >= 120 ? BandCondition.GOOD
                : sfi >= 90  ? BandCondition.FAIR
                :              BandCondition.POOR;
        bands.put("15m", new BandStatus("15m", "15m", stormDegrade(c15, storm, mildDisturbance),
                "High solar flux required"));

        // 12m
        BandCondition c12 = sfi >= 180 ? BandCondition.EXCELLENT
                : sfi >= 140 ? BandCondition.GOOD
                : sfi >= 100 ? BandCondition.FAIR
                :              BandCondition.POOR;
        bands.put("12m", new BandStatus("12m", "12m", stormDegrade(c12, storm, mildDisturbance),
                "Requires high solar flux"));

        // 10m
        BandCondition c10 = sfi >= 200 ? BandCondition.EXCELLENT
                : sfi >= 160 ? BandCondition.GOOD
                : sfi >= 110 ? BandCondition.FAIR
                :              BandCondition.POOR;
        bands.put("10m", new BandStatus("10m", "10m", stormDegrade(c10, storm, mildDisturbance),
                "Depends on solar maximum; Es possible"));

        // 6m — mainly Sporadic E / aurora, simplified
        BandCondition c6 = kIndex >= 4 ? BandCondition.GOOD : BandCondition.POOR;
        bands.put("6m", new BandStatus("6m", "6m", c6,
                kIndex >= 4 ? "Aurora/Es possible" : "Local / Sporadic E only"));

        return new PropagationConditions(sfi, kIndex, aIndex, Instant.now().toString(), bands);
    }

    /** Degrade a condition by 1 step on storm / mild disturbance */
    private BandCondition stormDegrade(BandCondition base, boolean storm, boolean mild) {
        if (storm) {
            return switch (base) {
                case EXCELLENT -> BandCondition.FAIR;
                case GOOD      -> BandCondition.POOR;
                default        -> BandCondition.POOR;
            };
        }
        if (mild) {
            return switch (base) {
                case EXCELLENT -> BandCondition.GOOD;
                case GOOD      -> BandCondition.FAIR;
                default        -> BandCondition.POOR;
            };
        }
        return base;
    }

    private int getInt(JsonNode node, String field, int defaultVal) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return defaultVal;
        try { return n.asInt(defaultVal); } catch (Exception e) { return defaultVal; }
    }

    private PropagationConditions fallback() {
        return buildConditions(75, 3, 15);  // typical quiet-Sun values
    }
}
