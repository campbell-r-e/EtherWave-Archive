package com.hamradio.logbook.controller;

import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Station management
 */
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class StationController {

    private final StationRepository stationRepository;

    @GetMapping
    public ResponseEntity<List<Station>> getAllStations() {
        return ResponseEntity.ok(stationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Station> getStation(@PathVariable Long id) {
        return stationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Station> createStation(@RequestBody Station station) {
        Station saved = stationRepository.save(station);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Station> updateStation(@PathVariable Long id, @RequestBody Station station) {
        if (!stationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        station.setId(id);
        Station updated = stationRepository.save(station);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        if (!stationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        stationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
