package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.dto.TelemetryRequest;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.RigTelemetryRepository;
import com.hamradio.logbook.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("Telemetry API Integration Tests")
class TelemetryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private RigTelemetryRepository telemetryRepository;

    private Station testStation;

    @BeforeEach
    void setUp() {
        telemetryRepository.deleteAll();
        stationRepository.deleteAll();

        // Create test station
        testStation = new Station();
        testStation.setStationName("Test Rig Station");
        testStation.setCallsign("W1RIG");
        testStation = stationRepository.save(testStation);
    }

    @Test
    @DisplayName("Should receive telemetry successfully")
    void shouldReceiveTelemetry() throws Exception {
        TelemetryRequest request = new TelemetryRequest();
        request.setStationId(testStation.getId());
        request.setFrequencyKhz(14250L);
        request.setMode("SSB");
        request.setPttActive(false);
        request.setSMeter(7);
        request.setSwr(1.5);

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should fail with non-existent station")
    void shouldFailWithNonExistentStation() throws Exception {
        TelemetryRequest request = new TelemetryRequest();
        request.setStationId(99999L);
        request.setFrequencyKhz(14250L);
        request.setMode("SSB");

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should receive telemetry without optional fields")
    void shouldReceiveTelemetryWithoutOptionals() throws Exception {
        TelemetryRequest request = new TelemetryRequest();
        request.setStationId(testStation.getId());
        request.setFrequencyKhz(7125L);
        request.setMode("CW");
        request.setPttActive(true);

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
