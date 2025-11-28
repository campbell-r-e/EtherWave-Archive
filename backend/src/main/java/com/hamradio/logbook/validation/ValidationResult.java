package com.hamradio.logbook.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of QSO validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return ValidationResult.builder().valid(true).build();
    }

    /**
     * Create a failed validation result with errors
     */
    public static ValidationResult failure(String... errors) {
        ValidationResult result = ValidationResult.builder().valid(false).build();
        for (String error : errors) {
            result.addError(error);
        }
        return result;
    }

    /**
     * Add an error to the validation result
     */
    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    /**
     * Add a warning to the validation result
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if there are any warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
