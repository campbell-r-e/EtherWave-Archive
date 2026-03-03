package com.hamradio.logbook.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator for the CQ World Wide DX Contest (CQWW)
 * Exchange: RST + CQ Zone
 * Scoring: 0 pts same country, 1 pt same continent, 3 pts different continent
 * Multipliers: DXCC entities × CQ Zones per band
 */
@Component
@Slf4j
public class CQWWValidator implements ContestValidator {

    private static final int MIN_CQ_ZONE = 1;
    private static final int MAX_CQ_ZONE = 40;

    // Valid HF bands for CQ WW (kHz): 160/80/40/20/15/10m
    private static final long[][] VALID_BAND_RANGES = {
        {1800, 2000},
        {3500, 4000},
        {7000, 7300},
        {14000, 14350},
        {21000, 21450},
        {28000, 29700}
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidationResult validate(QSO qso, Contest contest) {
        ValidationResult result = ValidationResult.success();

        if (qso.getContestData() == null || qso.getContestData().isEmpty()) {
            result.addError("Contest data is required for CQ WW DX (must include cq_zone)");
            return result;
        }

        try {
            JsonNode contestData = objectMapper.readTree(qso.getContestData());
            validateCqZone(contestData, result);
            validateMode(qso, result);
            validateFrequency(qso, result);
        } catch (Exception e) {
            log.error("Error parsing CQ WW contest data", e);
            result.addError("Invalid contest data format");
        }

        return result;
    }

    private void validateCqZone(JsonNode contestData, ValidationResult result) {
        JsonNode cqZoneNode = contestData.get("cq_zone");
        if (cqZoneNode == null || cqZoneNode.isNull()) {
            result.addError("CQ Zone is required for CQ WW DX (field: cq_zone)");
            return;
        }
        int cqZone = cqZoneNode.asInt();
        if (cqZone < MIN_CQ_ZONE || cqZone > MAX_CQ_ZONE) {
            result.addError("Invalid CQ Zone: " + cqZone + " (must be 1-40)");
        }
    }

    private void validateMode(QSO qso, ValidationResult result) {
        String mode = qso.getMode();
        if (mode == null) {
            result.addWarning("Mode not specified");
            return;
        }
        mode = mode.toUpperCase();
        if (!mode.equals("CW") && !mode.equals("SSB") && !mode.equals("AM") && !mode.equals("FM")) {
            result.addWarning("Mode '" + mode + "' is non-standard for CQ WW (expected CW or SSB)");
        }
    }

    private void validateFrequency(QSO qso, ValidationResult result) {
        if (qso.getFrequencyKhz() == null) {
            result.addWarning("Frequency not specified");
            return;
        }
        long freqKhz = qso.getFrequencyKhz();
        for (long[] range : VALID_BAND_RANGES) {
            if (freqKhz >= range[0] && freqKhz <= range[1]) {
                return;
            }
        }
        result.addWarning("Frequency " + freqKhz + " kHz is outside CQ WW contest bands (160/80/40/20/15/10m)");
    }

    @Override
    public String getContestCode() {
        return "CQWW";
    }

    @Override
    public String getDescription() {
        return "CQ World Wide DX Contest - Largest amateur radio contest worldwide";
    }
}
