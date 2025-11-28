package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.repository.ContestRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Service to initialize database with default contest configurations
 * Loads contest configs from JSON files on application startup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService {

    private final ContestRepository contestRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    @Transactional
    public void initializeContests() {
        log.info("Initializing contest configurations...");

        try {
            Resource[] resources = ResourcePatternUtils
                    .getResourcePatternResolver(resourceLoader)
                    .getResources("classpath:contest-configs/*.json");

            for (Resource resource : resources) {
                try {
                    loadContestConfig(resource);
                } catch (Exception e) {
                    log.error("Failed to load contest config from {}: {}", resource.getFilename(), e.getMessage());
                }
            }

            log.info("Contest initialization complete. Total contests: {}", contestRepository.count());
        } catch (IOException e) {
            log.error("Failed to load contest configurations", e);
        }
    }

    private void loadContestConfig(Resource resource) throws IOException {
        JsonNode config = objectMapper.readTree(resource.getInputStream());

        String contestCode = config.get("contestCode").asText();

        // Check if contest already exists
        if (contestRepository.findByContestCode(contestCode).isPresent()) {
            log.debug("Contest {} already exists, skipping", contestCode);
            return;
        }

        Contest contest = Contest.builder()
                .contestCode(contestCode)
                .contestName(config.get("contestName").asText())
                .description(config.has("description") ? config.get("description").asText() : null)
                .validatorClass(config.has("validatorClass") ? config.get("validatorClass").asText() : null)
                .isActive(config.has("isActive") ? config.get("isActive").asBoolean() : true)
                .rulesConfig(config.has("rulesConfig") ? config.get("rulesConfig").toString() : null)
                .build();

        contestRepository.save(contest);
        log.info("Loaded contest configuration: {} - {}", contestCode, contest.getContestName());
    }
}
