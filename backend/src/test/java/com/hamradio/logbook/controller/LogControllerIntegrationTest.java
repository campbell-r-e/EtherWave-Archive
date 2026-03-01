package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.dto.auth.LoginRequest;
import com.hamradio.logbook.dto.log.LogRequest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.LogParticipantRepository;
import com.hamradio.logbook.repository.LogRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Log API Integration Tests")
class LogControllerIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User secondUser;
    private String authToken;
    private String secondUserToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        logParticipantRepository.deleteAll();
        logRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("logtest");
        testUser.setCallsign("W1LOG");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.addRole(User.Role.ROLE_USER);
        testUser = userRepository.save(testUser);

        // Create second test user for multi-user tests
        secondUser = new User();
        secondUser.setUsername("logtest2");
        secondUser.setCallsign("W2LOG");
        secondUser.setPassword(passwordEncoder.encode("password123"));
        secondUser.addRole(User.Role.ROLE_USER);
        secondUser = userRepository.save(secondUser);

        // Get auth tokens
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("logtest");
        loginRequest.setPassword("password123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = objectMapper.readTree(response).get("token").asText();

        // Get second user token
        loginRequest.setUsername("logtest2");
        response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        secondUserToken = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    @DisplayName("Should create log successfully")
    void shouldCreateLog() throws Exception {
        LogRequest request = new LogRequest();
        request.setName("Field Day 2025");
        request.setDescription("Annual field day contest");
        request.setType(Log.LogType.SHARED);
        request.setIsPublic(false);

        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Field Day 2025"))
                .andExpect(jsonPath("$.description").value("Annual field day contest"))
                .andExpect(jsonPath("$.type").value("SHARED"))
                .andExpect(jsonPath("$.creatorUsername").value("logtest"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should fail to create log without authentication")
    void shouldFailToCreateLogWithoutAuth() throws Exception {
        LogRequest request = new LogRequest();
        request.setName("Test Log");
        request.setType(Log.LogType.PERSONAL);

        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should fail to create log with invalid data")
    void shouldFailToCreateLogWithInvalidData() throws Exception {
        LogRequest request = new LogRequest();
        // Missing required fields

        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should get all logs for current user")
    void shouldGetMyLogs() throws Exception {
        // Create multiple logs
        for (int i = 1; i <= 3; i++) {
            LogRequest request = new LogRequest();
            request.setName("Log " + i);
            request.setType(Log.LogType.PERSONAL);

            mockMvc.perform(post("/api/logs")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // Get all logs
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("Should get log by ID")
    void shouldGetLogById() throws Exception {
        // Create log
        LogRequest request = new LogRequest();
        request.setName("Test Log");
        request.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Get log by ID
        mockMvc.perform(get("/api/logs/" + logId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logId))
                .andExpect(jsonPath("$.name").value("Test Log"));
    }

    @Test
    @DisplayName("Should update log successfully")
    void shouldUpdateLog() throws Exception {
        // Create log
        LogRequest createRequest = new LogRequest();
        createRequest.setName("Original Name");
        createRequest.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Update log
        LogRequest updateRequest = new LogRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated description");
        updateRequest.setType(Log.LogType.PERSONAL);

        mockMvc.perform(put("/api/logs/" + logId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @DisplayName("Should delete log successfully")
    void shouldDeleteLog() throws Exception {
        // Create log
        LogRequest request = new LogRequest();
        request.setName("Test Log");
        request.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Delete log
        mockMvc.perform(delete("/api/logs/" + logId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should freeze log successfully")
    void shouldFreezeLog() throws Exception {
        // Create log
        LogRequest request = new LogRequest();
        request.setName("Test Log");
        request.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Freeze log
        mockMvc.perform(post("/api/logs/" + logId + "/freeze")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editable").value(false));
    }

    @Test
    @DisplayName("Should unfreeze log successfully")
    void shouldUnfreezeLog() throws Exception {
        // Create and freeze log
        LogRequest request = new LogRequest();
        request.setName("Test Log");
        request.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Freeze
        mockMvc.perform(post("/api/logs/" + logId + "/freeze")
                .header("Authorization", "Bearer " + authToken));

        // Unfreeze
        mockMvc.perform(post("/api/logs/" + logId + "/unfreeze")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editable").value(true));
    }

    @Test
    @DisplayName("Should get log participants")
    void shouldGetLogParticipants() throws Exception {
        // Create log
        LogRequest request = new LogRequest();
        request.setName("Multi-user Log");
        request.setType(Log.LogType.SHARED);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Get participants
        mockMvc.perform(get("/api/logs/" + logId + "/participants")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should fail to access another user's log without permission")
    void shouldFailToAccessOtherUsersLog() throws Exception {
        // Create log with first user
        LogRequest request = new LogRequest();
        request.setName("Private Log");
        request.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Try to access with second user
        mockMvc.perform(get("/api/logs/" + logId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should fail to update another user's log")
    void shouldFailToUpdateOtherUsersLog() throws Exception {
        // Create log with first user
        LogRequest createRequest = new LogRequest();
        createRequest.setName("Private Log");
        createRequest.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Try to update with second user
        LogRequest updateRequest = new LogRequest();
        updateRequest.setName("Hacked Name");
        updateRequest.setType(Log.LogType.PERSONAL);

        mockMvc.perform(put("/api/logs/" + logId)
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should fail to delete another user's log")
    void shouldFailToDeleteOtherUsersLog() throws Exception {
        // Create log with first user
        LogRequest request = new LogRequest();
        request.setName("Private Log");
        request.setType(Log.LogType.PERSONAL);

        String createResponse = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long logId = objectMapper.readTree(createResponse).get("id").asLong();

        // Try to delete with second user
        mockMvc.perform(delete("/api/logs/" + logId)
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should fail to get non-existent log")
    void shouldFailToGetNonExistentLog() throws Exception {
        mockMvc.perform(get("/api/logs/99999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}
