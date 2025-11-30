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

@DisplayName("Field Day Validator Tests")
class FieldDayValidatorTest {

    private FieldDayValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new FieldDayValidator();
        objectMapper = new ObjectMapper();
    }

    // ==================== VALID CLASS TESTS ====================

    @ParameterizedTest(name = "Class {0} should be valid")
    @ValueSource(strings = {"1A", "2A", "3A", "4A", "5A", "6A", "7A", "8A", "9A", "10A",
                            "1B", "1C", "1D", "1E", "1F", "2F", "3F", "10F", "40F"})
    @DisplayName("validate - Valid Field Day Classes - Passes")
    void validate_validClasses_passes(String fdClass) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", fdClass);
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
    @ValueSource(strings = {"0A", "41A", "1Z", "ABC", "", "1G", "100A", "1a", "2b"})
    @DisplayName("validate - Invalid Field Day Classes - Fails")
    void validate_invalidClasses_fails(String fdClass) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", fdClass);
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
            // California sections
            "ORG", "SCV", "LAX", "SF", "SB", "SDG", "SJV", "SV", "PAC", "EB",
            // Southern sections
            "AL", "GA", "KY", "NC", "NFL", "SFL", "WCF", "PR", "VI", "SC", "TN", "VA",
            // Central sections
            "AR", "LA", "MS", "NM", "NTX", "OK", "STX", "WTX",
            // Others
            "CT", "EMA", "ME", "NH", "RI", "VT", "WMA"
    })
    @DisplayName("validate - Valid ARRL Sections - Passes")
    void validate_validSections_passes(String section) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "2A");
        contestData.put("section", section);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== INVALID SECTION TESTS ====================

    @ParameterizedTest(name = "Section {0} should be invalid")
    @ValueSource(strings = {"XXX", "ABC", "", "org", "OrG", "123"})
    @DisplayName("validate - Invalid ARRL Sections - Fails")
    void validate_invalidSections_fails(String section) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", "2A");
        contestData.put("section", section);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("section");
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
        contestData.put("class", "2A");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("section");
    }

    @Test
    @DisplayName("validate - Missing Contest Data - Fails")
    void validate_missingContestData_fails() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(null)
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("validate - Empty Contest Data JSON - Fails")
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
    @DisplayName("validate - Invalid JSON - Handles Gracefully")
    void validate_invalidJson_handlesGracefully() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("invalid json {")
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    @Test
    @DisplayName("validate - Whitespace in Fields - Trims Correctly")
    void validate_whitespaceInFields_trimsCorrectly() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("class", " 2A ");
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
