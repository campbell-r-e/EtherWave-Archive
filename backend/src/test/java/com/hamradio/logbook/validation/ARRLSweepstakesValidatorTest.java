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

@DisplayName("ARRL Sweepstakes Validator Tests")
class ARRLSweepstakesValidatorTest {

    private ARRLSweepstakesValidator validator;
    private ObjectMapper objectMapper;
    private Contest sweepstakesContest;

    @BeforeEach
    void setUp() {
        validator = new ARRLSweepstakesValidator();
        objectMapper = new ObjectMapper();
        sweepstakesContest = TestDataBuilder.sweepstakesContest().build();
    }

    // ==================== VALID FULL EXCHANGE ====================

    @Test
    @DisplayName("validate - Complete valid exchange - Passes")
    void validate_completeValidExchange_passes() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 42);
        contestData.put("precedence", "A");
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W6ABC")
                .mode("SSB")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    // ==================== CONTEST DATA ====================

    @Test
    @DisplayName("validate - Missing contest data - Fails")
    void validate_missingContestData_fails() {
        QSO qso = QSO.builder().callsign("W1AW").build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Contest data is required"));
    }

    // ==================== SERIAL NUMBER ====================

    @Test
    @DisplayName("validate - Missing serial - Fails")
    void validate_missingSerial_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("precedence", "A");
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Serial number is required"));
    }

    @Test
    @DisplayName("validate - Serial number zero - Fails")
    void validate_serialZero_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 0);
        contestData.put("precedence", "A");
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Serial number must be a positive integer"));
    }

    @Test
    @DisplayName("validate - Negative serial number - Fails")
    void validate_negativeSerial_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", -5);
        contestData.put("precedence", "A");
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
    }

    // ==================== PRECEDENCE ====================

    @ParameterizedTest(name = "Precedence {0} should be valid")
    @ValueSource(strings = {"Q", "A", "B", "M", "S", "U"})
    @DisplayName("validate - Valid precedences - Passes")
    void validate_validPrecedences_passes(String precedence) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", precedence);
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("CW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @ParameterizedTest(name = "Precedence {0} should be invalid")
    @ValueSource(strings = {"C", "D", "X", "Z", "1", ""})
    @DisplayName("validate - Invalid precedences - Fails")
    void validate_invalidPrecedences_fails(String precedence) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", precedence);
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("validate - Missing precedence - Fails")
    void validate_missingPrecedence_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Precedence is required"));
    }

    // ==================== CHECK ====================

    @ParameterizedTest(name = "Check {0} should be valid")
    @ValueSource(strings = {"00", "01", "50", "97", "99"})
    @DisplayName("validate - Valid check values (two digits) - Passes")
    void validate_validCheckValues_passes(String check) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", "A");
        contestData.put("check", check);
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("CW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isTrue();
    }

    @ParameterizedTest(name = "Check ''{0}'' should be invalid")
    @ValueSource(strings = {"1997", "7", "AB", "2024", ""})
    @DisplayName("validate - Invalid check format - Fails")
    void validate_invalidCheckFormat_fails(String check) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", "A");
        contestData.put("check", check);
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("validate - Missing check - Fails")
    void validate_missingCheck_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", "A");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Check"));
    }

    // ==================== SECTION ====================

    @Test
    @DisplayName("validate - Missing section - Fails")
    void validate_missingSection_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", "A");
        contestData.put("check", "97");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("section is required"));
    }

    @ParameterizedTest(name = "Section {0} should be a known ARRL/RAC section")
    @ValueSource(strings = {"CT", "ORG", "MI", "PAC", "QC", "AB", "ONE"})
    @DisplayName("validate - Known ARRL/RAC sections - No section warning")
    void validate_knownSections_noSectionWarning(String section) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", "A");
        contestData.put("check", "97");
        contestData.put("section", section);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("CW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).noneMatch(w -> w.contains("Unknown ARRL/RAC section"));
    }

    @Test
    @DisplayName("validate - Unknown section - Warns but valid")
    void validate_unknownSection_warnsButValid() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", "A");
        contestData.put("check", "97");
        contestData.put("section", "XX");  // Non-existent section

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("CW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isTrue();  // Unknown section is a warning, not an error
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Unknown ARRL/RAC section"));
    }

    // ==================== MODE ====================

    @Test
    @DisplayName("validate - Non-standard mode - Warns but valid")
    void validate_nonStandardMode_warnsButValid() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("serial", 1);
        contestData.put("precedence", "A");
        contestData.put("check", "97");
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("FT8")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sweepstakesContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("non-standard"));
    }

    // ==================== METADATA ====================

    @Test
    @DisplayName("getContestCode - Returns ARRL-SS")
    void getContestCode_returnsARRLSS() {
        assertThat(validator.getContestCode()).isEqualTo("ARRL-SS");
    }

    @Test
    @DisplayName("getDescription - Returns description")
    void getDescription_returnsDescription() {
        assertThat(validator.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("supports - Matching contest code returns true")
    void supports_matchingContestCode_returnsTrue() {
        Contest contest = TestDataBuilder.sweepstakesContest().build();
        assertThat(validator.supports(contest)).isTrue();
    }
}
