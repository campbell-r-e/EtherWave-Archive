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

@DisplayName("POTA Validator Tests")
class POTAValidatorTest {

    private POTAValidator validator;
    private ObjectMapper objectMapper;
    private Contest potaContest;

    @BeforeEach
    void setUp() {
        validator = new POTAValidator();
        objectMapper = new ObjectMapper();
        potaContest = TestDataBuilder.potaContest().build();
    }

    @ParameterizedTest(name = "Park reference {0} should be valid")
    @ValueSource(strings = {"K-0817", "K-1234", "K-12345", "VE-1234", "G-0001", "DL-00001"})
    @DisplayName("validate - Valid POTA Park References - Passes")
    void validate_validParkReferences_passes(String parkRef) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", parkRef);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, potaContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @ParameterizedTest(name = "Park reference {0} should be invalid")
    @ValueSource(strings = {"K817", "K-", "K-ABC", "1234", "KABCD", "K-1", "k-1234"})
    @DisplayName("validate - Invalid POTA Park References - Fails")
    void validate_invalidParkReferences_fails(String parkRef) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", parkRef);

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, potaContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("validate - Missing contest data - Fails")
    void validate_missingContestData_fails() {
        QSO qso = QSO.builder().callsign("W1AW").build();
        ValidationResult result = validator.validate(qso, potaContest);
        assertThat(result.isValid()).isFalse();
    }
}
