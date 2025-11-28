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
            "6A", "7A", "8A", "9A", "10A", "11A", "12A",
            "13A", "14A", "15A", "16A", "17A", "18A",
            "19A", "20A", "21A", "22A", "23A", "24A"
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
            "AZ", "EWA", "LAX", "NV", "ORG", "SB", "SC", "SDG", "SF", "SJV", "SV", "PAC", // Pacific
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
            } else if (!VALID_CLASSES.contains(classValue.toUpperCase())) {
                result.addError("Invalid Field Day class: " + classValue);
            }

            // Validate section
            String section = contestData.has("section") ? contestData.get("section").asText() : null;
            if (section == null || section.isEmpty()) {
                result.addError("ARRL section is required");
            } else if (!VALID_SECTIONS.contains(section.toUpperCase())) {
                result.addWarning("Unknown ARRL section: " + section);
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
