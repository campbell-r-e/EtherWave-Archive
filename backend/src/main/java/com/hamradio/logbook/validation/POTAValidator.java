package com.hamradio.logbook.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validator for Parks on the Air (POTA) activations
 * Validates park references and activation rules
 */
@Component
@Slf4j
public class POTAValidator implements ContestValidator {

    // POTA reference format: K-1234, VE-1234, etc.
    private static final Pattern PARK_REF_PATTERN = Pattern.compile("^[A-Z]{1,3}-\\d{4,5}$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidationResult validate(QSO qso, Contest contest) {
        ValidationResult result = ValidationResult.success();

        if (qso.getContestData() == null || qso.getContestData().isEmpty()) {
            result.addError("Contest data is required for POTA");
            return result;
        }

        try {
            JsonNode contestData = objectMapper.readTree(qso.getContestData());

            // Validate park reference (required for activator)
            String parkRef = contestData.has("park_ref") ? contestData.get("park_ref").asText() : null;
            if (parkRef != null && !parkRef.isEmpty()) {
                if (!PARK_REF_PATTERN.matcher(parkRef).matches()) {
                    result.addError("Invalid POTA park reference format: " + parkRef + " (expected format: K-1234)");
                } else {
                    // Valid park reference - this is an activation
                    result.addWarning("POTA Activation at park: " + parkRef);
                }
            }

            // Validate hunter reference (if hunter contacted another park)
            String hunterRef = contestData.has("hunter_ref") ? contestData.get("hunter_ref").asText() : null;
            if (hunterRef != null && !hunterRef.isEmpty()) {
                if (!PARK_REF_PATTERN.matcher(hunterRef).matches()) {
                    result.addError("Invalid POTA hunter reference format: " + hunterRef);
                } else {
                    // Park-to-park contact
                    boolean isParkToPark = contestData.has("park_to_park") && contestData.get("park_to_park").asBoolean();
                    if (isParkToPark || (parkRef != null && !parkRef.isEmpty())) {
                        result.addWarning("Park-to-Park contact! Bonus points!");
                    }
                }
            }

            // Must have at least one park reference
            if ((parkRef == null || parkRef.isEmpty()) && (hunterRef == null || hunterRef.isEmpty())) {
                result.addError("POTA contact must include at least one park reference (park_ref or hunter_ref)");
            }

            // Validate frequency (some parks have restrictions)
            validateFrequency(qso, result);

        } catch (Exception e) {
            log.error("Error parsing POTA contest data", e);
            result.addError("Invalid contest data format");
        }

        return result;
    }

    /**
     * Validate frequency is within amateur bands
     */
    private void validateFrequency(QSO qso, ValidationResult result) {
        if (qso.getFrequencyKhz() == null) {
            result.addWarning("Frequency not specified");
            return;
        }

        long freqKhz = qso.getFrequencyKhz();

        // Basic amateur band validation
        boolean validBand = false;
        if (freqKhz >= 1800 && freqKhz <= 2000) validBand = true;      // 160m
        else if (freqKhz >= 3500 && freqKhz <= 4000) validBand = true; // 80m
        else if (freqKhz >= 7000 && freqKhz <= 7300) validBand = true; // 40m
        else if (freqKhz >= 14000 && freqKhz <= 14350) validBand = true; // 20m
        else if (freqKhz >= 21000 && freqKhz <= 21450) validBand = true; // 15m
        else if (freqKhz >= 28000 && freqKhz <= 29700) validBand = true; // 10m
        else if (freqKhz >= 50000 && freqKhz <= 54000) validBand = true; // 6m
        else if (freqKhz >= 144000 && freqKhz <= 148000) validBand = true; // 2m

        if (!validBand) {
            result.addWarning("Frequency " + freqKhz + " kHz may be outside amateur bands");
        }
    }

    @Override
    public String getContestCode() {
        return "POTA";
    }

    @Override
    public String getDescription() {
        return "Parks on the Air - Portable operations from parks and protected areas";
    }
}
