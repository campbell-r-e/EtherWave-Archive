package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.dto.QSORequest;
import com.hamradio.logbook.dto.auth.LoginRequest;
import com.hamradio.logbook.dto.log.LogRequest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Export API Integration Tests")
class ExportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private LogParticipantRepository logParticipantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private Long logId;
    private Long stationId;

    @BeforeEach
    void setUp() throws Exception {
        logParticipantRepository.deleteAll();
        logRepository.deleteAll();
        stationRepository.deleteAll();
        userRepository.deleteAll();

        // Create user
        User user = new User();
        user.setUsername("exportuser");
        user.setEmail("export@example.com");
        user.setCallsign("W1EXP");
        user.setPassword(passwordEncoder.encode("password123"));
        user.addRole(User.Role.ROLE_USER);
        user = userRepository.save(user);

        // Create station
        Station station = new Station();
        station.setStationName("Export Station");
        station.setCallsign("W1EXP");
        station = stationRepository.save(station);
        stationId = station.getId();

        // Get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("exportuser");
        loginRequest.setPassword("password123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = objectMapper.readTree(response).get("token").asText();

        // Create log
        LogRequest logRequest = new LogRequest();
        logRequest.setName("Export Log");
        logRequest.setType(Log.LogType.PERSONAL);

        response = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        logId = objectMapper.readTree(response).get("id").asLong();

        // Create some QSOs
        for (int i = 1; i <= 3; i++) {
            QSORequest qsoRequest = new QSORequest();
            qsoRequest.setStationId(stationId);
            qsoRequest.setCallsign("W" + i + "TEST");
            qsoRequest.setFrequencyKhz(14250L);
            qsoRequest.setMode("SSB");
            qsoRequest.setQsoDate(LocalDate.now());
            qsoRequest.setTimeOn(LocalTime.now());
            qsoRequest.setRstSent("59");
            qsoRequest.setRstRcvd("59");
            qsoRequest.setBand("20M");

            mockMvc.perform(post("/api/qsos")
                    .header("Authorization", "Bearer " + authToken)
                    .param("logId", logId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(qsoRequest)));
        }
    }

    @Test
    @DisplayName("Should export log as ADIF")
    void shouldExportAsAdif() throws Exception {
        mockMvc.perform(get("/api/export/adif/log/" + logId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/plain")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ADIF")));
    }

    @Test
    @DisplayName("Should export log as Cabrillo")
    void shouldExportAsCabrillo() throws Exception {
        mockMvc.perform(get("/api/export/cabrillo/log/" + logId)
                        .header("Authorization", "Bearer " + authToken)
                        .param("callsign", "W1EXP"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should export all QSOs as ADIF (deprecated endpoint)")
    void shouldExportAllAsAdif() throws Exception {
        mockMvc.perform(get("/api/export/adif")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @DisplayName("Should fail to export without authentication")
    void shouldFailToExportWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/export/adif/log/" + logId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should export with date range filter")
    void shouldExportWithDateRange() throws Exception {
        String startDate = LocalDate.now().minusDays(7).toString();
        String endDate = LocalDate.now().toString();

        mockMvc.perform(get("/api/export/adif/range")
                        .header("Authorization", "Bearer " + authToken)
                        .param("startDate", startDate)
                        .param("endDate", endDate))
                .andExpect(status().isOk());
    }
}
