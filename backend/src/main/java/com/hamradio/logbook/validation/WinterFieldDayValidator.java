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
 * Validator for Winter Field Day contest
 * Similar to ARRL Field Day but with indoor/outdoor distinction
 */
@Component
@Slf4j
public class WinterFieldDayValidator implements ContestValidator {

    private static final List<String> VALID_CLASSES = Arrays.asList(
            "1O", "2O", "3O", "4O", "5O", "6O", "7O", "8O", "9O",
            "1I", "2I", "3I", "4I", "5I", "6I", "7I", "8I", "9I",
            "1H", "2H", "3H", "4H", "5H"  // Home stations
    );

    private static final List<String> VALID_SECTIONS = Arrays.asList(
            "CT", "EMA", "ME", "NH", "RI", "VT", "WMA",
            "ENY", "NLI", "NNJ", "NNY", "SNJ", "WNY",
            "DE", "EPA", "MDC", "WPA",
            "AL", "GA", "KY", "NC", "NFL", "SC", "SFL", "WCF", "TN", "VA", "PR", "VI",
            "AR", "LA", "MS", "NM", "NTX", "OK", "STX", "WTX",
            "IA", "KS", "MO", "NE",
            "IL", "IN", "WI",
            "MI", "OH", "WV",
            "CO", "ND", "SD", "MN", "MT", "WY",
            "AK", "ID", "OR", "WA",
            "AZ", "EWA", "LAX", "NV", "ORG", "SB", "SC", "SDG", "SF", "SJV", "SV", "PAC",
            "EB", "MAR"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidationResult validate(QSO qso, Contest contest) {
        ValidationResult result = ValidationResult.success();

        if (qso.getContestData() == null || qso.getContestData().isEmpty()) {
            result.addError("Contest data is required for Winter Field Day");
            return result;
        }

        try {
            JsonNode contestData = objectMapper.readTree(qso.getContestData());

            // Validate class
            String classValue = contestData.has("class") ? contestData.get("class").asText() : null;
            if (classValue == null || classValue.isEmpty()) {
                result.addError("Winter Field Day class is required");
            } else if (!VALID_CLASSES.contains(classValue.toUpperCase())) {
                result.addError("Invalid Winter Field Day class: " + classValue);
            } else {
                // Check indoor/outdoor designation
                String indoorOutdoor = classValue.substring(classValue.length() - 1);
                if (indoorOutdoor.equals("O")) {
                    result.addWarning("Outdoor operation - bonus multiplier applies");
                } else if (indoorOutdoor.equals("I")) {
                    result.addWarning("Indoor operation");
                } else if (indoorOutdoor.equals("H")) {
                    result.addWarning("Home station operation");
                }
            }

            // Validate section
            String section = contestData.has("section") ? contestData.get("section").asText() : null;
            if (section == null || section.isEmpty()) {
                result.addError("ARRL section is required");
            } else if (!VALID_SECTIONS.contains(section.toUpperCase())) {
                result.addWarning("Unknown ARRL section: " + section);
            }

            // Validate indoor/outdoor field (redundant with class suffix, but some operators log it separately)
            String indoorOutdoor = contestData.has("indoor_outdoor") ? contestData.get("indoor_outdoor").asText() : null;
            if (indoorOutdoor != null && !indoorOutdoor.isEmpty()) {
                if (!indoorOutdoor.toUpperCase().matches("INDOOR|OUTDOOR|HOME")) {
                    result.addWarning("Invalid indoor/outdoor designation: " + indoorOutdoor);
                }
            }

        } catch (Exception e) {
            log.error("Error parsing Winter Field Day contest data", e);
            result.addError("Invalid contest data format");
        }

        return result;
    }

    @Override
    public String getContestCode() {
        return "WFD";
    }

    @Override
    public String getDescription() {
        return "Winter Field Day - Cold weather emergency preparedness exercise";
    }
}
