package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.DXCCPrefix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DXCCPrefix entity
 */
@Repository
public interface DXCCPrefixRepository extends JpaRepository<DXCCPrefix, Long> {

    /**
     * Find prefix by exact match
     */
    Optional<DXCCPrefix> findByPrefix(String prefix);

    /**
     * Find all prefixes for a DXCC code
     */
    @Query("SELECT dp FROM DXCCPrefix dp WHERE dp.dxccCode = :dxccCode")
    List<DXCCPrefix> findByDxccCode(@Param("dxccCode") Integer dxccCode);

    /**
     * Find all prefixes for a continent
     */
    @Query("SELECT dp FROM DXCCPrefix dp WHERE dp.continent = :continent ORDER BY dp.prefix")
    List<DXCCPrefix> findByContinent(@Param("continent") String continent);

    /**
     * Find prefixes that match a callsign (starts with)
     * Ordered by prefix length descending for longest match
     */
    @Query("SELECT dp FROM DXCCPrefix dp WHERE :callsign LIKE CONCAT(dp.prefix, '%') ORDER BY LENGTH(dp.prefix) DESC")
    List<DXCCPrefix> findMatchingPrefixes(@Param("callsign") String callsign);

    /**
     * Find exact match prefixes for a callsign
     */
    @Query("SELECT dp FROM DXCCPrefix dp WHERE dp.exactMatch = true AND dp.prefix = :callsign")
    Optional<DXCCPrefix> findExactMatch(@Param("callsign") String callsign);

    /**
     * Count total prefixes in database
     */
    long count();

    /**
     * Check if prefix database is populated
     */
    @Query("SELECT COUNT(dp) > 0 FROM DXCCPrefix dp")
    boolean exists();
}
