package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.config.TestConfig;
import com.hamradio.logbook.dto.QSORequest;
import com.hamradio.logbook.dto.auth.LoginRequest;
import com.hamradio.logbook.dto.auth.RegisterRequest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.LogParticipantRepository;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.StationRepository;
import com.hamradio.logbook.repository.UserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("QSO API Integration Tests")
class QSOControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private LogParticipantRepository logParticipantRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Log testLog;
    private Station testStation;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        logParticipantRepository.deleteAll();
        logRepository.deleteAll();
        stationRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("qsotest");
        testUser.setEmail("qsotest@example.com");
        testUser.setCallsign("W1TEST");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.addRole(User.Role.ROLE_USER);
        testUser = userRepository.save(testUser);

        // Create test station
        testStation = new Station();
        testStation.setStationName("Test Station");
        testStation.setCallsign(testUser.getCallsign());
        testStation = stationRepository.save(testStation);

        // Create test log
        testLog = new Log();
        testLog.setName("Test Log");
        testLog.setDescription("Test logbook for QSO tests");
        testLog.setType(Log.LogType.PERSONAL);
        testLog.setCreator(testUser);
        testLog = logRepository.save(testLog);

        // Add user as participant
        LogParticipant participant = new LogParticipant();
        participant.setLog(testLog);
        participant.setUser(testUser);
        participant.setRole(LogParticipant.ParticipantRole.CREATOR);
        logParticipantRepository.save(participant);

        // Get auth token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("qsotest");
        loginRequest.setPassword("password123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    @DisplayName("Should create QSO successfully")
    void shouldCreateQSO() throws Exception {
        QSORequest request = createValidQSORequest();

        mockMvc.perform(post("/api/qsos")
                        .header("Authorization", "Bearer " + authToken)
                        .param("logId", testLog.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callsign").value("N0CALL"))
                .andExpect(jsonPath("$.frequencyKhz").value(14250))
                .andExpect(jsonPath("$.mode").value("SSB"))
                .andExpect(jsonPath("$.rstSent").value("59"))
                .andExpect(jsonPath("$.rstRcvd").value("59"));
    }

    @Test
    @DisplayName("Should fail to create QSO without authentication")
    void shouldFailToCreateQSOWithoutAuth() throws Exception {
        QSORequest request = createValidQSORequest();

        mockMvc.perform(post("/api/qsos")
                        .param("logId", testLog.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get QSO by ID")
    void shouldGetQSOById() throws Exception {
        // Create QSO first
        QSORequest request = createValidQSORequest();

        String createResponse = mockMvc.perform(post("/api/qsos")
                        .header("Authorization", "Bearer " + authToken)
                        .param("logId", testLog.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long qsoId = objectMapper.readTree(createResponse).get("id").asLong();

        // Get QSO
        mockMvc.perform(get("/api/qsos/" + qsoId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qsoId))
                .andExpect(jsonPath("$.callsign").value("N0CALL"));
    }

    @Test
    @DisplayName("Should update QSO successfully")
    void shouldUpdateQSO() throws Exception {
        // Create QSO first
        QSORequest createRequest = createValidQSORequest();

        String createResponse = mockMvc.perform(post("/api/qsos")
                        .header("Authorization", "Bearer " + authToken)
                        .param("logId", testLog.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long qsoId = objectMapper.readTree(createResponse).get("id").asLong();

        // Update QSO
        QSORequest updateRequest = createValidQSORequest();
        updateRequest.setCallsign("W2CALL");
        updateRequest.setFrequencyKhz(7125L); // 7.125 MHz = 7125 kHz

        mockMvc.perform(put("/api/qsos/" + qsoId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callsign").value("W2CALL"))
                .andExpect(jsonPath("$.frequencyKhz").value(7125));
    }

    @Test
    @DisplayName("Should delete QSO successfully")
    void shouldDeleteQSO() throws Exception {
        // Create QSO first
        QSORequest request = createValidQSORequest();

        String createResponse = mockMvc.perform(post("/api/qsos")
                        .header("Authorization", "Bearer " + authToken)
                        .param("logId", testLog.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long qsoId = objectMapper.readTree(createResponse).get("id").asLong();

        // Delete QSO
        mockMvc.perform(delete("/api/qsos/" + qsoId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/qsos/" + qsoId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should fail to create QSO with invalid data")
    void shouldFailToCreateQSOWithInvalidData() throws Exception {
        QSORequest request = new QSORequest();
        // Missing required fields

        mockMvc.perform(post("/api/qsos")
                        .header("Authorization", "Bearer " + authToken)
                        .param("logId", testLog.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get QSOs for a log with pagination")
    void shouldGetQSOsForLogWithPagination() throws Exception {
        // Create multiple QSOs
        for (int i = 0; i < 5; i++) {
            QSORequest request = createValidQSORequest();
            request.setCallsign("N" + i + "CALL");

            mockMvc.perform(post("/api/qsos")
                    .header("Authorization", "Bearer " + authToken)
                    .param("logId", testLog.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // Get paginated QSOs
        mockMvc.perform(get("/api/qsos")
                        .header("Authorization", "Bearer " + authToken)
                        .param("logId", testLog.getId().toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(5))));
    }

    private QSORequest createValidQSORequest() {
        QSORequest request = new QSORequest();
        request.setStationId(testStation.getId());
        request.setCallsign("N0CALL");
        request.setFrequencyKhz(14250L); // 14.250 MHz = 14250 kHz
        request.setMode("SSB");
        request.setQsoDate(LocalDate.now());
        request.setTimeOn(LocalTime.now());
        request.setRstSent("59");
        request.setRstRcvd("59");
        request.setBand("20M");
        return request;
    }
}
