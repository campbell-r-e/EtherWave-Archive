package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.RigTelemetry;
import com.hamradio.logbook.repository.RigTelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TelemetryService {

    private final RigTelemetryRepository rigTelemetryRepository;

    @Autowired
    public TelemetryService(RigTelemetryRepository rigTelemetryRepository) {
        this.rigTelemetryRepository = rigTelemetryRepository;
    }

    public RigTelemetry saveTelemetry(RigTelemetry telemetry) {
        return rigTelemetryRepository.save(telemetry);
    }

    public Optional<RigTelemetry> getTelemetryById(Long id) {
        return rigTelemetryRepository.findById(id);
    }

    public List<RigTelemetry> getAllTelemetry() {
        return rigTelemetryRepository.findAll();
    }

    public void deleteTelemetry(Long id) {
        rigTelemetryRepository.deleteById(id);
    }

    public void deleteOldTelemetry(LocalDateTime before) {
        rigTelemetryRepository.deleteByTimestampBefore(before);
    }
}
