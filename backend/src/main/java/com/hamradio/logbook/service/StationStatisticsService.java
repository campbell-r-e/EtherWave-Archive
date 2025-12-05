package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.QSOResponse;
import com.hamradio.logbook.dto.StationStatistics;
import com.hamradio.logbook.dto.StationStatsSummary;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.LogParticipantRepository;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.QSORepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating multi-station contest statistics
 * Provides per-station metrics, leaderboards, and GOTA separation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StationStatisticsService {

    private final QSORepository qsoRepository;
    private final LogRepository logRepository;
    private final LogParticipantRepository logParticipantRepository;
    private final QSOService qsoService;

    /**
     * Get statistics for a specific station
     */
    @Transactional(readOnly = true)
    public StationStatistics getStationStats(Long logId, Integer stationNumber) {
        Log log = getLogOrThrow(logId);
        List<QSO> qsos = qsoRepository.findByLogAndStationNumber(log, stationNumber);

        return buildStationStatistics(log, qsos, stationNumber, false);
    }

    /**
     * Get statistics for GOTA station
     */
    @Transactional(readOnly = true)
    public StationStatistics getGotaStats(Long logId) {
        Log log = getLogOrThrow(logId);
        List<QSO> qsos = qsoRepository.findByLogAndIsGota(log, true);

        return buildStationStatistics(log, qsos, null, true);
    }

    /**
     * Get statistics for all stations including GOTA
     */
    @Transactional(readOnly = true)
    public List<StationStatistics> getAllStationStats(Long logId) {
        Log log = getLogOrThrow(logId);
        List<QSO> allQsos = qsoRepository.findByLog(log);

        // Group QSOs by station
        Map<Integer, List<QSO>> mainStations = allQsos.stream()
                .filter(q -> q.getStationNumber() != null && !q.getIsGota())
                .collect(Collectors.groupingBy(QSO::getStationNumber));

        List<QSO> gotaQsos = allQsos.stream()
                .filter(q -> q.getIsGota() != null && q.getIsGota())
                .collect(Collectors.toList());

        List<StationStatistics> stats = new ArrayList<>();

        // Build stats for each main station
        for (Map.Entry<Integer, List<QSO>> entry : mainStations.entrySet()) {
            stats.add(buildStationStatistics(log, entry.getValue(), entry.getKey(), false));
        }

        // Sort by station number
        stats.sort(Comparator.comparing(StationStatistics::getStationNumber));

        // Add GOTA if exists
        if (!gotaQsos.isEmpty()) {
            stats.add(buildStationStatistics(log, gotaQsos, null, true));
        }

        // Calculate ranks for main stations only (not GOTA)
        List<StationStatistics> mainStationStats = stats.stream()
                .filter(s -> !s.getIsGota())
                .sorted(Comparator.comparing(StationStatistics::getPoints).reversed())
                .collect(Collectors.toList());

        for (int i = 0; i < mainStationStats.size(); i++) {
            mainStationStats.get(i).setRank(i + 1);
        }

        return stats;
    }

    /**
     * Get comprehensive summary with main/GOTA separation
     */
    @Transactional(readOnly = true)
    public StationStatsSummary getStationSummary(Long logId) {
        List<StationStatistics> allStats = getAllStationStats(logId);

        List<StationStatistics> mainStations = allStats.stream()
                .filter(s -> !s.getIsGota())
                .collect(Collectors.toList());

        StationStatistics gota = allStats.stream()
                .filter(StationStatistics::getIsGota)
                .findFirst()
                .orElse(null);

        // Calculate main total (GOTA excluded for contest scoring)
        int mainQsoCount = mainStations.stream().mapToInt(StationStatistics::getQsoCount).sum();
        int mainPoints = mainStations.stream().mapToInt(StationStatistics::getPoints).sum();
        double mainRate = mainStations.stream().mapToDouble(StationStatistics::getQsoRate).sum();

        StationStatsSummary.MainTotal mainTotal = StationStatsSummary.MainTotal.builder()
                .qsoCount(mainQsoCount)
                .points(mainPoints)
                .qsoRate(mainRate)
                .build();

        // Calculate overall total (including GOTA)
        int overallQsoCount = mainQsoCount + (gota != null ? gota.getQsoCount() : 0);

        StationStatsSummary.OverallTotal overallTotal = StationStatsSummary.OverallTotal.builder()
                .qsoCount(overallQsoCount)
                .build();

        return StationStatsSummary.builder()
                .mainStations(mainStations)
                .gota(gota)
                .mainTotal(mainTotal)
                .overallTotal(overallTotal)
                .build();
    }

    /**
     * Build statistics for a specific set of QSOs
     */
    private StationStatistics buildStationStatistics(Log log, List<QSO> qsos, Integer stationNumber, boolean isGota) {
        // Calculate basic counts
        int qsoCount = qsos.size();
        int points = qsos.stream().mapToInt(q -> q.getPoints() != null ? q.getPoints() : 0).sum();

        // Calculate QSO rate (QSOs per hour)
        double qsoRate = calculateRate(qsos);

        // Band breakdown
        Map<String, Integer> bandBreakdown = qsos.stream()
                .filter(q -> q.getBand() != null)
                .collect(Collectors.groupingBy(
                        QSO::getBand,
                        Collectors.summingInt(q -> 1)
                ));

        // Mode breakdown
        Map<String, Integer> modeBreakdown = qsos.stream()
                .filter(q -> q.getMode() != null)
                .collect(Collectors.groupingBy(
                        QSO::getMode,
                        Collectors.summingInt(q -> 1)
                ));

        // Recent QSOs (last 10)
        List<QSOResponse> recentQSOs = qsos.stream()
                .sorted(Comparator.comparing(QSO::getCreatedAt).reversed())
                .limit(10)
                .map(this::toQSOResponse)
                .collect(Collectors.toList());

        // Get operators for this station
        List<String> operators = getOperatorsForStation(log, stationNumber, isGota);

        // Build station label
        String stationLabel;
        if (isGota) {
            stationLabel = "GOTA";
        } else if (stationNumber != null) {
            stationLabel = "Station " + stationNumber;
        } else {
            stationLabel = "Unassigned";
        }

        return StationStatistics.builder()
                .stationNumber(stationNumber)
                .isGota(isGota)
                .stationLabel(stationLabel)
                .qsoCount(qsoCount)
                .points(points)
                .qsoRate(qsoRate)
                .bandBreakdown(bandBreakdown)
                .modeBreakdown(modeBreakdown)
                .recentQSOs(recentQSOs)
                .operators(operators)
                .build();
    }

    /**
     * Calculate QSO rate in contacts per hour
     */
    private double calculateRate(List<QSO> qsos) {
        if (qsos.isEmpty()) {
            return 0.0;
        }

        // Get time span
        Optional<LocalDateTime> earliest = qsos.stream()
                .map(QSO::getCreatedAt)
                .min(LocalDateTime::compareTo);

        Optional<LocalDateTime> latest = qsos.stream()
                .map(QSO::getCreatedAt)
                .max(LocalDateTime::compareTo);

        if (earliest.isEmpty() || latest.isEmpty()) {
            return 0.0;
        }

        Duration duration = Duration.between(earliest.get(), latest.get());
        double hours = duration.toMinutes() / 60.0;

        // Avoid division by zero, minimum 1 minute
        if (hours < 0.017) { // less than 1 minute
            hours = 0.017;
        }

        return qsos.size() / hours;
    }

    /**
     * Get list of operators assigned to a station
     */
    private List<String> getOperatorsForStation(Log log, Integer stationNumber, boolean isGota) {
        List<LogParticipant> participants = logParticipantRepository.findByLogAndActive(log, true);

        return participants.stream()
                .filter(p -> {
                    if (isGota) {
                        return Boolean.TRUE.equals(p.getIsGota());
                    } else if (stationNumber != null) {
                        return stationNumber.equals(p.getStationNumber());
                    }
                    return false;
                })
                .map(p -> p.getUser().getUsername())
                .collect(Collectors.toList());
    }

    /**
     * Convert QSO entity to response DTO (simplified version)
     */
    private QSOResponse toQSOResponse(QSO qso) {
        return QSOResponse.builder()
                .id(qso.getId())
                .callsign(qso.getCallsign())
                .frequencyKhz(qso.getFrequencyKhz())
                .mode(qso.getMode())
                .band(qso.getBand())
                .qsoDate(qso.getQsoDate())
                .timeOn(qso.getTimeOn())
                .rstSent(qso.getRstSent())
                .rstRcvd(qso.getRstRcvd())
                .stationNumber(qso.getStationNumber())
                .isGota(qso.getIsGota())
                .createdAt(qso.getCreatedAt())
                .build();
    }

    /**
     * Helper: Get log by ID or throw exception
     */
    private Log getLogOrThrow(Long logId) {
        return logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
    }
}
