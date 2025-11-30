package com.hamradio.logbook.validation;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for Summits On The Air (SOTA) QSOs
 * Validates summit references and SOTA-specific data
 */
@Component
public class SOTAValidator implements ContestValidator {

    private static final Pattern SOTA_REF_PATTERN = Pattern.compile("^[A-Z0-9]{1,3}/[A-Z]{2}-\\d{3}$");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidationResult validate(QSO qso, Contest contest) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Parse contest data JSON
        String contestData = qso.getContestData();
        if (contestData == null || contestData.trim().isEmpty()) {
            errors.add("SOTA QSO requires contestData with summit_ref");
            return new ValidationResult(false, errors, warnings);
        }

        try {
            JsonNode data = objectMapper.readTree(contestData);

            // Validate summit reference
            JsonNode summitRef = data.get("summit_ref");
            if (summitRef == null || summitRef.asText().isEmpty()) {
                errors.add("SOTA summit_ref is required");
            } else {
                String ref = summitRef.asText();
                if (!SOTA_REF_PATTERN.matcher(ref).matches()) {
                    errors.add("Invalid SOTA summit reference format. Expected format: G/LD-001");
                }
            }

            // Validate points (optional but if present should be valid)
            JsonNode points = data.get("points");
            if (points != null && !points.isNull()) {
                int pointValue = points.asInt();
                if (pointValue < 1 || pointValue > 10) {
                    warnings.add("SOTA summit points typically range from 1-10");
                }
            }

            // Check for activator vs chaser
            JsonNode myRef = data.get("my_summit_ref");
            if (myRef != null && !myRef.isNull() && !myRef.asText().isEmpty()) {
                // This is an activator contact
                if (!SOTA_REF_PATTERN.matcher(myRef.asText()).matches()) {
                    errors.add("Invalid my_summit_ref format");
                }
            }

        } catch (Exception e) {
            errors.add("Invalid JSON in contestData: " + e.getMessage());
        }

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings);
    }

    @Override
    public String getContestCode() {
        return "SOTA";
    }

    @Override
    public String getDescription() {
        return "Summits On The Air (SOTA) - Validator for summit activations and chaser contacts";
    }
}
