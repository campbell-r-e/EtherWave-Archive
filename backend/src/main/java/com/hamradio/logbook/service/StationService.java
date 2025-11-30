package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.StationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StationService {

    private final StationRepository stationRepository;

    @Autowired
    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public Station createStation(Station station, User user) {
        // Validate unique station name
        if (stationRepository.findByStationName(station.getStationName()).isPresent()) {
            throw new IllegalArgumentException("Station name already exists");
        }

        return stationRepository.save(station);
    }

    public Optional<Station> getStationById(Long id) {
        return stationRepository.findById(id);
    }

    public Optional<Station> getStationByName(String stationName) {
        return stationRepository.findByStationName(stationName);
    }

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public List<Station> getStationsByCallsign(String callsign) {
        return stationRepository.findByCallsign(callsign);
    }

    public Station updateStation(Long id, Station stationUpdates) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));

        if (stationUpdates.getCallsign() != null) {
            station.setCallsign(stationUpdates.getCallsign());
        }
        if (stationUpdates.getLocation() != null) {
            station.setLocation(stationUpdates.getLocation());
        }
        if (stationUpdates.getGridSquare() != null) {
            station.setGridSquare(stationUpdates.getGridSquare());
        }
        if (stationUpdates.getAntenna() != null) {
            station.setAntenna(stationUpdates.getAntenna());
        }
        if (stationUpdates.getPowerWatts() != null) {
            station.setPowerWatts(stationUpdates.getPowerWatts());
        }
        if (stationUpdates.getRigModel() != null) {
            station.setRigModel(stationUpdates.getRigModel());
        }
        if (stationUpdates.getRigControlEnabled() != null) {
            station.setRigControlEnabled(stationUpdates.getRigControlEnabled());
        }
        if (stationUpdates.getRigControlHost() != null) {
            station.setRigControlHost(stationUpdates.getRigControlHost());
        }
        if (stationUpdates.getRigControlPort() != null) {
            station.setRigControlPort(stationUpdates.getRigControlPort());
        }

        return stationRepository.save(station);
    }

    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }
}
