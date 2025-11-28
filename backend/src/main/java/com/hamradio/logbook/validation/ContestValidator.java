package com.hamradio.logbook.validation;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;

/**
 * Interface for contest-specific validation
 * Each contest type implements this interface with its own rules
 */
public interface ContestValidator {

    /**
     * Validate a QSO according to contest rules
     *
     * @param qso The QSO to validate
     * @param contest The contest configuration
     * @return Validation result with errors/warnings
     */
    ValidationResult validate(QSO qso, Contest contest);

    /**
     * Get the contest code this validator handles
     * Example: "ARRL-FD", "POTA", "WFD"
     */
    String getContestCode();

    /**
     * Get a human-readable description of the validator
     */
    String getDescription();

    /**
     * Check if the validator supports a specific contest
     */
    default boolean supports(Contest contest) {
        return contest != null && getContestCode().equals(contest.getContestCode());
    }
}
