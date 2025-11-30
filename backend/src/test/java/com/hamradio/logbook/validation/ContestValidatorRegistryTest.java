package com.hamradio.logbook.validation;

import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Contest Validator Registry Tests")
class ContestValidatorRegistryTest {

    private ContestValidatorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ContestValidatorRegistry();
    }

    // ==================== VALIDATOR REGISTRATION TESTS ====================

    @Test
    @DisplayName("getValidator - ARRL Field Day - Returns FieldDayValidator")
    void getValidator_arrlFieldDay_returnsFieldDayValidator() {
        // Act
        ContestValidator validator = registry.getValidator("ARRL-FD");

        // Assert
        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(FieldDayValidator.class);
    }

    @Test
    @DisplayName("getValidator - Winter Field Day - Returns WinterFieldDayValidator")
    void getValidator_winterFieldDay_returnsWinterFieldDayValidator() {
        // Act
        ContestValidator validator = registry.getValidator("WFD");

        // Assert
        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(WinterFieldDayValidator.class);
    }

    @Test
    @DisplayName("getValidator - POTA - Returns POTAValidator")
    void getValidator_pota_returnsPOTAValidator() {
        // Act
        ContestValidator validator = registry.getValidator("POTA");

        // Assert
        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(POTAValidator.class);
    }

    @Test
    @DisplayName("getValidator - SOTA - Returns SOTAValidator")
    void getValidator_sota_returnsSOTAValidator() {
        // Act
        ContestValidator validator = registry.getValidator("SOTA");

        // Assert
        assertThat(validator).isNotNull();
        assertThat(validator).isInstanceOf(SOTAValidator.class);
    }

    @Test
    @DisplayName("getValidator - Unknown Contest - Returns Null")
    void getValidator_unknownContest_returnsNull() {
        // Act
        ContestValidator validator = registry.getValidator("UNKNOWN-CONTEST");

        // Assert
        assertThat(validator).isNull();
    }

    // ==================== VALIDATE METHOD TESTS ====================

    @Test
    @DisplayName("validate - Valid ARRL Field Day QSO - Returns No Errors")
    void validate_validArrlFieldDayQSO_returnsNoErrors() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        // Act
        List<String> errors = registry.validate("ARRL-FD", qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Invalid ARRL Field Day QSO - Returns Errors")
    void validate_invalidArrlFieldDayQSO_returnsErrors() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{\"class\":\"INVALID\",\"section\":\"XXX\"}")
                .build();

        // Act
        List<String> errors = registry.validate("ARRL-FD", qso);

        // Assert
        assertThat(errors).isNotEmpty();
    }

    @Test
    @DisplayName("validate - Valid Winter Field Day QSO - Returns No Errors")
    void validate_validWinterFieldDayQSO_returnsNoErrors() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{\"class\":\"2O\",\"section\":\"ORG\"}")
                .build();

        // Act
        List<String> errors = registry.validate("WFD", qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Valid POTA QSO - Returns No Errors")
    void validate_validPOTAQSO_returnsNoErrors() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{\"park_ref\":\"K-0817\"}")
                .build();

        // Act
        List<String> errors = registry.validate("POTA", qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Valid SOTA QSO - Returns No Errors")
    void validate_validSOTAQSO_returnsNoErrors() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{\"summit_ref\":\"W7W/CN-001\"}")
                .build();

        // Act
        List<String> errors = registry.validate("SOTA", qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Unknown Contest - Returns Empty List")
    void validate_unknownContest_returnsEmptyList() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null)
                .contestData("{}")
                .build();

        // Act
        List<String> errors = registry.validate("UNKNOWN-CONTEST", qso);

        // Assert
        assertThat(errors).isEmpty(); // No validator = no errors
    }

    // ==================== CASE SENSITIVITY TESTS ====================

    @Test
    @DisplayName("getValidator - Case Insensitive - Returns Validator")
    void getValidator_caseInsensitive_returnsValidator() {
        // Act
        ContestValidator validator1 = registry.getValidator("arrl-fd");
        ContestValidator validator2 = registry.getValidator("ARRL-FD");
        ContestValidator validator3 = registry.getValidator("ArRl-Fd");

        // Assert - All should return the same type of validator
        assertThat(validator1).isNotNull();
        assertThat(validator2).isNotNull();
        assertThat(validator3).isNotNull();
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("registerValidator - Custom Validator - Registers Successfully")
    void registerValidator_customValidator_registersSuccessfully() {
        // Arrange
        ContestValidator customValidator = new ContestValidator() {
            @Override
            public List<String> validate(QSO qso) {
                return List.of();
            }
        };

        // Act
        registry.registerValidator("CUSTOM-CONTEST", customValidator);
        ContestValidator retrieved = registry.getValidator("CUSTOM-CONTEST");

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved).isEqualTo(customValidator);
    }

    @Test
    @DisplayName("registerValidator - Replace Existing - Replaces Validator")
    void registerValidator_replaceExisting_replacesValidator() {
        // Arrange
        ContestValidator customValidator = new ContestValidator() {
            @Override
            public List<String> validate(QSO qso) {
                return List.of("Custom error");
            }
        };

        // Act
        registry.registerValidator("ARRL-FD", customValidator);
        ContestValidator retrieved = registry.getValidator("ARRL-FD");

        // Assert
        assertThat(retrieved).isEqualTo(customValidator);
    }

    // ==================== GET ALL VALIDATORS TESTS ====================

    @Test
    @DisplayName("getAllValidators - Returns All Registered Validators")
    void getAllValidators_returnsAllRegisteredValidators() {
        // Act
        var validators = registry.getAllValidators();

        // Assert
        assertThat(validators).isNotNull();
        assertThat(validators).containsKeys("ARRL-FD", "WFD", "POTA", "SOTA");
    }

    @Test
    @DisplayName("getSupportedContests - Returns List of Supported Contest Codes")
    void getSupportedContests_returnsListOfSupportedCodes() {
        // Act
        List<String> supported = registry.getSupportedContests();

        // Assert
        assertThat(supported).isNotNull();
        assertThat(supported).contains("ARRL-FD", "WFD", "POTA", "SOTA");
    }

    // ==================== VALIDATION CHAIN TESTS ====================

    @Test
    @DisplayName("validate - Multiple Validators - Each Validates Independently")
    void validate_multipleValidators_eachValidatesIndependently() {
        // Arrange
        QSO validFieldDayQSO = TestDataBuilder.aValidQSO(null, null)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        QSO validPOTAQSO = TestDataBuilder.aValidQSO(null, null)
                .contestData("{\"park_ref\":\"K-0817\"}")
                .build();

        // Act
        List<String> fdErrors = registry.validate("ARRL-FD", validFieldDayQSO);
        List<String> potaErrors = registry.validate("POTA", validPOTAQSO);

        // Assert
        assertThat(fdErrors).isEmpty();
        assertThat(potaErrors).isEmpty();

        // Cross-validation should fail
        List<String> fdWithPotaData = registry.validate("ARRL-FD", validPOTAQSO);
        List<String> potaWithFdData = registry.validate("POTA", validFieldDayQSO);

        assertThat(fdWithPotaData).isNotEmpty(); // POTA data not valid for Field Day
        assertThat(potaWithFdData).isNotEmpty(); // Field Day data not valid for POTA
    }

    // ==================== NULL HANDLING TESTS ====================

    @Test
    @DisplayName("validate - Null Contest Code - Returns Empty List")
    void validate_nullContestCode_returnsEmptyList() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, null).build();

        // Act
        List<String> errors = registry.validate(null, qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Null QSO - Handles Gracefully")
    void validate_nullQSO_handlesGracefully() {
        // Act & Assert
        assertThatCode(() -> registry.validate("ARRL-FD", null))
                .doesNotThrowAnyException();
    }

    // ==================== UNREGISTER TESTS ====================

    @Test
    @DisplayName("unregisterValidator - Existing Validator - Removes Successfully")
    void unregisterValidator_existingValidator_removesSuccessfully() {
        // Arrange
        assertThat(registry.getValidator("ARRL-FD")).isNotNull();

        // Act
        registry.unregisterValidator("ARRL-FD");

        // Assert
        assertThat(registry.getValidator("ARRL-FD")).isNull();
    }

    @Test
    @DisplayName("unregisterValidator - Non-Existent Validator - No Error")
    void unregisterValidator_nonExistentValidator_noError() {
        // Act & Assert
        assertThatCode(() -> registry.unregisterValidator("NON-EXISTENT"))
                .doesNotThrowAnyException();
    }
}
