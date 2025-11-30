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

@DisplayName("Winter Field Day Validator Tests")
class WinterFieldDayValidatorTest {

    private WinterFieldDayValidator validator;
    private ObjectMapper objectMapper;
    private Contest winterFieldDayContest;

    @BeforeEach
    void setUp() {
        validator = new WinterFieldDayValidator();
        objectMapper = new ObjectMapper();
        winterFieldDayContest = TestDataBuilder.winterFieldDayContest().build();
    }

    @ParameterizedTest(name = "Class {0} should be valid")
    @ValueSource(strings = {"1O", "2O", "3O", "4O", "5O", "1I", "2I", "3I", "1H", "2H"})
    @DisplayName("validate - Valid Winter Field Day Classes - Passes")
    void validate_validClasses_passes(String wfdClass) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", wfdClass);
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, winterFieldDayContest);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @ParameterizedTest(name = "Class {0} should be invalid")
    @ValueSource(strings = {"0O", "10O", "1A", "1X", "ABC"})
    @DisplayName("validate - Invalid Winter Field Day Classes - Fails")
    void validate_invalidClasses_fails(String wfdClass) throws Exception {
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", wfdClass);
        contestData.put("section", "ORG");

        QSO qso = QSO.builder()
                .callsign("W1AW")
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        ValidationResult result = validator.validate(qso, winterFieldDayContest);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("validate - Missing contest data - Fails")
    void validate_missingContestData_fails() {
        QSO qso = QSO.builder().callsign("W1AW").build();
        ValidationResult result = validator.validate(qso, winterFieldDayContest);
        assertThat(result.isValid()).isFalse();
    }
}
