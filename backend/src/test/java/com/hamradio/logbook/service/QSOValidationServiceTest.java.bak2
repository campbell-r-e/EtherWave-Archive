package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.testutil.TestDataBuilder;
import com.hamradio.logbook.validation.ContestValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QSO Validation Service Tests")
class QSOValidationServiceTest {

    @Mock
    private QSORepository qsoRepository;

    @Mock
    private CallsignValidationService callsignValidationService;

    @Mock
    private ContestValidatorRegistry contestValidatorRegistry;

    @InjectMocks
    private QSOValidationService qsoValidationService;

    private User testUser;
    private Station testStation;
    private Log testLog;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser().id(1L).build();
        testStation = TestDataBuilder.aValidStation().id(1L).build();
        testLog = TestDataBuilder.aValidLog(testUser).id(1L).build();
    }

    // ==================== BASIC VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Valid QSO - Passes Validation")
    void validate_validQSO_passesValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog).build();
        when(callsignValidationService.validateFormat("W1AW")).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
        verify(callsignValidationService).validateFormat("W1AW");
    }

    @Test
    @DisplayName("validate - Missing Required Callsign - Fails Validation")
    void validate_missingCallsign_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign(null)
                .build();

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Callsign is required"));
    }

    @Test
    @DisplayName("validate - Invalid Callsign Format - Fails Validation")
    void validate_invalidCallsignFormat_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("INVALID!!!")
                .build();
        when(callsignValidationService.validateFormat("INVALID!!!")).thenReturn(false);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Invalid callsign format"));
    }

    // ==================== FREQUENCY VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Missing Frequency - Fails Validation")
    void validate_missingFrequency_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .frequencyKhz(null)
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Frequency is required"));
    }

    @Test
    @DisplayName("validate - Negative Frequency - Fails Validation")
    void validate_negativeFrequency_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .frequencyKhz(-1L)
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Frequency must be positive"));
    }

    @Test
    @DisplayName("validate - Frequency Out of Range - Fails Validation")
    void validate_frequencyOutOfRange_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .frequencyKhz(999999999L) // Too high
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Frequency out of valid range"));
    }

    @Test
    @DisplayName("validate - Valid Ham Band Frequencies - Passes Validation")
    void validate_validHamBandFrequencies_passesValidation() {
        // Arrange & Act & Assert
        long[] validFreqs = {
                136L,    // 2200m
                475L,    // 630m
                1850L,   // 160m
                3550L,   // 80m
                7030L,   // 40m
                14250L,  // 20m
                21150L,  // 15m
                28450L,  // 10m
                50100L,  // 6m
                144200L, // 2m
                432100L  // 70cm
        };

        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        for (long freq : validFreqs) {
            QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                    .frequencyKhz(freq)
                    .build();
            List<String> errors = qsoValidationService.validate(qso);
            assertThat(errors).isEmpty();
        }
    }

    // ==================== MODE VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Missing Mode - Fails Validation")
    void validate_missingMode_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .mode(null)
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Mode is required"));
    }

    @Test
    @DisplayName("validate - Invalid Mode - Fails Validation")
    void validate_invalidMode_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .mode("INVALID_MODE")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Invalid mode"));
    }

    @Test
    @DisplayName("validate - Valid Modes - Passes Validation")
    void validate_validModes_passesValidation() {
        // Arrange & Act & Assert
        String[] validModes = {"SSB", "CW", "FM", "AM", "FT8", "FT4", "RTTY", "PSK31", "MFSK"};

        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        for (String mode : validModes) {
            QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                    .mode(mode)
                    .build();
            List<String> errors = qsoValidationService.validate(qso);
            assertThat(errors).isEmpty();
        }
    }

    // ==================== DATE/TIME VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Missing Date - Fails Validation")
    void validate_missingDate_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(null)
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("QSO date is required"));
    }

    @Test
    @DisplayName("validate - Missing Time - Fails Validation")
    void validate_missingTime_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .timeOn(null)
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Time is required"));
    }

    @Test
    @DisplayName("validate - Future Date - Fails Validation")
    void validate_futureDate_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.now().plusDays(1))
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("QSO date cannot be in the future"));
    }

    @Test
    @DisplayName("validate - Today's Date - Passes Validation")
    void validate_todaysDate_passesValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.now())
                .timeOn(LocalTime.now())
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== RST VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Invalid RST Sent - Fails Validation")
    void validate_invalidRstSent_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .rstSent("999") // Invalid for SSB (should be 2 digits)
                .mode("SSB")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("RST"));
    }

    @Test
    @DisplayName("validate - Valid RST for SSB - Passes Validation")
    void validate_validRstForSsb_passesValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .mode("SSB")
                .rstSent("59")
                .rstRcvd("57")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Valid RST for CW - Passes Validation")
    void validate_validRstForCw_passesValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .mode("CW")
                .rstSent("599")
                .rstRcvd("579")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== BAND VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Missing Band - Fails Validation")
    void validate_missingBand_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .band(null)
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Band is required"));
    }

    @Test
    @DisplayName("validate - Invalid Band - Fails Validation")
    void validate_invalidBand_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .band("INVALID_BAND")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Invalid band"));
    }

    // ==================== DUPLICATE DETECTION TESTS ====================

    @Test
    @DisplayName("checkDuplicate - Duplicate QSO - Returns True")
    void checkDuplicate_duplicateQSO_returnsTrue() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1AW")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .build();

        QSO existingQSO = TestDataBuilder.aValidQSO(testStation, testLog)
                .id(999L)
                .callsign("W1AW")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .build();

        when(qsoRepository.findByLogIdAndCallsignAndQsoDateAndTimeOn(
                testLog.getId(), "W1AW", LocalDate.of(2025, 1, 15), LocalTime.of(14, 30, 0)))
                .thenReturn(List.of(existingQSO));

        // Act
        boolean isDuplicate = qsoValidationService.checkDuplicate(qso);

        // Assert
        assertThat(isDuplicate).isTrue();
    }

    @Test
    @DisplayName("checkDuplicate - No Duplicate - Returns False")
    void checkDuplicate_noDuplicate_returnsFalse() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1AW")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .build();

        when(qsoRepository.findByLogIdAndCallsignAndQsoDateAndTimeOn(
                testLog.getId(), "W1AW", LocalDate.of(2025, 1, 15), LocalTime.of(14, 30, 0)))
                .thenReturn(Collections.emptyList());

        // Act
        boolean isDuplicate = qsoValidationService.checkDuplicate(qso);

        // Assert
        assertThat(isDuplicate).isFalse();
    }

    @Test
    @DisplayName("checkDuplicate - Same QSO Update - Not a Duplicate")
    void checkDuplicate_sameQSOUpdate_notADuplicate() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .id(100L)
                .callsign("W1AW")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .build();

        // Same QSO being updated
        when(qsoRepository.findByLogIdAndCallsignAndQsoDateAndTimeOn(
                testLog.getId(), "W1AW", LocalDate.of(2025, 1, 15), LocalTime.of(14, 30, 0)))
                .thenReturn(List.of(qso));

        // Act
        boolean isDuplicate = qsoValidationService.checkDuplicate(qso);

        // Assert
        assertThat(isDuplicate).isFalse(); // Not a duplicate of itself
    }

    // ==================== CONTEST VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Contest QSO without Contest Data - Fails Validation")
    void validate_contestQSOWithoutContestData_failsValidation() {
        // Arrange
        Contest fieldDay = Contest.builder().id(1L).contestCode("ARRL-FD").build();
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .contest(fieldDay)
                .contestData(null) // Missing contest data
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Contest data is required"));
    }

    @Test
    @DisplayName("validate - Contest QSO with Valid Contest Data - Passes Validation")
    void validate_contestQSOWithValidContestData_passesValidation() {
        // Arrange
        Contest fieldDay = Contest.builder().id(1L).contestCode("ARRL-FD").build();
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .contest(fieldDay)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);
        when(contestValidatorRegistry.validate(eq("ARRL-FD"), any())).thenReturn(Collections.emptyList());

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isEmpty();
    }

    // ==================== STATION/LOG VALIDATION TESTS ====================

    @Test
    @DisplayName("validate - Missing Station - Fails Validation")
    void validate_missingStation_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(null, testLog).build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Station is required"));
    }

    @Test
    @DisplayName("validate - Missing Log - Fails Validation")
    void validate_missingLog_failsValidation() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, null).build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("Log is required"));
    }

    // ==================== MULTIPLE ERRORS TEST ====================

    @Test
    @DisplayName("validate - Multiple Validation Errors - Returns All Errors")
    void validate_multipleValidationErrors_returnsAllErrors() {
        // Arrange
        QSO qso = QSO.builder()
                .station(testStation)
                .log(testLog)
                .callsign(null) // Missing callsign
                .frequencyKhz(null) // Missing frequency
                .mode(null) // Missing mode
                .band(null) // Missing band
                .qsoDate(null) // Missing date
                .timeOn(null) // Missing time
                .build();

        // Act
        List<String> errors = qsoValidationService.validate(qso);

        // Assert
        assertThat(errors).hasSizeGreaterThan(3);
        assertThat(errors).anyMatch(e -> e.contains("Callsign"));
        assertThat(errors).anyMatch(e -> e.contains("Frequency"));
        assertThat(errors).anyMatch(e -> e.contains("Mode"));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("validate - Grid Square Format - Validates Correctly")
    void validate_gridSquareFormat_validatesCorrectly() {
        // Arrange
        QSO validGrid = TestDataBuilder.aValidQSO(testStation, testLog)
                .gridSquare("FN42ab")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(validGrid);

        // Assert
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate - Invalid Grid Square - Fails Validation")
    void validate_invalidGridSquare_failsValidation() {
        // Arrange
        QSO invalidGrid = TestDataBuilder.aValidQSO(testStation, testLog)
                .gridSquare("INVALID")
                .build();
        when(callsignValidationService.validateFormat(any())).thenReturn(true);

        // Act
        List<String> errors = qsoValidationService.validate(invalidGrid);

        // Assert
        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.contains("grid"));
    }
}
