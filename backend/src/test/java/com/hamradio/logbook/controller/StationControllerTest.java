package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.service.StationService;
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
@DisplayName("Station Controller Tests")
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StationService stationService;

    private User testUser;
    private Station testStation;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser().id(1L).build();
        testStation = TestDataBuilder.aValidStation()
                .id(1L)
                .callsign("W1ABC")
                .stationName("Home Station")
                .gridSquare("FN42")
                .build();
        jwtToken = JwtTestUtil.generateToken("testuser");
    }

    // ==================== CREATE STATION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/stations - Valid Station - Returns 201 Created")
    void createStation_validStation_returns201() throws Exception {
        // Arrange
        when(stationService.createStation(any(Station.class), eq(1L))).thenReturn(testStation);

        // Act & Assert
        mockMvc.perform(post("/api/stations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testStation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.callsign").value("W1ABC"))
                .andExpect(jsonPath("$.stationName").value("Home Station"))
                .andExpect(jsonPath("$.gridSquare").value("FN42"));

        verify(stationService).createStation(any(Station.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/stations - Invalid Callsign - Returns 400 Bad Request")
    void createStation_invalidCallsign_returns400() throws Exception {
        // Arrange
        Station invalidStation = TestDataBuilder.aValidStation()
                .callsign("INVALID!!!")
                .build();

        when(stationService.createStation(any(Station.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Invalid callsign format"));

        // Act & Assert
        mockMvc.perform(post("/api/stations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("callsign")));
    }

    @Test
    @DisplayName("POST /api/stations - Not Authenticated - Returns 401 Unauthorized")
    void createStation_notAuthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testStation)))
                .andExpect(status().isUnauthorized());

        verify(stationService, never()).createStation(any(), any());
    }

    // ==================== GET STATION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/stations/{id} - Valid ID - Returns 200 OK")
    void getStation_validId_returns200() throws Exception {
        // Arrange
        when(stationService.getStationById(1L)).thenReturn(Optional.of(testStation));

        // Act & Assert
        mockMvc.perform(get("/api/stations/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.callsign").value("W1ABC"));

        verify(stationService).getStationById(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/stations/{id} - Station Not Found - Returns 404 Not Found")
    void getStation_notFound_returns404() throws Exception {
        // Arrange
        when(stationService.getStationById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/stations/999")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    // ==================== GET STATIONS BY USER TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/stations/user/{userId} - Valid User - Returns Stations")
    void getStationsByUser_validUser_returnsStations() throws Exception {
        // Arrange
        Station station2 = TestDataBuilder.aValidStation().id(2L).callsign("W1XYZ").stationName("Mobile").build();
        when(stationService.getStationsByUser(1L)).thenReturn(Arrays.asList(testStation, station2));

        // Act & Assert
        mockMvc.perform(get("/api/stations/user/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].callsign").value("W1ABC"))
                .andExpect(jsonPath("$[1].callsign").value("W1XYZ"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/stations/user/{userId} - No Stations - Returns Empty List")
    void getStationsByUser_noStations_returnsEmptyList() throws Exception {
        // Arrange
        when(stationService.getStationsByUser(1L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/stations/user/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== UPDATE STATION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("PUT /api/stations/{id} - Valid Update - Returns 200 OK")
    void updateStation_validUpdate_returns200() throws Exception {
        // Arrange
        Station updatedStation = TestDataBuilder.aValidStation()
                .id(1L)
                .callsign("W1ABC")
                .stationName("Updated Home Station")
                .build();

        when(stationService.updateStation(eq(1L), any(Station.class), eq(1L)))
                .thenReturn(updatedStation);

        // Act & Assert
        mockMvc.perform(put("/api/stations/1")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedStation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationName").value("Updated Home Station"));

        verify(stationService).updateStation(eq(1L), any(Station.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "otheruser")
    @DisplayName("PUT /api/stations/{id} - Not Owner - Returns 403 Forbidden")
    void updateStation_notOwner_returns403() throws Exception {
        // Arrange
        when(stationService.updateStation(eq(1L), any(Station.class), eq(2L)))
                .thenThrow(new IllegalStateException("Not authorized to update this station"));

        // Act & Assert
        mockMvc.perform(put("/api/stations/1")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testStation)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE STATION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/stations/{id} - Valid ID - Returns 204 No Content")
    void deleteStation_validId_returns204() throws Exception {
        // Arrange
        doNothing().when(stationService).deleteStation(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/stations/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        verify(stationService).deleteStation(1L, 1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("DELETE /api/stations/{id} - Station In Use - Returns 409 Conflict")
    void deleteStation_stationInUse_returns409() throws Exception {
        // Arrange
        doThrow(new IllegalStateException("Station is in use by existing QSOs"))
                .when(stationService).deleteStation(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/stations/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("in use")));
    }

    // ==================== SEARCH STATIONS TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("GET /api/stations/search - By Callsign - Returns Matching Stations")
    void searchStations_byCallsign_returnsMatching() throws Exception {
        // Arrange
        when(stationService.searchStations(1L, "W1ABC")).thenReturn(List.of(testStation));

        // Act & Assert
        mockMvc.perform(get("/api/stations/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("userId", "1")
                        .param("query", "W1ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].callsign").value("W1ABC"));
    }

    // ==================== GRID SQUARE VALIDATION TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/stations - Invalid Grid Square - Returns 400 Bad Request")
    void createStation_invalidGridSquare_returns400() throws Exception {
        // Arrange
        Station invalidStation = TestDataBuilder.aValidStation()
                .gridSquare("INVALID")
                .build();

        when(stationService.createStation(any(Station.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Invalid grid square format"));

        // Act & Assert
        mockMvc.perform(post("/api/stations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("grid")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/stations - Valid Grid Squares - Creates Successfully")
    void createStation_validGridSquares_createsSuccessfully() throws Exception {
        // Arrange
        String[] validGrids = {"FN42", "FN42ab", "DM13", "IO91vl"};

        for (String grid : validGrids) {
            Station station = TestDataBuilder.aValidStation().gridSquare(grid).build();
            when(stationService.createStation(any(Station.class), eq(1L))).thenReturn(station);

            mockMvc.perform(post("/api/stations")
                            .header("Authorization", "Bearer " + jwtToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(station)))
                    .andExpect(status().isCreated());
        }
    }

    // ==================== DUPLICATE CALLSIGN TESTS ====================

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("POST /api/stations - Duplicate Callsign for Same User - Returns 400 Bad Request")
    void createStation_duplicateCallsign_returns400() throws Exception {
        // Arrange
        when(stationService.createStation(any(Station.class), eq(1L)))
                .thenThrow(new IllegalArgumentException("Station with this callsign already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/stations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testStation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }
}
