package com.hamradio.logbook.validation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for contest validators
 * Automatically discovers and registers all ContestValidator beans
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContestValidatorRegistry {

    private final List<ContestValidator> validators;
    private final Map<String, ContestValidator> validatorMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ContestValidator validator : validators) {
            String contestCode = validator.getContestCode();
            validatorMap.put(contestCode, validator);
            log.info("Registered contest validator: {} -> {}", contestCode, validator.getClass().getSimpleName());
        }
        log.info("Total contest validators registered: {}", validatorMap.size());
    }

    /**
     * Get validator for a specific contest code
     */
    public Optional<ContestValidator> getValidator(String contestCode) {
        return Optional.ofNullable(validatorMap.get(contestCode));
    }

    /**
     * Check if a validator exists for the given contest code
     */
    public boolean hasValidator(String contestCode) {
        return validatorMap.containsKey(contestCode);
    }

    /**
     * Get all registered validators
     */
    public Map<String, ContestValidator> getAllValidators() {
        return new HashMap<>(validatorMap);
    }

    /**
     * Get count of registered validators
     */
    public int getValidatorCount() {
        return validatorMap.size();
    }
}
