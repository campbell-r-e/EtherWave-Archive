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

@DisplayName("SOTA Validator Tests")
class SOTAValidatorTest {

    private SOTAValidator validator;
    private ObjectMapper objectMapper;
    private Contest sotaContest;

    @BeforeEach
    void setUp() {
        validator = new SOTAValidator();
        objectMapper = new ObjectMapper();
        sotaContest = TestDataBuilder.sotaContest().build();
    }

    @ParameterizedTest(name = "Summit reference {0} should be valid")
    @ValueSource(strings = {"W7W/NG-001", "G/LD-001", "W/AZ-001", "VE/AB-001", "ZL/MB-001"})
    @DisplayName("validate - Valid SOTA Summit References - Passes")
    void validate_validSummitReferences_passes(String summitRef) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", summitRef);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sotaContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @ParameterizedTest(name = "Summit reference {0} should be invalid")
    @ValueSource(strings = {"W7W-001", "G/LD001", "INVALID", "W/AZ", "001", "w7w/ng-001"})
    @DisplayName("validate - Invalid SOTA Summit References - Fails")
    void validate_invalidSummitReferences_fails(String summitRef) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", summitRef);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sotaContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("validate - Missing contest data - Fails")
    void validate_missingContestData_fails() {
        QSO qso = QSO.builder().callsign("W1AW").build();
        ValidationResult result = validator.validate(qso, sotaContest);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("validate - Missing summit_ref - Fails")
    void validate_missingSummitRef_fails() throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("points", 4);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, sotaContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }
}
