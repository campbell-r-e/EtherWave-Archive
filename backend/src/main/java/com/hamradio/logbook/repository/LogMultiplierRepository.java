package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.LogMultiplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogMultiplierRepository extends JpaRepository<LogMultiplier, Long> {

    /**
     * Find all multipliers for a specific log
     */
    List<LogMultiplier> findByLogId(Long logId);

    /**
     * Find all multipliers of a specific type for a log
     * Example: All states worked (multiplierType = "STATE")
     */
    List<LogMultiplier> findByLogIdAndMultiplierType(Long logId, String multiplierType);

    /**
     * Find all multipliers for a specific log and band
     * Used for per-band multiplier contests
     */
    List<LogMultiplier> findByLogIdAndBand(Long logId, String band);

    /**
     * Check if a specific multiplier has been worked
     */
    Optional<LogMultiplier> findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
            Long logId, String multiplierType, String multiplierValue, String band);

    /**
     * Count total multipliers for a log
     */
    long countByLogId(Long logId);

    /**
     * Count multipliers by type for a log
     */
    long countByLogIdAndMultiplierType(Long logId, String multiplierType);

    /**
     * Get distinct multiplier types for a log
     */
    @Query("SELECT DISTINCT lm.multiplierType FROM LogMultiplier lm WHERE lm.log.id = :logId")
    List<String> findDistinctMultiplierTypesByLogId(@Param("logId") Long logId);

    /**
     * Get distinct multiplier values for a log and type
     * Example: List of all states worked
     */
    @Query("SELECT DISTINCT lm.multiplierValue FROM LogMultiplier lm " +
           "WHERE lm.log.id = :logId AND lm.multiplierType = :type")
    List<String> findDistinctMultiplierValuesByLogIdAndType(
            @Param("logId") Long logId, @Param("type") String type);

    /**
     * Delete all multipliers for a log (used when recalculating)
     */
    void deleteByLogId(Long logId);

    /**
     * Delete all multipliers of a specific type for a log
     */
    void deleteByLogIdAndMultiplierType(Long logId, String multiplierType);
}
