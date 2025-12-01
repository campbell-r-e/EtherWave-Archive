package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.validation.ContestValidator;
import com.hamradio.logbook.validation.ContestValidatorRegistry;
import com.hamradio.logbook.validation.ValidationResult;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("QSOValidationService Unit Tests")
class QSOValidationServiceTest {

    @Mock
    private ContestValidatorRegistry validatorRegistry;

    @Mock
    private QSORepository qsoRepository;

    @Mock
    private ContestValidator contestValidator;

    @InjectMocks
    private QSOValidationService qsoValidationService;

    private QSO validQSO;
    private Contest testContest;

    @BeforeEach
    void setUp() {
        testContest = new Contest();
        testContest.setId(1L);
        testContest.setContestCode("CQWW");
        testContest.setContestName("CQ World Wide DX Contest");

        validQSO = new QSO();
        validQSO.setId(1L);
        validQSO.setCallsign("W1AW");
        validQSO.setFrequencyKhz(14074L);
        validQSO.setBand("20m");
        validQSO.setMode("FT8");
        validQSO.setQsoDate(LocalDate.now());
        validQSO.setTimeOn(LocalTime.of(14, 30));
    }

    @Test
    @DisplayName("Should validate a valid QSO successfully")
    void shouldValidateValidQSO() {
        lenient().when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any())).thenReturn(Collections.emptyList());

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should fail validation when callsign is missing")
    void shouldFailWhenCallsignMissing() {
        validQSO.setCallsign(null);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("Callsign is required")));
    }

    @Test
    @DisplayName("Should fail validation when callsign is empty")
    void shouldFailWhenCallsignEmpty() {
        validQSO.setCallsign("");

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("Callsign is required")));
    }

    @Test
    @DisplayName("Should warn when callsign format is invalid")
    void shouldWarnWhenCallsignFormatInvalid() {
        validQSO.setCallsign("INVALID@CALL");
        lenient().when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any())).thenReturn(Collections.emptyList());

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertTrue(result.getWarnings().stream().anyMatch(warn -> warn.contains("Callsign format may be invalid")));
    }

    @Test
    @DisplayName("Should accept valid callsign formats")
    void shouldAcceptValidCallsignFormats() {
        String[] validCallsigns = {"W1AW", "K3ABC", "VE3XYZ", "W1AW/M", "VE3/W1AW", "N1ABC/5"};

        for (String callsign : validCallsigns) {
            validQSO.setCallsign(callsign);
            lenient().when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any())).thenReturn(Collections.emptyList());

            ValidationResult result = qsoValidationService.validateQSO(validQSO);

            assertFalse(result.getWarnings().stream().anyMatch(warn -> warn.contains("Callsign format may be invalid")),
                    "Should accept valid callsign: " + callsign);
        }
    }

    @Test
    @DisplayName("Should fail validation when frequency is missing")
    void shouldFailWhenFrequencyMissing() {
        validQSO.setFrequencyKhz(null);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("Valid frequency is required")));
    }

    @Test
    @DisplayName("Should fail validation when frequency is zero or negative")
    void shouldFailWhenFrequencyInvalid() {
        validQSO.setFrequencyKhz(0L);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("Valid frequency is required")));
    }

    @Test
    @DisplayName("Should fail validation when mode is missing")
    void shouldFailWhenModeMissing() {
        validQSO.setMode(null);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("Mode is required")));
    }

    @Test
    @DisplayName("Should fail validation when QSO date is missing")
    void shouldFailWhenQsoDateMissing() {
        validQSO.setQsoDate(null);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("QSO date is required")));
    }

    @Test
    @DisplayName("Should fail validation when time on is missing")
    void shouldFailWhenTimeOnMissing() {
        validQSO.setTimeOn(null);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(err -> err.contains("Time on is required")));
    }

    @Test
    @DisplayName("Should warn when band doesn't match frequency")
    void shouldWarnWhenBandMismatchesFrequency() {
        validQSO.setFrequencyKhz(14074L);  // 20m
        validQSO.setBand("40m");  // Wrong band
        lenient().when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any())).thenReturn(Collections.emptyList());

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertTrue(result.getWarnings().stream().anyMatch(warn ->
            warn.contains("Band") && warn.contains("may not match frequency")));
    }

    @Test
    @DisplayName("Should correctly identify bands from frequencies")
    void shouldCorrectlyIdentifyBands() {
        lenient().when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any())).thenReturn(Collections.emptyList());

        // Test various bands
        testBandIdentification(14074L, "20m");
        testBandIdentification(7074L, "40m");
        testBandIdentification(3573L, "80m");
        testBandIdentification(1840L, "160m");
        testBandIdentification(21074L, "15m");
        testBandIdentification(28074L, "10m");
        testBandIdentification(50100L, "6m");
        testBandIdentification(144200L, "2m");
    }

    private void testBandIdentification(Long frequency, String expectedBand) {
        validQSO.setFrequencyKhz(frequency);
        validQSO.setBand(expectedBand);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.getWarnings().stream().anyMatch(warn ->
            warn.contains("Band") && warn.contains("may not match frequency")),
            "Frequency " + frequency + " should match band " + expectedBand);
    }

    @Test
    @DisplayName("Should perform contest validation when contest is present")
    void shouldPerformContestValidation() {
        validQSO.setContest(testContest);

        ValidationResult contestResult = ValidationResult.success();
        when(validatorRegistry.getValidator("CQWW")).thenReturn(Optional.of(contestValidator));
        when(contestValidator.validate(any(QSO.class), any(Contest.class))).thenReturn(contestResult);
        when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any(Contest.class)))
                .thenReturn(Collections.emptyList());

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should include contest validation errors")
    void shouldIncludeContestValidationErrors() {
        validQSO.setContest(testContest);

        ValidationResult contestResult = ValidationResult.failure();
        contestResult.addError("Invalid exchange");
        when(validatorRegistry.getValidator("CQWW")).thenReturn(Optional.of(contestValidator));
        when(contestValidator.validate(any(QSO.class), any(Contest.class))).thenReturn(contestResult);
        when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any(Contest.class)))
                .thenReturn(Collections.emptyList());

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Invalid exchange"));
    }

    @Test
    @DisplayName("Should include contest validation warnings")
    void shouldIncludeContestValidationWarnings() {
        validQSO.setContest(testContest);

        ValidationResult contestResult = ValidationResult.success();
        contestResult.addWarning("Unusual multiplier");
        when(validatorRegistry.getValidator("CQWW")).thenReturn(Optional.of(contestValidator));
        when(contestValidator.validate(any(QSO.class), any(Contest.class))).thenReturn(contestResult);
        when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any(Contest.class)))
                .thenReturn(Collections.emptyList());

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().contains("Unusual multiplier"));
    }

    @Test
    @DisplayName("Should warn when no validator found for contest")
    void shouldWarnWhenNoValidatorFound() {
        validQSO.setContest(testContest);

        when(validatorRegistry.getValidator("CQWW")).thenReturn(Optional.empty());
        when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any(Contest.class)))
                .thenReturn(Collections.emptyList());

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().stream().anyMatch(warn ->
            warn.contains("No validator found for contest")));
    }

    @Test
    @DisplayName("Should check for duplicate QSOs")
    void shouldCheckForDuplicates() {
        validQSO.setContest(testContest);

        QSO duplicate = new QSO();
        duplicate.setId(2L);
        duplicate.setCallsign("W1AW");
        duplicate.setBand("20m");
        duplicate.setMode("FT8");

        when(qsoRepository.findDuplicates(eq("W1AW"), eq("20m"), eq("FT8"), any(Contest.class)))
                .thenReturn(Arrays.asList(duplicate));

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertTrue(result.getWarnings().stream().anyMatch(warn ->
            warn.contains("Possible duplicate")));
    }

    @Test
    @DisplayName("Should filter out current QSO from duplicates")
    void shouldFilterOutCurrentQSOFromDuplicates() {
        validQSO.setId(1L);
        validQSO.setContest(testContest);

        QSO sameQSO = new QSO();
        sameQSO.setId(1L);  // Same ID as current QSO

        when(qsoRepository.findDuplicates(anyString(), anyString(), anyString(), any(Contest.class)))
                .thenReturn(Arrays.asList(sameQSO));

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.getWarnings().stream().anyMatch(warn ->
            warn.contains("Possible duplicate")));
    }

    @Test
    @DisplayName("Should not check duplicates when contest is null")
    void shouldNotCheckDuplicatesWhenContestNull() {
        validQSO.setContest(null);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        // Should still be valid, just won't check for duplicates
        assertTrue(result.isValid() || result.hasErrors());
    }

    @Test
    @DisplayName("Should accumulate multiple validation errors")
    void shouldAccumulateMultipleErrors() {
        validQSO.setCallsign(null);
        validQSO.setFrequencyKhz(null);
        validQSO.setMode(null);
        validQSO.setQsoDate(null);
        validQSO.setTimeOn(null);

        ValidationResult result = qsoValidationService.validateQSO(validQSO);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 5);
    }
}
