package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.service.LogService;
import com.hamradio.logbook.testutil.JwtTestUtil;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hamradio.logbook.config.TestConfig;
import org.springframework.context.annotation.Import;

@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Log Controller Tests")
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LogService logService;

    private User testUser;
    private Log testLog;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser().id(1L).build();
        testLog = TestDataBuilder.aValidLog(testUser)
                .id(1L)
                .logName("Field Day 2025")
                .build();
        jwtToken = JwtTestUtil.generateToken("testuser");
    }

    // ==================== CREATE LOG TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/logs - Valid Log - Returns 201 Created")
    void createLog_validLog_returns201() throws Exception {
        // Arrange
        when(logService.createLog(any(Log.class), eq(1L))).thenReturn(testLog);

        // Act & Assert
        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLog)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.logName").value("Field Day 2025"));

        verify(logService).createLog(any(Log.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/logs - Missing Required Field - Returns 400 Bad Request")
    void createLog_missingRequiredField_returns400() throws Exception {
        // Arrange
        Log invalidLog = TestDataBuilder.aValidLog(testUser)
                .logName(null) // Missing required field
                .build();

        when(logService.createLog(any(Log.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Log name is required"));

        // Act & Assert
        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLog)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("required")));
    }

    @Test
    @DisplayName("POST /api/logs - Not Authenticated - Returns 401 Unauthorized")
    void createLog_notAuthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLog)))
                .andExpect(status().isUnauthorized());

        verify(logService, never()).createLog(any(), any());
    }

    // ==================== GET LOG TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/{id} - Valid ID - Returns 200 OK")
    void getLog_validId_returns200() throws Exception {
        // Arrange
        when(logService.getLogById(1L)).thenReturn(Optional.of(testLog));

        // Act & Assert
        mockMvc.perform(get("/api/logs/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.logName").value("Field Day 2025"));

        verify(logService).getLogById(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/{id} - Log Not Found - Returns 404 Not Found")
    void getLog_notFound_returns404() throws Exception {
        // Arrange
        when(logService.getLogById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/logs/999")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());

        verify(logService).getLogById(999L);
    }

    // ==================== GET LOGS BY USER TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/user/{userId} - Valid User - Returns User's Logs")
    void getLogsByUser_validUser_returnsLogs() throws Exception {
        // Arrange
        Log log2 = TestDataBuilder.aValidLog(testUser).id(2L).logName("Winter Field Day 2025").build();
        when(logService.getLogsByUser(1L)).thenReturn(Arrays.asList(testLog, log2));

        // Act & Assert
        mockMvc.perform(get("/api/logs/user/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].logName").value("Field Day 2025"))
                .andExpect(jsonPath("$[1].logName").value("Winter Field Day 2025"));

        verify(logService).getLogsByUser(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/user/{userId} - User Has No Logs - Returns Empty List")
    void getLogsByUser_userHasNoLogs_returnsEmptyList() throws Exception {
        // Arrange
        when(logService.getLogsByUser(1L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/logs/user/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== UPDATE LOG TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/logs/{id} - Valid Update - Returns 200 OK")
    void updateLog_validUpdate_returns200() throws Exception {
        // Arrange
        Log updatedLog = TestDataBuilder.aValidLog(testUser)
                .id(1L)
                .logName("Updated Field Day 2025")
                .build();

        when(logService.updateLog(eq(1L), any(Log.class), eq(1L))).thenReturn(updatedLog);

        // Act & Assert
        mockMvc.perform(put("/api/logs/1")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedLog)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logName").value("Updated Field Day 2025"));

        verify(logService).updateLog(eq(1L), any(Log.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/logs/{id} - Frozen Log - Returns 403 Forbidden")
    void updateLog_frozenLog_returns403() throws Exception {
        // Arrange
        when(logService.updateLog(eq(1L), any(Log.class), eq(1L)))
                .thenThrow(new IllegalStateException("Cannot update frozen log"));

        // Act & Assert
        mockMvc.perform(put("/api/logs/1")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLog)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("frozen")));
    }

    @Test
    @WithMockUser(username = "otheruser")
    @DisplayName("PUT /api/logs/{id} - Not Creator - Returns 403 Forbidden")
    void updateLog_notCreator_returns403() throws Exception {
        // Arrange
        when(logService.updateLog(eq(1L), any(Log.class), eq(2L)))
                .thenThrow(new IllegalStateException("Only the creator can update this log"));

        // Act & Assert
        mockMvc.perform(put("/api/logs/1")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLog)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE LOG TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/logs/{id} - Valid ID - Returns 204 No Content")
    void deleteLog_validId_returns204() throws Exception {
        // Arrange
        doNothing().when(logService).deleteLog(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/logs/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        verify(logService).deleteLog(1L, 1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/logs/{id} - Log Not Found - Returns 404 Not Found")
    void deleteLog_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Log not found"))
                .when(logService).deleteLog(999L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/logs/999")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "otheruser")
    @DisplayName("DELETE /api/logs/{id} - Not Creator - Returns 403 Forbidden")
    void deleteLog_notCreator_returns403() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Only the creator can delete this log"))
                .when(logService).deleteLog(1L, 2L);

        // Act & Assert
        mockMvc.perform(delete("/api/logs/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    // ==================== FREEZE/UNFREEZE LOG TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/logs/{id}/freeze - Valid Log - Returns 200 OK")
    void freezeLog_validLog_returns200() throws Exception {
        // Arrange
        doNothing().when(logService).freezeLog(1L, 1L);

        // Act & Assert
        mockMvc.perform(post("/api/logs/1/freeze")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("frozen")));

        verify(logService).freezeLog(1L, 1L);
    }

    @Test
    @WithMockUser(username = "otheruser")
    @DisplayName("POST /api/logs/{id}/freeze - Not Creator - Returns 403 Forbidden")
    void freezeLog_notCreator_returns403() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Only the creator can freeze this log"))
                .when(logService).freezeLog(1L, 2L);

        // Act & Assert
        mockMvc.perform(post("/api/logs/1/freeze")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/logs/{id}/unfreeze - Valid Frozen Log - Returns 200 OK")
    void unfreezeLog_validFrozenLog_returns200() throws Exception {
        // Arrange
        doNothing().when(logService).unfreezeLog(1L, 1L);

        // Act & Assert
        mockMvc.perform(post("/api/logs/1/unfreeze")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("unfrozen")));

        verify(logService).unfreezeLog(1L, 1L);
    }

    // ==================== PERMISSION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/{id}/has-write-access - User Has Access - Returns True")
    void hasWriteAccess_userHasAccess_returnsTrue() throws Exception {
        // Arrange
        when(logService.hasWriteAccess(1L, 1L)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/logs/1/has-write-access")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAccess").value(true));

        verify(logService).hasWriteAccess(1L, 1L);
    }

    @Test
    @WithMockUser(username = "viewer")
    @DisplayName("GET /api/logs/{id}/has-write-access - User Has No Access - Returns False")
    void hasWriteAccess_userHasNoAccess_returnsFalse() throws Exception {
        // Arrange
        when(logService.hasWriteAccess(1L, 3L)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/logs/1/has-write-access")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAccess").value(false));
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/logs - Duplicate Log Name - Returns 400 Bad Request")
    void createLog_duplicateLogName_returns400() throws Exception {
        // Arrange
        when(logService.createLog(any(Log.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Log name already exists for this user"));

        // Act & Assert
        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLog)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/logs - Log Name Too Long - Returns 400 Bad Request")
    void createLog_logNameTooLong_returns400() throws Exception {
        // Arrange
        Log longNameLog = TestDataBuilder.aValidLog(testUser)
                .logName("A".repeat(300)) // Too long
                .build();

        when(logService.createLog(any(Log.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Log name is too long"));

        // Act & Assert
        mockMvc.perform(post("/api/logs")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longNameLog)))
                .andExpect(status().isBadRequest());
    }

    // ==================== MULTI-USER COLLABORATION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/{id}/participants - Valid Log - Returns Participants List")
    void getLogParticipants_validLog_returnsParticipants() throws Exception {
        // Arrange
        when(logService.getLogParticipants(1L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/logs/1/participants")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        verify(logService).getLogParticipants(1L);
    }

    // ==================== SEARCH LOGS TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/search - By Name - Returns Matching Logs")
    void searchLogs_byName_returnsMatching() throws Exception {
        // Arrange
        when(logService.searchLogs(1L, "Field Day")).thenReturn(List.of(testLog));

        // Act & Assert
        mockMvc.perform(get("/api/logs/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("userId", "1")
                        .param("query", "Field Day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].logName").value(containsString("Field Day")));
    }

    // ==================== LOG STATISTICS TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/{id}/stats - Valid Log - Returns Statistics")
    void getLogStatistics_validLog_returnsStats() throws Exception {
        // Arrange
        when(logService.getLogStatistics(1L)).thenReturn(java.util.Map.of(
                "totalQSOs", 100L,
                "uniqueStations", 75L
        ));

        // Act & Assert
        mockMvc.perform(get("/api/logs/1/stats")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQSOs").value(100))
                .andExpect(jsonPath("$.uniqueStations").value(75));

        verify(logService).getLogStatistics(1L);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/logs/user/{userId} - Invalid User ID - Returns Empty List")
    void getLogsByUser_invalidUserId_returnsEmptyList() throws Exception {
        // Arrange
        when(logService.getLogsByUser(999L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/logs/user/999")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/logs/{id}/freeze - Already Frozen - Returns 400 Bad Request")
    void freezeLog_alreadyFrozen_returns400() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Log is already frozen"))
                .when(logService).freezeLog(1L, 1L);

        // Act & Assert
        mockMvc.perform(post("/api/logs/1/freeze")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already frozen")));
    }
}
