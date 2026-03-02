package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.LogMultiplierRepository;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core scoring engine for contest QSO scoring
 * Handles point calculation, duplicate detection, and final score computation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScoringService {

    private final QSORepository qsoRepository;
    private final LogRepository logRepository;
    private final LogMultiplierRepository logMultiplierRepository;
    private final StationRepository stationRepository;
    private final ObjectMapper objectMapper;
    private final DuplicateDetectionService duplicateDetectionService;
    private final MultiplierTrackingService multiplierTrackingService;

    /**
     * Calculate points for a single QSO based on contest rules
     * @param qso The QSO to score
     * @return Point value (0 if duplicate or invalid)
     */
    public int calculateQsoPoints(QSO qso) {
        // Invalid QSOs get 0 points
        if (qso.getIsValid() != null && !qso.getIsValid()) {
            return 0;
        }

        // Duplicates get 0 points
        if (qso.getIsDuplicate() != null && qso.getIsDuplicate()) {
            return 0;
        }

        // If no contest, use default scoring
        if (qso.getContest() == null) {
            return calculateDefaultPoints(qso);
        }

        // Contest-specific scoring
        return calculateContestPoints(qso, qso.getContest());
    }

    /**
     * Default scoring for non-contest QSOs
     */
    private int calculateDefaultPoints(QSO qso) {
        String mode = qso.getMode().toUpperCase();

        // CW and digital modes: 2 points
        if (mode.contains("CW") || mode.contains("RTTY") ||
            mode.contains("FT8") || mode.contains("FT4") ||
            mode.contains("PSK") || mode.contains("JT")) {
            return 2;
        }

        // Phone modes: 1 point
        return 1;
    }

    /**
     * Contest-specific scoring based on rules config
     */
    private int calculateContestPoints(QSO qso, Contest contest) {
        try {
            JsonNode rulesConfig = objectMapper.readTree(contest.getRulesConfig());
            JsonNode scoring = rulesConfig.get("scoring");

            if (scoring == null) {
                return calculateDefaultPoints(qso);
            }

            String mode = qso.getMode().toUpperCase();

            // Check for CW
            if (mode.contains("CW") && scoring.has("cw")) {
                return scoring.get("cw").asInt();
            }

            // Check for digital
            if ((mode.contains("RTTY") || mode.contains("FT8") || mode.contains("FT4") ||
                 mode.contains("PSK") || mode.contains("JT")) && scoring.has("digital")) {
                return scoring.get("digital").asInt();
            }

            // Check for phone
            if (scoring.has("phone")) {
                return scoring.get("phone").asInt();
            }

            return 1; // Default fallback

        } catch (Exception e) {
            log.error("Error calculating contest points for QSO {}: {}", qso.getId(), e.getMessage());
            return calculateDefaultPoints(qso);
        }
    }

    /**
     * Calculate GOTA bonus points for Field Day
     * @param logId The log ID
     * @return Total GOTA bonus (100 for having GOTA + 5 per GOTA QSO)
     */
    public int calculateGotaBonus(Long logId) {
        try {
            // Count valid GOTA QSOs (non-duplicates)
            List<QSO> allQsos = qsoRepository.findAllByLogId(logId);
            long gotaQsoCount = allQsos.stream()
                    .filter(q -> Boolean.TRUE.equals(q.getIsGota()))
                    .filter(q -> !Boolean.TRUE.equals(q.getIsDuplicate()))
                    .count();

            // 100 points for having GOTA station + 5 points per GOTA QSO
            return gotaQsoCount > 0 ? 100 + ((int)gotaQsoCount * 5) : 0;

        } catch (Exception e) {
            log.error("Error calculating GOTA bonus for log {}: {}", logId, e.getMessage());
            return 0;
        }
    }

    /**
     * Recalculate all scores for a log
     * This includes: QSO points, duplicates, multipliers, and final score
     * @param logId The log ID
     */
    @Transactional
    public void recalculateLogScores(Long logId) {
        log.info("Starting score recalculation for log {}", logId);

        Log targetLog = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found: " + logId));

        // Step 1: Mark duplicates
        duplicateDetectionService.markDuplicates(logId);

        // Step 2: Recalculate multipliers
        multiplierTrackingService.recalculateMultipliers(logId);

        // Step 3: Calculate points for each QSO
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);
        int totalPoints = 0;          // Main contest points (GOTA excluded)
        int validQsoCount = 0;        // Main contest QSO count (GOTA excluded)
        int gotaPoints = 0;           // GOTA points (tracked separately)
        int gotaQsoCount = 0;         // GOTA QSO count (tracked separately)

        for (QSO qso : qsos) {
            int points = calculateQsoPoints(qso);
            qso.setPoints(points);
            qsoRepository.save(qso);

            boolean isGota = Boolean.TRUE.equals(qso.getIsGota());
            boolean isDupe = Boolean.TRUE.equals(qso.getIsDuplicate());

            if (!isDupe) {
                if (isGota) {
                    // GOTA QSOs: track separately, not counted in main score
                    gotaPoints += points;
                    gotaQsoCount++;
                } else {
                    // Main contest QSOs: count toward contest score
                    totalPoints += points;
                    validQsoCount++;
                }
            }
        }

        log.debug("Scoring breakdown for log {}: Main QSOs={}, Main Points={}, GOTA QSOs={}, GOTA Points={}",
                logId, validQsoCount, totalPoints, gotaQsoCount, gotaPoints);

        // Step 4: Calculate final score with power multiplier and bonuses
        int finalScore = calculateFinalScore(targetLog, totalPoints);

        // Step 5: Update log aggregate fields
        int totalMultipliers = (int) logMultiplierRepository.countByLogId(logId);

        targetLog.setTotalQsos(validQsoCount);
        targetLog.setTotalPoints(totalPoints);
        targetLog.setTotalMultipliers(totalMultipliers);
        targetLog.setCalculatedScore(finalScore);
        targetLog.setLastScoreCalculation(LocalDateTime.now());

        logRepository.save(targetLog);

        log.info("Score recalculation complete for log {}. QSOs: {}, Points: {}, Mults: {}, Final Score: {}",
                logId, validQsoCount, totalPoints, totalMultipliers, finalScore);
    }

    /**
     * Calculate final score with power multiplier and bonus points
     */
    private int calculateFinalScore(Log targetLog, int basePoints) {
        if (targetLog.getContest() == null) {
            return basePoints;
        }

        try {
            JsonNode rulesConfig = objectMapper.readTree(targetLog.getContest().getRulesConfig());

            // Apply power multiplier for Field Day
            int powerMultiplier = determinePowerMultiplier(targetLog.getId(), rulesConfig);
            int scoreWithMultiplier = basePoints * powerMultiplier;

            // Add bonus points
            int bonusPoints = calculateBonusPoints(targetLog.getId(), rulesConfig);

            // Add GOTA bonus
            int gotaBonus = calculateGotaBonus(targetLog.getId());

            return scoreWithMultiplier + bonusPoints + gotaBonus;

        } catch (Exception e) {
            log.error("Error calculating final score for log {}: {}", targetLog.getId(), e.getMessage());
            return basePoints;
        }
    }

    /**
     * Determine power multiplier based on highest power used
     * Field Day: QRP (≤5W) = 5x, Low (≤150W) = 2x, High (≤1500W) = 1x
     */
    private int determinePowerMultiplier(Long logId, JsonNode rulesConfig) {
        try {
            JsonNode powerMultipliers = rulesConfig.get("power_multipliers");
            if (powerMultipliers == null) {
                return 1;
            }

            // Find highest power used in log
            List<QSO> qsos = qsoRepository.findAllByLogId(logId);
            int maxPower = qsos.stream()
                    .filter(q -> q.getPowerWatts() != null)
                    .mapToInt(QSO::getPowerWatts)
                    .max()
                    .orElse(150); // Default to low power

            // Determine multiplier
            if (maxPower <= 5 && powerMultipliers.has("qrp")) {
                return powerMultipliers.get("qrp").asInt();
            } else if (maxPower <= 150 && powerMultipliers.has("low")) {
                return powerMultipliers.get("low").asInt();
            } else if (powerMultipliers.has("high")) {
                return powerMultipliers.get("high").asInt();
            }

            return 1;

        } catch (Exception e) {
            log.error("Error determining power multiplier: {}", e.getMessage());
            return 1;
        }
    }

    /**
     * Calculate bonus points from log's bonus metadata against contest config rules.
     * The log stores claimed bonuses as a JSON map (bonus_key -> count).
     * The contest config stores point values per bonus key under "bonus_points".
     * Example: {"100pct_emergency_power": 1, "youth_participation": 3}
     * with config {"100pct_emergency_power": 100, "youth_participation": 20}
     * yields 100 + 60 = 160 bonus points.
     */
    private int calculateBonusPoints(Long logId, JsonNode rulesConfig) {
        try {
            JsonNode bonusConfig = rulesConfig.get("bonus_points");
            if (bonusConfig == null) {
                return 0;
            }

            Log targetLog = logRepository.findById(logId).orElse(null);
            if (targetLog == null || targetLog.getBonusMetadata() == null || targetLog.getBonusMetadata().isBlank()) {
                return 0;
            }

            JsonNode claimed = objectMapper.readTree(targetLog.getBonusMetadata());
            int total = 0;

            var fields = claimed.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                int claimedCount = entry.getValue().asInt(0);
                if (claimedCount > 0 && bonusConfig.has(entry.getKey())) {
                    total += bonusConfig.get(entry.getKey()).asInt(0) * claimedCount;
                }
            }

            log.debug("Bonus points for log {}: {}", logId, total);
            return total;

        } catch (Exception e) {
            log.error("Error calculating bonus points for log {}: {}", logId, e.getMessage());
            return 0;
        }
    }

    /**
     * Quick score update for single QSO (used when creating/editing QSOs)
     * More efficient than full recalculation
     */
    @Transactional
    public void updateScoreForQso(QSO qso) {
        log.debug("Updating score for QSO {}", qso.getId());

        // Check if duplicate
        boolean isDupe = duplicateDetectionService.isDuplicate(qso);
        qso.setIsDuplicate(isDupe);

        // Calculate points
        int points = calculateQsoPoints(qso);
        qso.setPoints(points);

        // Check if new multiplier
        boolean isNewMult = multiplierTrackingService.isNewMultiplier(qso);
        qso.setIsMultiplier(isNewMult);

        // Update multipliers if new
        if (isNewMult && !isDupe) {
            multiplierTrackingService.updateMultipliersForQso(qso);
        }

        // Note: No need to save QSO explicitly - it's already managed by JPA
        // The changes will be persisted when the transaction commits

        // Update log aggregate (quick update, not full recalc)
        updateLogAggregates(qso.getLog().getId());
    }

    /**
     * Quick update of log aggregates without full recalculation
     */
    private void updateLogAggregates(Long logId) {
        Log targetLog = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found: " + logId));

        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        int validQsoCount = (int) qsos.stream()
                .filter(q -> !q.getIsDuplicate())
                .count();

        int totalPoints = qsos.stream()
                .filter(q -> !q.getIsDuplicate())
                .mapToInt(QSO::getPoints)
                .sum();

        int totalMultipliers = (int) logMultiplierRepository.countByLogId(logId);
        int finalScore = calculateFinalScore(targetLog, totalPoints);

        targetLog.setTotalQsos(validQsoCount);
        targetLog.setTotalPoints(totalPoints);
        targetLog.setTotalMultipliers(totalMultipliers);
        targetLog.setCalculatedScore(finalScore);
        targetLog.setLastScoreCalculation(LocalDateTime.now());

        logRepository.save(targetLog);
    }
}
