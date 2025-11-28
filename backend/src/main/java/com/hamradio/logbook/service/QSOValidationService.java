package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.validation.ContestValidator;
import com.hamradio.logbook.validation.ContestValidatorRegistry;
import com.hamradio.logbook.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for validating QSOs against contest rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QSOValidationService {

    private final ContestValidatorRegistry validatorRegistry;
    private final QSORepository qsoRepository;

    /**
     * Validate a QSO
     * Performs both basic and contest-specific validation
     */
    public ValidationResult validateQSO(QSO qso) {
        ValidationResult result = ValidationResult.success();

        // Basic validation
        performBasicValidation(qso, result);

        // Contest-specific validation
        if (qso.getContest() != null) {
            performContestValidation(qso, qso.getContest(), result);
        }

        // Duplicate check
        checkForDuplicates(qso, result);

        return result;
    }

    /**
     * Perform basic validation (required fields, format checks)
     */
    private void performBasicValidation(QSO qso, ValidationResult result) {
        if (qso.getCallsign() == null || qso.getCallsign().isEmpty()) {
            result.addError("Callsign is required");
        } else if (!isValidCallsign(qso.getCallsign())) {
            result.addWarning("Callsign format may be invalid: " + qso.getCallsign());
        }

        if (qso.getFrequencyKhz() == null || qso.getFrequencyKhz() <= 0) {
            result.addError("Valid frequency is required");
        }

        if (qso.getMode() == null || qso.getMode().isEmpty()) {
            result.addError("Mode is required");
        }

        if (qso.getQsoDate() == null) {
            result.addError("QSO date is required");
        }

        if (qso.getTimeOn() == null) {
            result.addError("Time on is required");
        }

        // Validate band matches frequency
        if (qso.getBand() != null && qso.getFrequencyKhz() != null) {
            String expectedBand = frequencyToBand(qso.getFrequencyKhz());
            if (expectedBand != null && !expectedBand.equals(qso.getBand())) {
                result.addWarning("Band '" + qso.getBand() + "' may not match frequency " + qso.getFrequencyKhz() + " kHz (expected: " + expectedBand + ")");
            }
        }
    }

    /**
     * Perform contest-specific validation
     */
    private void performContestValidation(QSO qso, Contest contest, ValidationResult result) {
        Optional<ContestValidator> validator = validatorRegistry.getValidator(contest.getContestCode());

        if (validator.isEmpty()) {
            result.addWarning("No validator found for contest: " + contest.getContestCode());
            return;
        }

        ValidationResult contestResult = validator.get().validate(qso, contest);
        result.getErrors().addAll(contestResult.getErrors());
        result.getWarnings().addAll(contestResult.getWarnings());

        if (contestResult.hasErrors()) {
            result.setValid(false);
        }
    }

    /**
     * Check for duplicate QSOs
     */
    private void checkForDuplicates(QSO qso, ValidationResult result) {
        if (qso.getContest() != null && qso.getCallsign() != null && qso.getBand() != null && qso.getMode() != null) {
            List<QSO> duplicates = qsoRepository.findDuplicates(
                    qso.getCallsign(),
                    qso.getBand(),
                    qso.getMode(),
                    qso.getContest()
            );

            // Filter out the current QSO if it's an update
            duplicates = duplicates.stream()
                    .filter(dup -> !dup.getId().equals(qso.getId()))
                    .toList();

            if (!duplicates.isEmpty()) {
                result.addWarning("Possible duplicate: " + duplicates.size() + " similar QSO(s) found with " + qso.getCallsign() + " on " + qso.getBand() + " " + qso.getMode());
            }
        }
    }

    /**
     * Basic callsign format validation
     * Format: [prefix]callarea[suffix] (e.g., K3ABC, W1AW/M, VE3/W1AW)
     */
    private boolean isValidCallsign(String callsign) {
        if (callsign == null || callsign.isEmpty()) {
            return false;
        }
        // Very basic regex - accepts most common callsign formats
        return callsign.matches("^[A-Z0-9]{1,3}[0-9][A-Z0-9]{0,4}(/[A-Z0-9]{1,4})?$");
    }

    /**
     * Convert frequency to band
     */
    private String frequencyToBand(Long freqKhz) {
        if (freqKhz == null) return null;

        if (freqKhz >= 1800 && freqKhz <= 2000) return "160m";
        if (freqKhz >= 3500 && freqKhz <= 4000) return "80m";
        if (freqKhz >= 5330 && freqKhz <= 5405) return "60m";
        if (freqKhz >= 7000 && freqKhz <= 7300) return "40m";
        if (freqKhz >= 10100 && freqKhz <= 10150) return "30m";
        if (freqKhz >= 14000 && freqKhz <= 14350) return "20m";
        if (freqKhz >= 18068 && freqKhz <= 18168) return "17m";
        if (freqKhz >= 21000 && freqKhz <= 21450) return "15m";
        if (freqKhz >= 24890 && freqKhz <= 24990) return "12m";
        if (freqKhz >= 28000 && freqKhz <= 29700) return "10m";
        if (freqKhz >= 50000 && freqKhz <= 54000) return "6m";
        if (freqKhz >= 144000 && freqKhz <= 148000) return "2m";
        if (freqKhz >= 222000 && freqKhz <= 225000) return "1.25m";
        if (freqKhz >= 420000 && freqKhz <= 450000) return "70cm";

        return null;
    }
}
