package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Station entity
 */
@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    /**
     * Find station by station name
     */
    Optional<Station> findByStationName(String stationName);

    /**
     * Find all stations by callsign
     */
    List<Station> findByCallsign(String callsign);

    /**
     * Find all stations with rig control enabled
     */
    List<Station> findByRigControlEnabled(Boolean rigControlEnabled);
}
