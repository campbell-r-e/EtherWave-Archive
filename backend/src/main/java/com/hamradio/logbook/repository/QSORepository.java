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
}
