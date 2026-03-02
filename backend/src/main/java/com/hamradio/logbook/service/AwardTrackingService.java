package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Computes award-tracking progress for a given log.
 * Supports DXCC (countries worked/confirmed), WAS (US states), and VUCC (grid squares).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AwardTrackingService {

    private static final int WAS_TOTAL = 50;    // 50 US states
    private static final int DXCC_TOTAL = 340;  // approximate DXCC entities
    private static final int VUCC_THRESHOLD = 100; // grids needed for basic VUCC

    private final QSORepository qsoRepository;
    private final LogRepository logRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    // -----------------------------------------------------------------------
    // Response DTOs (inner records for clean serialization)
    // -----------------------------------------------------------------------

    public record DXCCProgress(
            List<String> workedCountries,
            List<String> confirmedCountries,
            int workedCount,
            int confirmedCount,
            int totalEntities
    ) {}

    public record WASProgress(
            List<String> workedStates,
            List<String> confirmedStates,
            int workedCount,
            int confirmedCount,
            int totalStates
    ) {}

    public record VUCCProgress(
            List<String> workedGrids,
            List<String> confirmedGrids,
            int workedCount,
            int confirmedCount,
            int threshold
    ) {}

    public record AwardProgress(
            Long logId,
            String logName,
            long totalQsos,
            DXCCProgress dxcc,
            WASProgress was,
            VUCCProgress vucc
    ) {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Compute award progress for the given log.
     * Throws SecurityException if the requesting user has no access.
     */
    public AwardProgress getProgress(Long logId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        Log log = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        if (!logService.hasAccess(log, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        List<String> workedCountries  = qsoRepository.findDistinctCountriesByLogId(logId);
        List<String> confirmedCountries = qsoRepository.findDistinctConfirmedCountriesByLogId(logId);

        List<String> workedStates    = qsoRepository.findDistinctStatesByLogIdForWAS(logId);
        List<String> confirmedStates = qsoRepository.findDistinctConfirmedStatesByLogId(logId);

        List<String> workedGrids    = qsoRepository.findDistinctGrid4ByLogId(logId);
        List<String> confirmedGrids = qsoRepository.findDistinctConfirmedGrid4ByLogId(logId);

        long totalQsos = qsoRepository.countByLogId(logId);

        return new AwardProgress(
                logId,
                log.getName(),
                totalQsos,
                new DXCCProgress(workedCountries, confirmedCountries,
                        workedCountries.size(), confirmedCountries.size(), DXCC_TOTAL),
                new WASProgress(workedStates, confirmedStates,
                        workedStates.size(), confirmedStates.size(), WAS_TOTAL),
                new VUCCProgress(workedGrids, confirmedGrids,
                        workedGrids.size(), confirmedGrids.size(), VUCC_THRESHOLD)
        );
    }
}
