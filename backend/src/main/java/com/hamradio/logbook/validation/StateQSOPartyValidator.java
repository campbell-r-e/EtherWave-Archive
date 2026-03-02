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
 * Validator for State QSO Parties
 * Exchange: Serial Number + State/Province (out-of-state), or Serial + County (in-state)
 * Scoring: 1 point per QSO; multipliers vary by party
 */
@Component
@Slf4j
public class StateQSOPartyValidator implements ContestValidator {

    private static final List<String> VALID_STATES = Arrays.asList(
            // US states and territories
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
            "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
            "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
            "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
            "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
            "DC", "PR", "VI",
            // Canadian provinces
            "AB", "BC", "MB", "NB", "NL", "NS", "NT", "NU", "ON", "PE", "QC", "SK", "YT",
            // DX (outside North America)
            "DX"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidationResult validate(QSO qso, Contest contest) {
        ValidationResult result = ValidationResult.success();

        if (qso.getContestData() == null || qso.getContestData().isEmpty()) {
            result.addError("Contest data is required for State QSO Party (serial, state)");
            return result;
        }

        try {
            JsonNode contestData = objectMapper.readTree(qso.getContestData());
            validateSerial(contestData, result);
            validateState(contestData, result);
        } catch (Exception e) {
            log.error("Error parsing State QSO Party contest data", e);
            result.addError("Invalid contest data format");
        }

        return result;
    }

    private void validateSerial(JsonNode contestData, ValidationResult result) {
        JsonNode serialNode = contestData.get("serial");
        if (serialNode == null || serialNode.isNull()) {
            result.addError("Serial number is required for State QSO Party");
        } else if (serialNode.asInt() < 1) {
            result.addError("Serial number must be a positive integer, got: " + serialNode.asInt());
        }
    }

    private void validateState(JsonNode contestData, ValidationResult result) {
        JsonNode stateNode = contestData.get("state");
        if (stateNode == null || stateNode.isNull() || stateNode.asText().isEmpty()) {
            result.addError("State/province is required for State QSO Party (field: state)");
        } else {
            String state = stateNode.asText().toUpperCase();
            if (!VALID_STATES.contains(state)) {
                result.addWarning("Unknown state/province: '" + state + "'");
            }
        }
    }

    @Override
    public String getContestCode() {
        return "STATE-QSO-PARTY";
    }

    @Override
    public String getDescription() {
        return "State QSO Party - State-level amateur radio contests sponsored by regional clubs";
    }
}
