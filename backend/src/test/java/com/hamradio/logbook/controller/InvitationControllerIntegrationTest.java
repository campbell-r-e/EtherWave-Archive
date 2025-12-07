package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.dto.auth.LoginRequest;
import com.hamradio.logbook.dto.log.InvitationRequest;
import com.hamradio.logbook.dto.log.LogRequest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Invitation API Integration Tests")
class InvitationControllerIntegrationTest {

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
    private InvitationRepository invitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User inviteeUser;
    private String authToken;
    private String inviteeToken;
    private Long logId;

    @BeforeEach
    void setUp() throws Exception {
        invitationRepository.deleteAll();
        logParticipantRepository.deleteAll();
        logRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        testUser = new User();
        testUser.setUsername("inviter");
        testUser.setEmail("inviter@example.com");
        testUser.setCallsign("W1INV");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.addRole(User.Role.ROLE_USER);
        testUser = userRepository.save(testUser);

        inviteeUser = new User();
        inviteeUser.setUsername("invitee");
        inviteeUser.setEmail("invitee@example.com");
        inviteeUser.setCallsign("W2INV");
        inviteeUser.setPassword(passwordEncoder.encode("password123"));
        inviteeUser.addRole(User.Role.ROLE_USER);
        inviteeUser = userRepository.save(inviteeUser);

        // Get tokens
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("inviter");
        loginRequest.setPassword("password123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = objectMapper.readTree(response).get("token").asText();

        loginRequest.setUsernameOrEmail("invitee");
        response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        inviteeToken = objectMapper.readTree(response).get("token").asText();

        // Create a shared log
        LogRequest logRequest = new LogRequest();
        logRequest.setName("Shared Log");
        logRequest.setType(Log.LogType.SHARED);

        response = mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        logId = objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    @DisplayName("Should create invitation successfully")
    void shouldCreateInvitation() throws Exception {
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        mockMvc.perform(post("/api/invitations")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should get pending invitations")
    void shouldGetPendingInvitations() throws Exception {
        // Create invitation
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        mockMvc.perform(post("/api/invitations")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Get pending invitations for invitee
        mockMvc.perform(get("/api/invitations/pending")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should get sent invitations")
    void shouldGetSentInvitations() throws Exception {
        // Create invitation
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        mockMvc.perform(post("/api/invitations")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Get sent invitations
        mockMvc.perform(get("/api/invitations/sent")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should get invitations for log")
    void shouldGetInvitationsForLog() throws Exception {
        // Create invitation
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        mockMvc.perform(post("/api/invitations")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Get invitations for log
        mockMvc.perform(get("/api/invitations/log/" + logId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should accept invitation successfully")
    void shouldAcceptInvitation() throws Exception {
        // Create invitation
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        String createResponse = mockMvc.perform(post("/api/invitations")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long invitationId = objectMapper.readTree(createResponse).get("id").asLong();

        // Accept invitation
        mockMvc.perform(post("/api/invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should decline invitation successfully")
    void shouldDeclineInvitation() throws Exception {
        // Create invitation
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        String createResponse = mockMvc.perform(post("/api/invitations")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long invitationId = objectMapper.readTree(createResponse).get("id").asLong();

        // Decline invitation
        mockMvc.perform(post("/api/invitations/" + invitationId + "/decline")
                        .header("Authorization", "Bearer " + inviteeToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should cancel invitation successfully")
    void shouldCancelInvitation() throws Exception {
        // Create invitation
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        String createResponse = mockMvc.perform(post("/api/invitations")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long invitationId = objectMapper.readTree(createResponse).get("id").asLong();

        // Cancel invitation
        mockMvc.perform(post("/api/invitations/" + invitationId + "/cancel")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should fail to create invitation without authentication")
    void shouldFailToCreateInvitationWithoutAuth() throws Exception {
        InvitationRequest request = new InvitationRequest();
        request.setLogId(logId);
        request.setInviteeUsername("invitee");
        request.setProposedRole(LogParticipant.ParticipantRole.STATION);

        mockMvc.perform(post("/api/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
