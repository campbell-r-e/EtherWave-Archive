package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.RigTelemetry;
import com.hamradio.logbook.entity.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RigTelemetry entity
 */
@Repository
public interface RigTelemetryRepository extends JpaRepository<RigTelemetry, Long> {

    /**
     * Find telemetry data for a specific station
     */
    Page<RigTelemetry> findByStation(Station station, Pageable pageable);

    /**
     * Find latest telemetry for a station
     */
    Optional<RigTelemetry> findFirstByStationOrderByTimestampDesc(Station station);

    /**
     * Find telemetry data within a time range
     */
    @Query("SELECT r FROM RigTelemetry r WHERE r.station = :station AND r.timestamp BETWEEN :startTime AND :endTime ORDER BY r.timestamp DESC")
    List<RigTelemetry> findByStationAndTimeRange(@Param("station") Station station, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Delete old telemetry data (cleanup)
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
