package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.QSOLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for QSOLocation entity
 */
@Repository
public interface QSOLocationRepository extends JpaRepository<QSOLocation, Long> {

    /**
     * Find QSO location by QSO ID
     */
    @Query("SELECT ql FROM QSOLocation ql WHERE ql.qso.id = :qsoId")
    Optional<QSOLocation> findByQsoId(@Param("qsoId") Long qsoId);

    /**
     * Find all QSO locations for a specific log
     */
    @Query("SELECT ql FROM QSOLocation ql WHERE ql.qso.log.id = :logId")
    List<QSOLocation> findByLogId(@Param("logId") Long logId);

    /**
     * Find QSO locations within bounding box for a specific log
     */
    @Query("SELECT ql FROM QSOLocation ql WHERE ql.qso.log.id = :logId " +
           "AND ql.contactLat BETWEEN :minLat AND :maxLat " +
           "AND ql.contactLon BETWEEN :minLon AND :maxLon")
    List<QSOLocation> findByLogIdAndBounds(
            @Param("logId") Long logId,
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon
    );

    /**
     * Find QSO locations by DXCC entity for a specific log
     */
    @Query("SELECT ql FROM QSOLocation ql WHERE ql.qso.log.id = :logId AND ql.contactDxcc = :dxcc")
    List<QSOLocation> findByLogIdAndDxcc(@Param("logId") Long logId, @Param("dxcc") String dxcc);

    /**
     * Find QSO locations by continent for a specific log
     */
    @Query("SELECT ql FROM QSOLocation ql WHERE ql.qso.log.id = :logId AND ql.contactContinent = :continent")
    List<QSOLocation> findByLogIdAndContinent(@Param("logId") Long logId, @Param("continent") String continent);

    /**
     * Count QSO locations for a specific log
     */
    @Query("SELECT COUNT(ql) FROM QSOLocation ql WHERE ql.qso.log.id = :logId")
    long countByLogId(@Param("logId") Long logId);

    /**
     * Delete QSO location by QSO ID
     */
    @Query("DELETE FROM QSOLocation ql WHERE ql.qso.id = :qsoId")
    void deleteByQsoId(@Param("qsoId") Long qsoId);
}
