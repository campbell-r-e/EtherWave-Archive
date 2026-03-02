package com.hamradio.logbook.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("State QSO Party Validator Tests")
class StateQSOPartyValidatorTest {

    private StateQSOPartyValidator validator;
    private ObjectMapper objectMapper;
    private Contest stateQsoPartyContest;

    @BeforeEach
    void setUp() {
        validator = new StateQSOPartyValidator();
        objectMapper = new ObjectMapper();
        stateQsoPartyContest = TestDataBuilder.stateQsoPartyContest().build();
    }

    // ==================== VALID FULL EXCHANGE ====================

    @Test
    @DisplayName("validate - Complete valid exchange - Passes")
    void validate_completeValidExchange_passes() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("state", "IN");

        QSO qso = QSO.builder()
                .callsign("W9XYZ")
                .mode("SSB")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    // ==================== CONTEST DATA ====================

    @Test
    @DisplayName("validate - Missing contest data - Fails")
    void validate_missingContestData_fails() {
        QSO qso = QSO.builder().callsign("W1AW").build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Contest data is required"));
    }

    // ==================== SERIAL NUMBER ====================

    @Test
    @DisplayName("validate - Missing serial - Fails")
    void validate_missingSerial_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("state", "OH");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Serial number is required"));
    }

    @Test
    @DisplayName("validate - Serial number zero - Fails")
    void validate_serialZero_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 0);
        contestData.put("state", "OH");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Serial number must be a positive integer"));
    }

    @Test
    @DisplayName("validate - Negative serial number - Fails")
    void validate_negativeSerial_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", -1);
        contestData.put("state", "OH");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isFalse();
    }

    // ==================== STATE/PROVINCE ====================

    @ParameterizedTest(name = "State {0} should be valid")
    @ValueSource(strings = {"AL", "CA", "TX", "NY", "FL", "OH", "IN", "WA", "AK", "HI"})
    @DisplayName("validate - Valid US states - No state warning")
    void validate_validUSStates_noStateWarning(String state) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("state", state);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("SSB")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).noneMatch(w -> w.contains("Unknown state/province"));
    }

    @ParameterizedTest(name = "Canadian province {0} should be valid")
    @ValueSource(strings = {"AB", "BC", "ON", "QC", "MB", "SK"})
    @DisplayName("validate - Valid Canadian provinces - No state warning")
    void validate_validCanadianProvinces_noStateWarning(String province) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("state", province);

        QSO qso = QSO.builder()
                .callsign("VE3ABC")
                .mode("CW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).noneMatch(w -> w.contains("Unknown state/province"));
    }

    @Test
    @DisplayName("validate - DX station - Passes")
    void validate_dxStation_passes() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("state", "DX");

        QSO qso = QSO.builder()
                .callsign("DL1ABC")
                .mode("SSB")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("validate - Missing state - Fails")
    void validate_missingState_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("State/province is required"));
    }

    @Test
    @DisplayName("validate - Unknown state - Warns but valid")
    void validate_unknownState_warnsButValid() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("state", "ZZ");  // Non-existent

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, stateQsoPartyContest);

        assertThat(result.isValid()).isTrue();  // Unknown state is a warning, not an error
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Unknown state/province"));
    }

    // ==================== METADATA ====================

    @Test
    @DisplayName("getContestCode - Returns STATE-QSO-PARTY")
    void getContestCode_returnsStateQsoParty() {
        assertThat(validator.getContestCode()).isEqualTo("STATE-QSO-PARTY");
    }

    @Test
    @DisplayName("getDescription - Returns description")
    void getDescription_returnsDescription() {
        assertThat(validator.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("supports - Matching contest code returns true")
    void supports_matchingContestCode_returnsTrue() {
        Contest contest = TestDataBuilder.stateQsoPartyContest().build();
        assertThat(validator.supports(contest)).isTrue();
    }
}
