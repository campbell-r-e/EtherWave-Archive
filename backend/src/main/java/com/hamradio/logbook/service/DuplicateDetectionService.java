package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.QSORepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for detecting duplicate QSOs based on contest rules
 * Different contests have different duplicate criteria
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final QSORepository qsoRepository;
    private final ObjectMapper objectMapper;

    /**
     * Check if a QSO is a duplicate
     * @param qso The QSO to check
     * @return true if duplicate, false otherwise
     */
    public boolean isDuplicate(QSO qso) {
        if (qso.getContest() == null) {
            return checkDefaultDuplicate(qso);
        }

        return checkContestDuplicate(qso, qso.getContest());
    }

    /**
     * Default duplicate check for non-contest QSOs
     * Duplicate if same callsign + band within 24 hours
     */
    private boolean checkDefaultDuplicate(QSO qso) {
        LocalDateTime qsoTime = LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn());
        LocalDateTime windowStart = qsoTime.minusHours(24);
        LocalDateTime windowEnd = qsoTime.plusHours(24);

        List<QSO> existing = qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                qso.getLog().getId(),
                qso.getCallsign(),
                qso.getBand(),
                windowStart.toLocalDate(),
                windowEnd.toLocalDate()
        );

        // Check if any existing QSO matches (excluding self)
        return existing.stream()
                .filter(q -> q != null) // Filter out null QSOs
                .filter(q -> !q.getId().equals(qso.getId()))
                .anyMatch(q -> {
                    LocalDateTime existingTime = LocalDateTime.of(q.getQsoDate(), q.getTimeOn());
                    return Math.abs(Duration.between(qsoTime, existingTime).toHours()) <= 24;
                });
    }

    /**
     * Contest-specific duplicate check based on rules
     */
    private boolean checkContestDuplicate(QSO qso, Contest contest) {
        try {
            JsonNode rulesConfig = objectMapper.readTree(contest.getRulesConfig());
            JsonNode dupeConfig = rulesConfig.get("duplicate_window");

            String dupeWindow = dupeConfig != null ? dupeConfig.asText() : "24_hours";
            JsonNode dupeFields = rulesConfig.get("duplicate_fields");

            // Determine duplicate criteria
            boolean checkMode = dupeFields != null && dupeFields.toString().contains("mode");
            boolean checkBand = dupeFields != null && dupeFields.toString().contains("band");

            // Get time window
            long hoursWindow = parseDuplicateWindow(dupeWindow);

            // Field Day and similar: callsign + band + mode within 24 hours
            if (checkMode && checkBand) {
                return checkDuplicateWithMode(qso, hoursWindow);
            }

            // Band-specific duplicates
            if (checkBand) {
                return checkDuplicateWithBand(qso, hoursWindow);
            }

            // Callsign only (rare, but supported)
            return checkDuplicateCallsignOnly(qso, hoursWindow);

        } catch (Exception e) {
            log.error("Error checking contest duplicate for QSO {}: {}", qso.getId(), e.getMessage());
            return checkDefaultDuplicate(qso);
        }
    }

    /**
     * Parse duplicate window configuration
     */
    private long parseDuplicateWindow(String window) {
        switch (window.toLowerCase()) {
            case "24_hours":
                return 24;
            case "48_hours":
                return 48;
            case "contest_duration":
                return 9999; // Effectively no time limit
            default:
                return 24;
        }
    }

    /**
     * Check duplicate with mode (e.g., Field Day)
     */
    private boolean checkDuplicateWithMode(QSO qso, long hoursWindow) {
        LocalDateTime qsoTime = LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn());
        LocalDateTime windowStart = qsoTime.minusHours(hoursWindow);
        LocalDateTime windowEnd = qsoTime.plusHours(hoursWindow);

        List<QSO> existing = qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                qso.getLog().getId(),
                qso.getCallsign(),
                qso.getBand(),
                windowStart.toLocalDate(),
                windowEnd.toLocalDate()
        );

        return existing.stream()
                .filter(q -> q != null) // Filter out null QSOs
                .filter(q -> !q.getId().equals(qso.getId()))
                .filter(q -> q.getMode().equalsIgnoreCase(qso.getMode()))
                .anyMatch(q -> {
                    LocalDateTime existingTime = LocalDateTime.of(q.getQsoDate(), q.getTimeOn());
                    return Math.abs(Duration.between(qsoTime, existingTime).toHours()) <= hoursWindow;
                });
    }

    /**
     * Check duplicate with band only
     */
    private boolean checkDuplicateWithBand(QSO qso, long hoursWindow) {
        LocalDateTime qsoTime = LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn());
        LocalDateTime windowStart = qsoTime.minusHours(hoursWindow);
        LocalDateTime windowEnd = qsoTime.plusHours(hoursWindow);

        List<QSO> existing = qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                qso.getLog().getId(),
                qso.getCallsign(),
                qso.getBand(),
                windowStart.toLocalDate(),
                windowEnd.toLocalDate()
        );

        return existing.stream()
                .filter(q -> q != null) // Filter out null QSOs
                .filter(q -> !q.getId().equals(qso.getId()))
                .anyMatch(q -> {
                    LocalDateTime existingTime = LocalDateTime.of(q.getQsoDate(), q.getTimeOn());
                    return Math.abs(Duration.between(qsoTime, existingTime).toHours()) <= hoursWindow;
                });
    }

    /**
     * Check duplicate by callsign only (no band/mode)
     */
    private boolean checkDuplicateCallsignOnly(QSO qso, long hoursWindow) {
        LocalDateTime qsoTime = LocalDateTime.of(qso.getQsoDate(), qso.getTimeOn());
        LocalDateTime windowStart = qsoTime.minusHours(hoursWindow);
        LocalDateTime windowEnd = qsoTime.plusHours(hoursWindow);

        List<QSO> existing = qsoRepository.findByLogIdAndCallsignAndDateRange(
                qso.getLog().getId(),
                qso.getCallsign(),
                windowStart.toLocalDate(),
                windowEnd.toLocalDate()
        );

        return existing.stream()
                .filter(q -> q != null) // Filter out null QSOs
                .filter(q -> !q.getId().equals(qso.getId()))
                .anyMatch(q -> {
                    LocalDateTime existingTime = LocalDateTime.of(q.getQsoDate(), q.getTimeOn());
                    return Math.abs(Duration.between(qsoTime, existingTime).toHours()) <= hoursWindow;
                });
    }

    /**
     * Mark all duplicates in a log
     * Used during bulk recalculation
     * @param logId The log ID
     * @return Number of duplicates marked
     */
    @Transactional
    public int markDuplicates(Long logId) {
        log.info("Marking duplicates for log {}", logId);

        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        // Sort by date/time to process chronologically
        qsos.sort((q1, q2) -> {
            int dateCompare = q1.getQsoDate().compareTo(q2.getQsoDate());
            if (dateCompare != 0) return dateCompare;
            return q1.getTimeOn().compareTo(q2.getTimeOn());
        });

        int dupeCount = 0;

        // First pass: clear all duplicate flags
        for (QSO qso : qsos) {
            qso.setIsDuplicate(false);
        }

        // Second pass: mark duplicates (first contact wins)
        for (QSO qso : qsos) {
            boolean isDupe = isDuplicate(qso);
            if (isDupe) {
                qso.setIsDuplicate(true);
                dupeCount++;
            }
        }

        // Save all
        qsoRepository.saveAll(qsos);

        log.info("Marked {} duplicates in log {}", dupeCount, logId);
        return dupeCount;
    }
}
