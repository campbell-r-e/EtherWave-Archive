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

@DisplayName("SOTA Validator Tests")
class SOTAValidatorTest {

    private SOTAValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new SOTAValidator();
        objectMapper = new ObjectMapper();
    }

    // ==================== VALID SUMMIT REFERENCE TESTS ====================

    @ParameterizedTest(name = "Summit reference {0} should be valid")
    @ValueSource(strings = {
            // US Summits
            "W7W/CN-001", "W6/CT-001", "W0C/FR-001",
            // Canadian Summits
            "VE7/VI-001", "VE3/ON-001",
            // UK Summits
            "G/LD-001", "GM/SS-001",
            // European Summits
            "HB/BE-001", "OE/ST-001", "DL/BW-001",
            // Asian Summits
            "JA/TK-001", "VK1/AC-001"
    })
    @DisplayName("validate - Valid SOTA Summit References - Passes")
    void validate_validSummitReferences_passes(String summitRef) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", summitRef);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== INVALID SUMMIT REFERENCE TESTS ====================

    @ParameterizedTest(name = "Summit reference {0} should be invalid")
    @ValueSource(strings = {
            "INVALID", "W7W", "W7W/", "W7W/CN", "W7W/CN-", "123/456-789",
            "", "w7w/cn-001", // Lowercase
            "W7W CN-001", // Missing slash
            "W7W/CN_001", // Underscore instead of hyphen
            "W7W-CN-001"  // Wrong separator
    })
    @DisplayName("validate - Invalid SOTA Summit References - Fails")
    void validate_invalidSummitReferences_fails(String summitRef) throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", summitRef);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("summit");
    }

    // ==================== MISSING FIELD TESTS ====================

    @Test
    @DisplayName("validate - Missing Summit Reference - Fails")
    void validate_missingSummitRef_fails() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        // summit_ref missing

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).containsIgnoringCase("summit");
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
    @DisplayName("validate - Correct Format - Association/Region-Number - Passes")
    void validate_correctFormat_associationRegionNumber_passes() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", "W7W/CN-001");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Three Digit Summit Number - Valid")
    void validate_threeDigitSummitNumber_valid() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", "W7W/CN-123");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Four Digit Summit Number - Valid")
    void validate_fourDigitSummitNumber_valid() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", "W7W/CN-1234");

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
    @DisplayName("validate - Whitespace in Summit Ref - Trims Correctly")
    void validate_whitespaceInSummitRef_trimsCorrectly() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", " W7W/CN-001 ");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== REGION CODE TESTS ====================

    @Test
    @DisplayName("validate - Single Character Region - Valid")
    void validate_singleCharacterRegion_valid() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", "G/L-001");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Two Character Region - Valid")
    void validate_twoCharacterRegion_valid() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", "G/LD-001");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Three Character Region - Valid")
    void validate_threeCharacterRegion_valid() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", "W7W/ABC-001");

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== ASSOCIATION CODE TESTS ====================

    @Test
    @DisplayName("validate - Various Association Codes - All Valid")
    void validate_variousAssociationCodes_allValid() throws Exception {
        String[] validRefs = {
                "W7W/CN-001",  // US
                "VE7/VI-001",  // Canada
                "G/LD-001",    // UK
                "HB/BE-001",   // Switzerland
                "DL/BW-001",   // Germany
                "JA/TK-001"    // Japan
        };

        for (String ref : validRefs) {
            ObjectNode contestData = objectMapper.createObjectNode();
            contestData.put("summit_ref", ref);

            QSO qso = TestDataBuilder.aValidQSO(null, null)
                    .contestData(objectMapper.writeValueAsString(contestData))
                    .build();

            List<String> errors = validator.validate(qso);
            assertThat(errors).isEmpty();
        }
    }

    // ==================== ALTITUDE TESTS ====================

    @Test
    @DisplayName("validate - Summit with Altitude - Accepts Optional Altitude")
    void validate_summitWithAltitude_acceptsOptionalAltitude() throws Exception {
        // Arrange
        ObjectNode contestData = objectMapper.createObjectNode();
        contestData.put("summit_ref", "W7W/CN-001");
        contestData.put("altitude", 2500);

        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData(objectMapper.writeValueAsString(contestData))
                .build();

        // Act
        List<String> errors = validator.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }
}
