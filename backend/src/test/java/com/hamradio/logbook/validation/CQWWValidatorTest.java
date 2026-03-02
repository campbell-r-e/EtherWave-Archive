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

@DisplayName("CQ WW DX Validator Tests")
class CQWWValidatorTest {

    private CQWWValidator validator;
    private ObjectMapper objectMapper;
    private Contest cqwwContest;

    @BeforeEach
    void setUp() {
        validator = new CQWWValidator();
        objectMapper = new ObjectMapper();
        cqwwContest = TestDataBuilder.cqwwContest().build();
    }

    // ==================== CQ ZONE VALIDATION ====================

    @ParameterizedTest(name = "CQ Zone {0} should be valid")
    @ValueSource(ints = {1, 3, 5, 14, 25, 40})
    @DisplayName("validate - Valid CQ Zones (1-40) - Passes")
    void validate_validCqZones_passes(int cqZone) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("cq_zone", cqZone);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("SSB")
                .frequencyKhz(14250L)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @ParameterizedTest(name = "CQ Zone {0} should be invalid")
    @ValueSource(ints = {0, -1, 41, 100})
    @DisplayName("validate - Invalid CQ Zones (out of range) - Fails")
    void validate_invalidCqZones_fails(int cqZone) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("cq_zone", cqZone);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("SSB")
                .frequencyKhz(14250L)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("validate - Missing CQ Zone - Fails")
    void validate_missingCqZone_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        // No cq_zone field

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("SSB")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("CQ Zone is required"));
    }

    // ==================== CONTEST DATA VALIDATION ====================

    @Test
    @DisplayName("validate - Missing contest data - Fails")
    void validate_missingContestData_fails() {
        QSO qso = QSO.builder().callsign("W1AW").build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Contest data is required"));
    }

    @Test
    @DisplayName("validate - Empty contest data - Fails")
    void validate_emptyContestData_fails() {
        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData("")
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isFalse();
    }

    // ==================== FREQUENCY VALIDATION ====================

    @ParameterizedTest(name = "Frequency {0} kHz should be a valid CQ WW band")
    @ValueSource(longs = {1850, 3750, 7150, 14250, 21300, 28500})
    @DisplayName("validate - Valid CQ WW band frequencies - No frequency warning")
    void validate_validBandFrequencies_noFrequencyWarning(long freqKhz) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("cq_zone", 5);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("SSB")
                .frequencyKhz(freqKhz)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).noneMatch(w -> w.contains("outside CQ WW contest bands"));
    }

    @Test
    @DisplayName("validate - Out-of-band frequency - Warns but valid")
    void validate_outOfBandFrequency_warnsButValid() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("cq_zone", 5);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("SSB")
                .frequencyKhz(50100L)  // 6m - not in CQ WW
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isTrue();  // Frequency issues are warnings, not errors
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("outside CQ WW contest bands"));
    }

    @Test
    @DisplayName("validate - Missing frequency - Warns but valid")
    void validate_missingFrequency_warnsButValid() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("cq_zone", 5);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("SSB")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Frequency not specified"));
    }

    // ==================== MODE VALIDATION ====================

    @ParameterizedTest(name = "Mode {0} should be standard for CQ WW")
    @ValueSource(strings = {"CW", "SSB", "AM", "FM"})
    @DisplayName("validate - Standard CQ WW modes - No mode warning")
    void validate_standardModes_noModeWarning(String mode) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("cq_zone", 5);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode(mode)
                .frequencyKhz(14250L)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.getWarnings()).noneMatch(w -> w.contains("non-standard"));
    }

    @Test
    @DisplayName("validate - Non-standard mode - Warns but valid")
    void validate_nonStandardMode_warnsButValid() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("cq_zone", 5);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .mode("FT8")
                .frequencyKhz(14074L)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, cqwwContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("non-standard"));
    }

    // ==================== METADATA ====================

    @Test
    @DisplayName("getContestCode - Returns CQWW")
    void getContestCode_returnsCQWW() {
        assertThat(validator.getContestCode()).isEqualTo("CQWW");
    }

    @Test
    @DisplayName("getDescription - Returns description")
    void getDescription_returnsDescription() {
        assertThat(validator.getDescription()).isNotBlank();
    }

    @Test
    @DisplayName("supports - Matching contest code returns true")
    void supports_matchingContestCode_returnsTrue() {
        Contest contest = TestDataBuilder.cqwwContest().build();
        assertThat(validator.supports(contest)).isTrue();
    }

    @Test
    @DisplayName("supports - Non-matching contest code returns false")
    void supports_nonMatchingContestCode_returnsFalse() {
        Contest contest = TestDataBuilder.fieldDayContest().build();
        assertThat(validator.supports(contest)).isFalse();
    }
}
