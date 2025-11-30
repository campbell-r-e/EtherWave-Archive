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

@DisplayName("POTA Validator Tests")
class POTAValidatorTest {

    private POTAValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new POTAValidator();
        objectMapper = new ObjectMapper();
    }

    // ==================== VALID PARK REFERENCE TESTS ====================

    @ParameterizedTest(name = "Park reference {0} should be valid")
    @ValueSource(strings = {
            // US Parks
            "K-0817", "K-1234", "K-9999",
            // Canadian Parks
            "VE-0100", "VE-9999",
            // UK Parks
            "G-0001", "G-9999",
            // Germany
            "DL-0001",
            // Japan
            "JA-0001",
            // Multi-character country codes
            "VE3-0001", "W1-0001"
    })
    @DisplayName("validate - Valid POTA Park References - Passes")
    void validate_validParkReferences_passes(String parkRef) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", parkRef);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== INVALID PARK REFERENCE TESTS ====================

    @ParameterizedTest(name = "Park reference {0} should be invalid")
    @ValueSource(strings = {
            "INVALID", "K", "K-", "K-ABCD", "123-4567", "", "K-12345", // Too many digits
            "k-0817", // Lowercase
            "K 0817", // Space instead of hyphen
            "K_0817"  // Underscore instead of hyphen
    })
    @DisplayName("validate - Invalid POTA Park References - Fails")
    void validate_invalidParkReferences_fails(String parkRef) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", parkRef);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("park");
    }

    // ==================== MISSING FIELD TESTS ====================

    @Test
    @DisplayName("validate - Missing Park Reference - Fails")
    void validate_missingParkRef_fails() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        // park_ref missing

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("park");
    }

    @Test
    @DisplayName("validate - Null Contest Data - Fails")
    void validate_nullContestData_fails() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(null)
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    // ==================== FORMAT TESTS ====================

    @Test
    @DisplayName("validate - Correct Format - Country-Number - Passes")
    void validate_correctFormat_countryNumber_passes() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", "K-0817");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Four Digit Park Number - Valid")
    void validate_fourDigitParkNumber_valid() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", "K-1234");

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
    @DisplayName("validate - Empty Contest Data JSON - Fails")
    void validate_emptyContestData_fails() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{}")
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
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
    @DisplayName("validate - Whitespace in Park Ref - Trims Correctly")
    void validate_whitespaceInParkRef_trimsCorrectly() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", " K-0817 ");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== MULTIPLE PARKS TESTS ====================

    @Test
    @DisplayName("validate - Multiple Parks - Accepts Comma Separated")
    void validate_multipleParks_acceptsCommaSeparated() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", "K-0817,K-0818,K-0819");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Multiple Parks with Invalid One - Fails")
    void validate_multipleParksWithInvalid_fails() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("park_ref", "K-0817,INVALID,K-0819");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("invalid");
    }

    // ==================== COUNTRY CODE TESTS ====================

    @Test
    @DisplayName("validate - Various Country Codes - All Valid")
    void validate_variousCountryCodes_allValid() throws Exception {
        String[] validRefs = {"K-0817", "VE-0100", "G-0001", "DL-0001", "JA-0001", "W-0100"};

        for (String ref : validRefs) {
            ObjectNode contestData = objectMapper.createObjectNode();
            contestData.put("park_ref", ref);

            QSO qso = TestDataBuilder.aValidQSO(null, null)
                    .contestData(objectMapper.writeValueAsString(contestData))
                    .build();

            List<String> errors = validator.validate(qso);
            assertThat(errors).isEmpty();
        }
    }
}
