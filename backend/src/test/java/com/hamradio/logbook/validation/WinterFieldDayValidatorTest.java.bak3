package com.hamradio.logbook.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Winter Field Day Validator Tests")
class WinterFieldDayValidatorTest {

    private WinterFieldDayValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new WinterFieldDayValidator();
        objectMapper = new ObjectMapper();
    }

    // ==================== VALID CLASS TESTS ====================

    @ParameterizedTest(name = "Class {0} should be valid")
    @ValueSource(strings = {
            "1O", "2O", "3O", "4O", "5O", "6O", "7O", "8O", "9O", "10O",
            "1I", "2I", "3I", "4I", "5I",
            "1H", "2H", "3H"
    })
    @DisplayName("validate - Valid Winter Field Day Classes - Passes")
    void validate_validClasses_passes(String wfdClass) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", wfdClass);
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== INVALID CLASS TESTS ====================

    @ParameterizedTest(name = "Class {0} should be invalid")
    @ValueSource(strings = {"0O", "1A", "2B", "1Z", "ABC", "", "100O", "1o", "2i"})
    @DisplayName("validate - Invalid Winter Field Day Classes - Fails")
    void validate_invalidClasses_fails(String wfdClass) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", wfdClass);
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("class");
    }

    // ==================== VALID SECTION TESTS ====================

    @ParameterizedTest(name = "Section {0} should be valid")
    @ValueSource(strings = {
            "ORG", "SCV", "LAX", "SF", "SB", "SDG", "SJV", "SV", "PAC", "EB",
            "AL", "GA", "KY", "NC", "NFL", "SFL", "WCF", "PR", "VI", "SC", "TN", "VA"
    })
    @DisplayName("validate - Valid ARRL Sections - Passes")
    void validate_validSections_passes(String section) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "2O");
        contestData.put("section", section);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== MISSING FIELD TESTS ====================

    @Test
    @DisplayName("validate - Missing Class - Fails")
    void validate_missingClass_fails() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("class");
    }

    @Test
    @DisplayName("validate - Missing Section - Fails")
    void validate_missingSection_fails() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "2O");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("section");
    }

    // ==================== CLASS TYPE TESTS ====================

    @Test
    @DisplayName("validate - Outdoor Class - Accepts O Suffix")
    void validate_outdoorClass_acceptsOSuffix() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "3O");
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Indoor Class - Accepts I Suffix")
    void validate_indoorClass_acceptsISuffix() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "2I");
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Home Class - Accepts H Suffix")
    void validate_homeClass_acceptsHSuffix() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "1H");
        contestData.put("section", "ORG");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("validate - Empty Contest Data - Fails")
    void validate_emptyContestData_fails() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{}")
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).hasSize(2); // Both class and section missing
    }

    @Test
    @DisplayName("validate - Whitespace in Fields - Trims Correctly")
    void validate_whitespaceInFields_trimsCorrectly() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", " 2O ");
        contestData.put("section", " ORG ");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }
}
