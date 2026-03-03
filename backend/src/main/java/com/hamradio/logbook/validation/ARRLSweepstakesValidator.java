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
 * Validator for the ARRL Sweepstakes contest
 * Exchange: Serial Number + Precedence + Callsign + Check + Section
 * Scoring: 2 points per QSO; multiplier = number of ARRL/RAC sections worked
 */
@Component
@Slf4j
public class ARRLSweepstakesValidator implements ContestValidator {

    // Q=QRP (<5W), A=low power (<100W), B=high power, M=multi-op, S=school, U=unlimited
    private static final List<String> VALID_PRECEDENCES = Arrays.asList("Q", "A", "B", "M", "S", "U");

    // All ARRL and RAC sections
    private static final List<String> VALID_SECTIONS = Arrays.asList(
            // New England
            "CT", "EMA", "ME", "NH", "RI", "VT", "WMA",
            // Atlantic
            "ENY", "NLI", "NNJ", "NNY", "SNJ", "WNY",
            // Roanoke
            "DE", "EPA", "MDC", "WPA",
            // Southeastern
            "AL", "GA", "NC", "NFL", "SC", "SFL", "TN", "VA", "WCF", "PR", "VI",
            // Delta
            "AR", "LA", "MS", "NM", "NTX", "OK", "STX", "WTX",
            // Great Lakes
            "IL", "IN", "WI",
            // Ohio Valley
            "KY", "OH", "WV",
            // Central
            "CO", "IA", "KS", "MN", "MO", "NE", "ND", "SD",
            // Northwestern
            "AK", "ID", "MT", "OR", "WA",
            // Pacific
            "AZ", "EWA", "LAX", "NV", "ORG", "PAC", "SB", "SCV", "SDG", "SF", "SJV", "SV", "WWA",
            // Michigan
            "MI",
            // Canada (RAC sections)
            "AB", "BC", "GTA", "MB", "MAR", "NL", "NT", "ONE", "ONN", "ONS", "PE", "QC", "SK"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidationResult validate(QSO qso, Contest contest) {
        ValidationResult result = ValidationResult.success();

        if (qso.getContestData() == null || qso.getContestData().isEmpty()) {
            result.addError("Contest data is required for ARRL Sweepstakes (serial, precedence, check, section)");
            return result;
        }

        try {
            JsonNode contestData = objectMapper.readTree(qso.getContestData());
            validateSerial(contestData, result);
            validatePrecedence(contestData, result);
            validateCheck(contestData, result);
            validateSection(contestData, result);
            validateMode(qso, result);
        } catch (Exception e) {
            log.error("Error parsing ARRL Sweepstakes contest data", e);
            result.addError("Invalid contest data format");
        }

        return result;
    }

    private void validateSerial(JsonNode contestData, ValidationResult result) {
        JsonNode serialNode = contestData.get("serial");
        if (serialNode == null || serialNode.isNull()) {
            result.addError("Serial number is required for ARRL Sweepstakes");
        } else if (serialNode.asInt() < 1) {
            result.addError("Serial number must be a positive integer, got: " + serialNode.asInt());
        }
    }

    private void validatePrecedence(JsonNode contestData, ValidationResult result) {
        JsonNode precedenceNode = contestData.get("precedence");
        if (precedenceNode == null || precedenceNode.isNull() || precedenceNode.asText().isEmpty()) {
            result.addError("Precedence is required for ARRL Sweepstakes (Q, A, B, M, S, or U)");
        } else {
            String precedence = precedenceNode.asText().toUpperCase();
            if (!VALID_PRECEDENCES.contains(precedence)) {
                result.addError("Invalid precedence: '" + precedence + "' (must be Q, A, B, M, S, or U)");
            }
        }
    }

    private void validateCheck(JsonNode contestData, ValidationResult result) {
        JsonNode checkNode = contestData.get("check");
        if (checkNode == null || checkNode.isNull()) {
            result.addError("Check (two-digit year first licensed) is required for ARRL Sweepstakes");
        } else {
            String check = checkNode.asText().trim();
            if (!check.matches("\\d{2}")) {
                result.addError("Check must be exactly two digits (e.g. 97 for 1997), got: '" + check + "'");
            }
        }
    }

    private void validateSection(JsonNode contestData, ValidationResult result) {
        JsonNode sectionNode = contestData.get("section");
        if (sectionNode == null || sectionNode.isNull() || sectionNode.asText().isEmpty()) {
            result.addError("ARRL/RAC section is required for ARRL Sweepstakes");
        } else {
            String section = sectionNode.asText().toUpperCase();
            if (!VALID_SECTIONS.contains(section)) {
                result.addWarning("Unknown ARRL/RAC section: '" + section + "'");
            }
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
            result.addWarning("Mode '" + mode + "' is non-standard for ARRL Sweepstakes (expected CW or SSB)");
        }
    }

    @Override
    public String getContestCode() {
        return "ARRL-SS";
    }

    @Override
    public String getDescription() {
        return "ARRL Sweepstakes - Annual fall sprint through all ARRL and RAC sections";
    }
}
