package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.MapCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for MapCluster entity
 */
@Repository
public interface MapClusterRepository extends JpaRepository<MapCluster, Long> {

    /**
     * Find clusters for a specific log, zoom level, and filter hash
     */
    @Query("SELECT mc FROM MapCluster mc WHERE mc.log.id = :logId AND mc.zoomLevel = :zoomLevel AND mc.filterHash = :filterHash")
    List<MapCluster> findByLogIdAndZoomAndFilterHash(
            @Param("logId") Long logId,
            @Param("zoomLevel") Integer zoomLevel,
            @Param("filterHash") String filterHash
    );

    /**
     * Find all clusters for a specific log and zoom level (any filter)
     */
    @Query("SELECT mc FROM MapCluster mc WHERE mc.log.id = :logId AND mc.zoomLevel = :zoomLevel")
    List<MapCluster> findByLogIdAndZoom(
            @Param("logId") Long logId,
            @Param("zoomLevel") Integer zoomLevel
    );

    /**
     * Delete clusters for a specific log and zoom level
     */
    @Modifying
    @Query("DELETE FROM MapCluster mc WHERE mc.log.id = :logId AND mc.zoomLevel = :zoomLevel")
    void deleteByLogIdAndZoom(@Param("logId") Long logId, @Param("zoomLevel") Integer zoomLevel);

    /**
     * Delete all clusters for a specific log
     */
    @Modifying
    @Query("DELETE FROM MapCluster mc WHERE mc.log.id = :logId")
    void deleteByLogId(@Param("logId") Long logId);

    /**
     * Delete clusters for specific log and filter hash (any zoom level)
     */
    @Modifying
    @Query("DELETE FROM MapCluster mc WHERE mc.log.id = :logId AND mc.filterHash = :filterHash")
    void deleteByLogIdAndFilterHash(@Param("logId") Long logId, @Param("filterHash") String filterHash);
}
