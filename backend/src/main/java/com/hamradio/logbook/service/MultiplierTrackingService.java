package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.LogMultiplier;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.LogMultiplierRepository;
import com.hamradio.logbook.repository.QSORepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for tracking multipliers in contest logs
 * Handles different multiplier types: states, DXCC, sections, grids, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiplierTrackingService {

    private final LogMultiplierRepository logMultiplierRepository;
    private final QSORepository qsoRepository;
    private final ObjectMapper objectMapper;

    /**
     * Check if a QSO is a new multiplier
     * @param qso The QSO to check
     * @return true if new multiplier, false otherwise
     */
    public boolean isNewMultiplier(QSO qso) {
        if (qso.getContest() == null) {
            // For non-contest logs, check state multiplier
            return isNewStateMultiplier(qso);
        }

        return isNewContestMultiplier(qso, qso.getContest());
    }

    /**
     * Check if state is a new multiplier (for non-contest logs)
     */
    private boolean isNewStateMultiplier(QSO qso) {
        if (qso.getState() == null || qso.getState().isEmpty()) {
            return false;
        }

        Optional<LogMultiplier> existing = logMultiplierRepository
                .findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                        qso.getLog().getId(),
                        "STATE",
                        qso.getState().toUpperCase(),
                        null // All-band multiplier
                );

        return existing.isEmpty();
    }

    /**
     * Check if QSO is a new multiplier based on contest rules
     */
    private boolean isNewContestMultiplier(QSO qso, Contest contest) {
        try {
            JsonNode rulesConfig = objectMapper.readTree(contest.getRulesConfig());
            JsonNode multipliersConfig = rulesConfig.get("multipliers");

            if (multipliersConfig == null) {
                return isNewStateMultiplier(qso);
            }

            String multType = multipliersConfig.get("type").asText();
            boolean perBand = multipliersConfig.has("per_band") &&
                              multipliersConfig.get("per_band").asBoolean();

            String band = perBand ? qso.getBand() : null;

            switch (multType.toLowerCase()) {
                case "arrl_section":
                    return checkArrlSectionMultiplier(qso, band);
                case "state":
                    return checkStateMultiplier(qso, band);
                case "dxcc":
                    return checkDxccMultiplier(qso, band);
                case "grid":
                    return checkGridMultiplier(qso, band);
                case "cq_zone":
                    return checkCqZoneMultiplier(qso, band);
                case "itu_zone":
                    return checkItuZoneMultiplier(qso, band);
                default:
                    return false;
            }

        } catch (Exception e) {
            log.error("Error checking multiplier for QSO {}: {}", qso.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Check ARRL section multiplier (for Field Day)
     */
    private boolean checkArrlSectionMultiplier(QSO qso, String band) {
        try {
            if (qso.getContestData() == null) {
                return false;
            }

            JsonNode contestData = objectMapper.readTree(qso.getContestData());
            if (!contestData.has("section")) {
                return false;
            }

            String section = contestData.get("section").asText().toUpperCase();

            Optional<LogMultiplier> existing = logMultiplierRepository
                    .findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                            qso.getLog().getId(),
                            "ARRL_SECT",
                            section,
                            band
                    );

            return existing.isEmpty();

        } catch (Exception e) {
            log.error("Error checking ARRL section multiplier: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check state multiplier
     */
    private boolean checkStateMultiplier(QSO qso, String band) {
        if (qso.getState() == null || qso.getState().isEmpty()) {
            return false;
        }

        Optional<LogMultiplier> existing = logMultiplierRepository
                .findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                        qso.getLog().getId(),
                        "STATE",
                        qso.getState().toUpperCase(),
                        band
                );

        return existing.isEmpty();
    }

    /**
     * Check DXCC multiplier
     */
    private boolean checkDxccMultiplier(QSO qso, String band) {
        if (qso.getDxcc() == null) {
            return false;
        }

        Optional<LogMultiplier> existing = logMultiplierRepository
                .findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                        qso.getLog().getId(),
                        "DXCC",
                        qso.getDxcc().toString(),
                        band
                );

        return existing.isEmpty();
    }

    /**
     * Check grid square multiplier
     */
    private boolean checkGridMultiplier(QSO qso, String band) {
        if (qso.getGridSquare() == null || qso.getGridSquare().isEmpty()) {
            return false;
        }

        // Use first 4 characters for grid multiplier
        String gridSquare = qso.getGridSquare().substring(0, Math.min(4, qso.getGridSquare().length()))
                .toUpperCase();

        Optional<LogMultiplier> existing = logMultiplierRepository
                .findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                        qso.getLog().getId(),
                        "GRID",
                        gridSquare,
                        band
                );

        return existing.isEmpty();
    }

    /**
     * Check CQ Zone multiplier
     */
    private boolean checkCqZoneMultiplier(QSO qso, String band) {
        if (qso.getCqZone() == null) {
            return false;
        }

        Optional<LogMultiplier> existing = logMultiplierRepository
                .findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                        qso.getLog().getId(),
                        "CQ_ZONE",
                        qso.getCqZone().toString(),
                        band
                );

        return existing.isEmpty();
    }

    /**
     * Check ITU Zone multiplier
     */
    private boolean checkItuZoneMultiplier(QSO qso, String band) {
        if (qso.getItuZone() == null) {
            return false;
        }

        Optional<LogMultiplier> existing = logMultiplierRepository
                .findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                        qso.getLog().getId(),
                        "ITU_ZONE",
                        qso.getItuZone().toString(),
                        band
                );

        return existing.isEmpty();
    }

    /**
     * Update multipliers for a QSO (add new multipliers)
     * @param qso The QSO to process
     */
    @Transactional
    public void updateMultipliersForQso(QSO qso) {
        if (qso.getContest() == null) {
            updateDefaultMultipliers(qso);
            return;
        }

        updateContestMultipliers(qso, qso.getContest());
    }

    /**
     * Update default multipliers (state) for non-contest QSO
     */
    private void updateDefaultMultipliers(QSO qso) {
        if (qso.getState() != null && !qso.getState().isEmpty() &&
            isNewStateMultiplier(qso)) {

            LogMultiplier mult = LogMultiplier.builder()
                    .log(qso.getLog())
                    .multiplierType("STATE")
                    .multiplierValue(qso.getState().toUpperCase())
                    .band(null)
                    .firstQso(qso)
                    .workedDate(LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn()))
                    .build();

            logMultiplierRepository.save(mult);

            // Mark QSO as multiplier
            qso.setIsMultiplier(true);
            qso.setMultiplierTypes("[\"STATE\"]");
            qsoRepository.save(qso);

            log.debug("Added STATE multiplier: {} for log {}", qso.getState(), qso.getLog().getId());
        }
    }

    /**
     * Update contest-specific multipliers
     */
    private void updateContestMultipliers(QSO qso, Contest contest) {
        try {
            JsonNode rulesConfig = objectMapper.readTree(contest.getRulesConfig());
            JsonNode multipliersConfig = rulesConfig.get("multipliers");

            if (multipliersConfig == null) {
                updateDefaultMultipliers(qso);
                return;
            }

            String multType = multipliersConfig.get("type").asText();
            boolean perBand = multipliersConfig.has("per_band") &&
                              multipliersConfig.get("per_band").asBoolean();

            String band = perBand ? qso.getBand() : null;
            List<String> newMultTypes = new ArrayList<>();

            // Add multipliers based on type
            switch (multType.toLowerCase()) {
                case "arrl_section":
                    if (addArrlSectionMultiplier(qso, band)) {
                        newMultTypes.add("ARRL_SECT");
                    }
                    break;
                case "state":
                    if (addStateMultiplier(qso, band)) {
                        newMultTypes.add("STATE");
                    }
                    break;
                case "dxcc":
                    if (addDxccMultiplier(qso, band)) {
                        newMultTypes.add("DXCC");
                    }
                    break;
                case "grid":
                    if (addGridMultiplier(qso, band)) {
                        newMultTypes.add("GRID");
                    }
                    break;
            }

            // Update QSO multiplier flags
            if (!newMultTypes.isEmpty()) {
                qso.setIsMultiplier(true);
                qso.setMultiplierTypes(objectMapper.writeValueAsString(newMultTypes));
                qsoRepository.save(qso);
            }

        } catch (Exception e) {
            log.error("Error updating contest multipliers for QSO {}: {}", qso.getId(), e.getMessage());
        }
    }

    /**
     * Add ARRL section multiplier
     */
    private boolean addArrlSectionMultiplier(QSO qso, String band) {
        try {
            if (qso.getContestData() == null) {
                return false;
            }

            JsonNode contestData = objectMapper.readTree(qso.getContestData());
            if (!contestData.has("section")) {
                return false;
            }

            String section = contestData.get("section").asText().toUpperCase();

            if (checkArrlSectionMultiplier(qso, band)) {
                LogMultiplier mult = LogMultiplier.builder()
                        .log(qso.getLog())
                        .multiplierType("ARRL_SECT")
                        .multiplierValue(section)
                        .band(band)
                        .firstQso(qso)
                        .workedDate(LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn()))
                        .build();

                logMultiplierRepository.save(mult);
                log.debug("Added ARRL_SECT multiplier: {} for log {}", section, qso.getLog().getId());
                return true;
            }

        } catch (Exception e) {
            log.error("Error adding ARRL section multiplier: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Add state multiplier
     */
    private boolean addStateMultiplier(QSO qso, String band) {
        if (qso.getState() != null && !qso.getState().isEmpty() &&
            checkStateMultiplier(qso, band)) {

            LogMultiplier mult = LogMultiplier.builder()
                    .log(qso.getLog())
                    .multiplierType("STATE")
                    .multiplierValue(qso.getState().toUpperCase())
                    .band(band)
                    .firstQso(qso)
                    .workedDate(LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn()))
                    .build();

            logMultiplierRepository.save(mult);
            log.debug("Added STATE multiplier: {} for log {}", qso.getState(), qso.getLog().getId());
            return true;
        }
        return false;
    }

    /**
     * Add DXCC multiplier
     */
    private boolean addDxccMultiplier(QSO qso, String band) {
        if (qso.getDxcc() != null && checkDxccMultiplier(qso, band)) {

            LogMultiplier mult = LogMultiplier.builder()
                    .log(qso.getLog())
                    .multiplierType("DXCC")
                    .multiplierValue(qso.getDxcc().toString())
                    .band(band)
                    .firstQso(qso)
                    .workedDate(LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn()))
                    .build();

            logMultiplierRepository.save(mult);
            log.debug("Added DXCC multiplier: {} for log {}", qso.getDxcc(), qso.getLog().getId());
            return true;
        }
        return false;
    }

    /**
     * Add grid multiplier
     */
    private boolean addGridMultiplier(QSO qso, String band) {
        if (qso.getGridSquare() != null && !qso.getGridSquare().isEmpty() &&
            checkGridMultiplier(qso, band)) {

            String gridSquare = qso.getGridSquare().substring(0, Math.min(4, qso.getGridSquare().length()))
                    .toUpperCase();

            LogMultiplier mult = LogMultiplier.builder()
                    .log(qso.getLog())
                    .multiplierType("GRID")
                    .multiplierValue(gridSquare)
                    .band(band)
                    .firstQso(qso)
                    .workedDate(LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn()))
                    .build();

            logMultiplierRepository.save(mult);
            log.debug("Added GRID multiplier: {} for log {}", gridSquare, qso.getLog().getId());
            return true;
        }
        return false;
    }

    /**
     * Recalculate all multipliers for a log
     * Clears existing multipliers and rebuilds from scratch
     * @param logId The log ID
     */
    @Transactional
    public void recalculateMultipliers(Long logId) {
        log.info("Recalculating multipliers for log {}", logId);

        // Delete existing multipliers
        logMultiplierRepository.deleteByLogId(logId);

        // Get all QSOs sorted chronologically
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);
        qsos.sort((q1, q2) -> {
            int dateCompare = q1.getQsoDate().compareTo(q2.getQsoDate());
            if (dateCompare != 0) return dateCompare;
            return q1.getTimeOn().compareTo(q2.getTimeOn());
        });

        // Process each QSO
        int multCount = 0;
        for (QSO qso : qsos) {
            // Skip duplicates
            if (qso.getIsDuplicate()) {
                qso.setIsMultiplier(false);
                qso.setMultiplierTypes(null);
                continue;
            }

            // Check and add multipliers
            boolean wasNewMult = isNewMultiplier(qso);
            if (wasNewMult) {
                updateMultipliersForQso(qso);
                multCount++;
            } else {
                qso.setIsMultiplier(false);
                qso.setMultiplierTypes(null);
            }
        }

        qsoRepository.saveAll(qsos);

        log.info("Recalculated {} multipliers for log {}", multCount, logId);
    }
}
