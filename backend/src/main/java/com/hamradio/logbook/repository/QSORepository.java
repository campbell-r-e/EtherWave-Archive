package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.entity.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for QSO entity
 */
@Repository
public interface QSORepository extends JpaRepository<QSO, Long> {

    /**
     * Find all QSOs for a specific station
     */
    Page<QSO> findByStation(Station station, Pageable pageable);

    /**
     * Find all QSOs for a specific contest
     */
    Page<QSO> findByContest(Contest contest, Pageable pageable);

    /**
     * Find QSOs by callsign
     */
    List<QSO> findByCallsign(String callsign);

    /**
     * Find QSOs by date range
     */
    @Query("SELECT q FROM QSO q WHERE q.qsoDate BETWEEN :startDate AND :endDate ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find QSOs for a specific station and date range
     */
    @Query("SELECT q FROM QSO q WHERE q.station = :station AND q.qsoDate BETWEEN :startDate AND :endDate ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByStationAndDateRange(@Param("station") Station station, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find recent QSOs (for live feed)
     */
    @Query("SELECT q FROM QSO q ORDER BY q.createdAt DESC")
    Page<QSO> findRecent(Pageable pageable);

    /**
     * Find QSOs by band
     */
    List<QSO> findByBand(String band);

    /**
     * Find QSOs by mode
     */
    List<QSO> findByMode(String mode);

    /**
     * Get distinct states contacted
     */
    @Query("SELECT DISTINCT q.state FROM QSO q WHERE q.state IS NOT NULL AND q.country = 'USA'")
    List<String> findDistinctStates();

    /**
     * Get distinct countries contacted
     */
    @Query("SELECT DISTINCT q.country FROM QSO q WHERE q.country IS NOT NULL")
    List<String> findDistinctCountries();

    /**
     * Count QSOs for a specific contest
     */
    long countByContest(Contest contest);

    /**
     * Count QSOs for a specific station
     */
    long countByStation(Station station);

    /**
     * Find duplicate QSOs (same callsign, band, mode within same contest)
     */
    @Query("SELECT q FROM QSO q WHERE q.callsign = :callsign AND q.band = :band AND q.mode = :mode AND q.contest = :contest")
    List<QSO> findDuplicates(@Param("callsign") String callsign, @Param("band") String band, @Param("mode") String mode, @Param("contest") Contest contest);

    // ===========================
    // Log-aware query methods for multi-user support
    // ===========================

    /**
     * Find all QSOs for a specific log with pagination
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId ORDER BY q.createdAt DESC")
    Page<QSO> findByLogId(@Param("logId") Long logId, Pageable pageable);

    /**
     * Find all QSOs for a specific log (without pagination, for exports)
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findAllByLogId(@Param("logId") Long logId);

    /**
     * Find recent QSOs for a specific log
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId ORDER BY q.createdAt DESC")
    List<QSO> findRecentByLogId(@Param("logId") Long logId, Pageable pageable);

    /**
     * Find QSOs by log ID and date range
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId AND q.qsoDate BETWEEN :startDate AND :endDate ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByLogIdAndDateRange(@Param("logId") Long logId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get distinct states contacted for a specific log
     */
    @Query("SELECT DISTINCT q.state FROM QSO q WHERE q.log.id = :logId AND q.state IS NOT NULL AND q.country = 'USA'")
    List<String> findDistinctStatesByLogId(@Param("logId") Long logId);

    /**
     * Count QSOs for a specific log
     */
    long countByLogId(Long logId);

    /**
     * Count QSOs by log, station, and duplicate status
     * Used for GOTA bonus calculation
     */
    @Query("SELECT COUNT(q) FROM QSO q WHERE q.log.id = :logId AND q.station.id = :stationId AND q.isDuplicate = :isDuplicate")
    long countByLogIdAndStationIdAndIsDuplicate(
            @Param("logId") Long logId,
            @Param("stationId") Long stationId,
            @Param("isDuplicate") Boolean isDuplicate
    );

    /**
     * Find QSOs by log, callsign, band, and date range
     * Used for duplicate detection
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId AND q.callsign = :callsign AND q.band = :band " +
           "AND q.qsoDate BETWEEN :startDate AND :endDate ORDER BY q.qsoDate, q.timeOn")
    List<QSO> findByLogIdAndCallsignAndBandAndDateRange(
            @Param("logId") Long logId,
            @Param("callsign") String callsign,
            @Param("band") String band,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find QSOs by log, callsign, and date range (no band filter)
     * Used for duplicate detection when band is not a factor
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId AND q.callsign = :callsign " +
           "AND q.qsoDate BETWEEN :startDate AND :endDate ORDER BY q.qsoDate, q.timeOn")
    List<QSO> findByLogIdAndCallsignAndDateRange(
            @Param("logId") Long logId,
            @Param("callsign") String callsign,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find QSOs by log and station (for GOTA filtering)
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId AND q.station.id = :stationId ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByLogIdAndStationId(
            @Param("logId") Long logId,
            @Param("stationId") Long stationId
    );

    /**
     * Find QSOs by log and station with duplicate filter
     */
    @Query("SELECT q FROM QSO q WHERE q.log.id = :logId AND q.station.id = :stationId AND q.isDuplicate = :isDuplicate " +
           "ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByLogIdAndStationIdAndIsDuplicateList(
            @Param("logId") Long logId,
            @Param("stationId") Long stationId,
            @Param("isDuplicate") Boolean isDuplicate
    );

    // ===========================
    // Multi-station contest queries
    // ===========================

    /**
     * Find all QSOs for a specific log (entity-based)
     */
    @Query("SELECT q FROM QSO q WHERE q.log = :log ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByLog(@Param("log") com.hamradio.logbook.entity.Log log);

    /**
     * Find QSOs by log and station number
     */
    @Query("SELECT q FROM QSO q WHERE q.log = :log AND q.stationNumber = :stationNumber ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByLogAndStationNumber(
            @Param("log") com.hamradio.logbook.entity.Log log,
            @Param("stationNumber") Integer stationNumber
    );

    /**
     * Find QSOs by log and GOTA status
     */
    @Query("SELECT q FROM QSO q WHERE q.log = :log AND q.isGota = :isGota ORDER BY q.qsoDate DESC, q.timeOn DESC")
    List<QSO> findByLogAndIsGota(
            @Param("log") com.hamradio.logbook.entity.Log log,
            @Param("isGota") Boolean isGota
    );

    // ===========================
    // Award tracking queries
    // ===========================

    /**
     * Get distinct countries contacted for a specific log (DXCC award)
     */
    @Query("SELECT DISTINCT q.country FROM QSO q WHERE q.log.id = :logId AND q.country IS NOT NULL AND q.country <> ''")
    List<String> findDistinctCountriesByLogId(@Param("logId") Long logId);

    /**
     * Get distinct countries confirmed (LoTW or QSL) for a specific log
     */
    @Query("SELECT DISTINCT q.country FROM QSO q WHERE q.log.id = :logId AND q.country IS NOT NULL AND q.country <> '' " +
           "AND (q.lotwRcvd = 'Y' OR q.qslRcvd = 'Y')")
    List<String> findDistinctConfirmedCountriesByLogId(@Param("logId") Long logId);

    /**
     * Get distinct US states contacted for a specific log (WAS award)
     */
    @Query("SELECT DISTINCT q.state FROM QSO q WHERE q.log.id = :logId AND q.state IS NOT NULL AND q.state <> '' " +
           "AND q.country = 'USA'")
    List<String> findDistinctStatesByLogIdForWAS(@Param("logId") Long logId);

    /**
     * Get distinct US states confirmed (LoTW or QSL) for a specific log
     */
    @Query("SELECT DISTINCT q.state FROM QSO q WHERE q.log.id = :logId AND q.state IS NOT NULL AND q.state <> '' " +
           "AND q.country = 'USA' AND (q.lotwRcvd = 'Y' OR q.qslRcvd = 'Y')")
    List<String> findDistinctConfirmedStatesByLogId(@Param("logId") Long logId);

    /**
     * Get distinct 4-character grid squares contacted for a specific log (VUCC award)
     * Uses SUBSTRING to extract the first 4 characters (e.g. "EM72" from "EM72ab")
     */
    @Query("SELECT DISTINCT SUBSTRING(q.gridSquare, 1, 4) FROM QSO q WHERE q.log.id = :logId " +
           "AND q.gridSquare IS NOT NULL AND LENGTH(q.gridSquare) >= 4")
    List<String> findDistinctGrid4ByLogId(@Param("logId") Long logId);

    /**
     * Get distinct 4-character grid squares confirmed for a specific log
     */
    @Query("SELECT DISTINCT SUBSTRING(q.gridSquare, 1, 4) FROM QSO q WHERE q.log.id = :logId " +
           "AND q.gridSquare IS NOT NULL AND LENGTH(q.gridSquare) >= 4 " +
           "AND (q.lotwRcvd = 'Y' OR q.qslRcvd = 'Y')")
    List<String> findDistinctConfirmedGrid4ByLogId(@Param("logId") Long logId);
}
