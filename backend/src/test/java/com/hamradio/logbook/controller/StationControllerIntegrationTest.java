package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Station;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false) // No auth required for this controller
@ActiveProfiles("test")
@Transactional
@DisplayName("Station API Integration Tests")
class StationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StationRepository stationRepository;

    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create station successfully")
    void shouldCreateStation() throws Exception {
        Station station = new Station();
        station.setStationName("Home Station");
        station.setCallsign("W1TEST");
        station.setGridSquare("FN31pr");

        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stationName").value("Home Station"))
                .andExpect(jsonPath("$.callsign").value("W1TEST"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should get all stations")
    void shouldGetAllStations() throws Exception {
        // Create test stations
        for (int i = 1; i <= 3; i++) {
            Station station = new Station();
            station.setStationName("Station " + i);
            station.setCallsign("W" + i + "TEST");
            stationRepository.save(station);
        }

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Should get station by ID")
    void shouldGetStationById() throws Exception {
        Station station = new Station();
        station.setStationName("Test Station");
        station.setCallsign("W2TEST");
        Station saved = stationRepository.save(station);

        mockMvc.perform(get("/api/stations/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationName").value("Test Station"))
                .andExpect(jsonPath("$.callsign").value("W2TEST"));
    }

    @Test
    @DisplayName("Should update station successfully")
    void shouldUpdateStation() throws Exception {
        Station station = new Station();
        station.setStationName("Old Name");
        station.setCallsign("W3TEST");
        Station saved = stationRepository.save(station);

        Station updated = new Station();
        updated.setStationName("New Name");
        updated.setCallsign("W3TEST");

        mockMvc.perform(put("/api/stations/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationName").value("New Name"));
    }

    @Test
    @DisplayName("Should delete station successfully")
    void shouldDeleteStation() throws Exception {
        Station station = new Station();
        station.setStationName("To Delete");
        station.setCallsign("W4TEST");
        Station saved = stationRepository.save(station);

        mockMvc.perform(delete("/api/stations/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stations/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent station")
    void shouldReturn404ForNonExistentStation() throws Exception {
        mockMvc.perform(get("/api/stations/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent station")
    void shouldReturn404WhenUpdatingNonExistentStation() throws Exception {
        Station station = new Station();
        station.setStationName("Test");
        station.setCallsign("W5TEST");

        mockMvc.perform(put("/api/stations/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(station)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent station")
    void shouldReturn404WhenDeletingNonExistentStation() throws Exception {
        mockMvc.perform(delete("/api/stations/99999"))
                .andExpect(status().isNotFound());
    }
}
