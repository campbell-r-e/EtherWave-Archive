package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.MaidenheadGrid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MaidenheadGrid entity
 */
@Repository
public interface MaidenheadGridRepository extends JpaRepository<MaidenheadGrid, Long> {

    /**
     * Find grid by grid square and log ID
     */
    @Query("SELECT mg FROM MaidenheadGrid mg WHERE mg.grid = :grid AND mg.log.id = :logId AND mg.precision = :precision")
    Optional<MaidenheadGrid> findByGridAndLogIdAndPrecision(
            @Param("grid") String grid,
            @Param("logId") Long logId,
            @Param("precision") Integer precision
    );

    /**
     * Find all grids for a specific log
     */
    @Query("SELECT mg FROM MaidenheadGrid mg WHERE mg.log.id = :logId ORDER BY mg.grid")
    List<MaidenheadGrid> findByLogId(@Param("logId") Long logId);

    /**
     * Find all grids for a specific log and precision
     */
    @Query("SELECT mg FROM MaidenheadGrid mg WHERE mg.log.id = :logId AND mg.precision = :precision ORDER BY mg.grid")
    List<MaidenheadGrid> findByLogIdAndPrecision(@Param("logId") Long logId, @Param("precision") Integer precision);

    /**
     * Count grids for a specific log
     */
    @Query("SELECT COUNT(mg) FROM MaidenheadGrid mg WHERE mg.log.id = :logId")
    long countByLogId(@Param("logId") Long logId);

    /**
     * Delete all grids for a specific log
     */
    @Query("DELETE FROM MaidenheadGrid mg WHERE mg.log.id = :logId")
    void deleteByLogId(@Param("logId") Long logId);
}
