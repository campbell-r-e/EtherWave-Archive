package com.hamradio.logbook.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Validator for ARRL Field Day contest
 * Validates class, section, and other Field Day specific rules
 */
@Component
@Slf4j
public class FieldDayValidator implements ContestValidator {

    private static final List<String> VALID_CLASSES = Arrays.asList(
            "1A", "1B", "1C", "1D", "1E", "1F",
            "2A", "2B", "2C", "2D", "2E", "2F",
            "3A", "3B", "3C", "3D", "3E", "3F",
            "4A", "4B", "4C", "4D", "4E", "4F",
            "5A", "5B", "5C", "5D", "5E", "5F",
            "6A", "6B", "6C", "6D", "6E", "6F",
            "7A", "7B", "7C", "7D", "7E", "7F",
            "8A", "8B", "8C", "8D", "8E", "8F",
            "9A", "9B", "9C", "9D", "9E", "9F",
            "10A", "10B", "10C", "10D", "10E", "10F",
            "11A", "11B", "11C", "11D", "11E", "11F",
            "12A", "12B", "12C", "12D", "12E", "12F",
            "13A", "13B", "13C", "13D", "13E", "13F",
            "14A", "14B", "14C", "14D", "14E", "14F",
            "15A", "15B", "15C", "15D", "15E", "15F",
            "16A", "16B", "16C", "16D", "16E", "16F",
            "17A", "17B", "17C", "17D", "17E", "17F",
            "18A", "18B", "18C", "18D", "18E", "18F",
            "19A", "19B", "19C", "19D", "19E", "19F",
            "20A", "20B", "20C", "20D", "20E", "20F",
            "21A", "21B", "21C", "21D", "21E", "21F",
            "22A", "22B", "22C", "22D", "22E", "22F",
            "23A", "23B", "23C", "23D", "23E", "23F",
            "24A", "24B", "24C", "24D", "24E", "24F",
            "25A", "26A", "27A", "28A", "29A", "30A",
            "31A", "32A", "33A", "34A", "35A", "36A",
            "37A", "38A", "39A", "40A"
    );

    private static final List<String> VALID_SECTIONS = Arrays.asList(
            "CT", "EMA", "ME", "NH", "RI", "VT", "WMA",  // New England
            "ENY", "NLI", "NNJ", "NNY", "SNJ", "WNY",     // Atlantic
            "DE", "EPA", "MDC", "WPA",                     // Delta
            "AL", "GA", "KY", "NC", "NFL", "SC", "SFL", "WCF", "TN", "VA", "PR", "VI", // Southeastern
            "AR", "LA", "MS", "NM", "NTX", "OK", "STX", "WTX",  // West Gulf
            "IA", "KS", "MO", "NE",                        // Midwest
            "IL", "IN", "WI",                              // Great Lakes
            "MI", "OH", "WV",                              // Central
            "CO", "ND", "SD", "MN", "MT", "WY",           // Dakota
            "AK", "ID", "OR", "WA",                        // Northwestern
            "AZ", "EWA", "LAX", "NV", "ORG", "SB", "SC", "SCV", "SDG", "SF", "SJV", "SV", "PAC", // Pacific
            "EB", "PAC",                                   // Roanoke
            "MAR"                                          // Maritime (Canada)
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidationResult validate(QSO qso, Contest contest) {
        ValidationResult result = ValidationResult.success();

        if (qso.getContestData() == null || qso.getContestData().isEmpty()) {
            result.addError("Contest data is required for Field Day");
            return result;
        }

        try {
            JsonNode contestData = objectMapper.readTree(qso.getContestData());

            // Validate class
            String classValue = contestData.has("class") ? contestData.get("class").asText() : null;
            if (classValue == null || classValue.isEmpty()) {
                result.addError("Field Day class is required");
            } else if (!VALID_CLASSES.contains(classValue)) {
                result.addError("Invalid Field Day class: " + classValue);
            }

            // Validate section
            String section = contestData.has("section") ? contestData.get("section").asText() : null;
            if (section == null || section.isEmpty()) {
                result.addError("ARRL section is required");
            } else if (!VALID_SECTIONS.contains(section)) {
                result.addError("Invalid ARRL section: " + section);
            }

            // Validate mode scoring
            validateModeScoring(qso, result);

            // Validate duplicate contacts
            validateDuplicates(qso, contest, result);

        } catch (Exception e) {
            log.error("Error parsing Field Day contest data", e);
            result.addError("Invalid contest data format");
        }

        return result;
    }

    /**
     * Validate mode scoring (CW/Digital = 2 points, Phone = 1 point)
     */
    private void validateModeScoring(QSO qso, ValidationResult result) {
        String mode = qso.getMode();
        if (mode == null) {
            result.addWarning("Mode is not specified");
            return;
        }

        mode = mode.toUpperCase();
        if (mode.equals("SSB") || mode.equals("FM") || mode.equals("AM")) {
            // Phone modes = 1 point
            result.addWarning("Phone mode: 1 point per contact");
        } else if (mode.equals("CW")) {
            // CW = 2 points
            result.addWarning("CW mode: 2 points per contact");
        } else {
            // Digital modes = 2 points
            result.addWarning("Digital mode: 2 points per contact");
        }
    }

    /**
     * Check for duplicate contacts (same callsign, band, mode)
     */
    private void validateDuplicates(QSO qso, Contest contest, ValidationResult result) {
        // This would check against existing QSOs in the database
        // For now, just add a warning placeholder
        // Actual duplicate checking happens in the service layer
    }

    @Override
    public String getContestCode() {
        return "ARRL-FD";
    }

    @Override
    public String getDescription() {
        return "ARRL Field Day - 24-hour emergency preparedness exercise";
    }
}
