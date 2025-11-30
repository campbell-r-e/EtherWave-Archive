package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.repository.ContestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hamradio.logbook.config.TestConfig;
import org.springframework.context.annotation.Import;

@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Contest Controller Tests")
class ContestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContestRepository contestRepository;

    private Contest fieldDayContest;
    private Contest potaContest;

    @BeforeEach
    void setUp() {
        fieldDayContest = Contest.builder()
                .id(1L)
                .contestCode("ARRL-FD")
                .contestName("ARRL Field Day")
                .description("Annual emergency preparedness exercise")
                .isActive(true)
                .validatorClass("com.hamradio.logbook.validation.FieldDayValidator")
                .rulesConfig("{\"required_fields\":[\"class\",\"section\"]}")
                .build();

        potaContest = Contest.builder()
                .id(2L)
                .contestCode("POTA")
                .contestName("Parks on the Air")
                .description("Activate and hunt parks")
                .isActive(true)
                .validatorClass("com.hamradio.logbook.validation.POTAValidator")
                .rulesConfig("{\"required_fields\":[\"park_ref\"]}")
                .build();
    }

    // ==================== GET ALL CONTESTS ====================

    @Test
    @DisplayName("GET /api/contests - Returns All Contests")
    void getAllContests_returnsAllContests() throws Exception {
        when(contestRepository.findAll()).thenReturn(List.of(fieldDayContest, potaContest));

        mockMvc.perform(get("/api/contests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].contestCode").value("ARRL-FD"))
                .andExpect(jsonPath("$[1].contestCode").value("POTA"));

        verify(contestRepository).findAll();
    }

    @Test
    @DisplayName("GET /api/contests - No Contests - Returns Empty List")
    void getAllContests_noContests_returnsEmptyList() throws Exception {
        when(contestRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/contests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(contestRepository).findAll();
    }

    // ==================== GET ACTIVE CONTESTS ====================

    @Test
    @DisplayName("GET /api/contests/active - Returns Active Contests Only")
    void getActiveContests_returnsActiveContestsOnly() throws Exception {
        when(contestRepository.findByIsActive(true)).thenReturn(List.of(fieldDayContest, potaContest));

        mockMvc.perform(get("/api/contests/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].isActive").value(true));

        verify(contestRepository).findByIsActive(true);
    }

    // ==================== GET CONTEST BY ID ====================

    @Test
    @DisplayName("GET /api/contests/{id} - Valid ID - Returns Contest")
    void getContest_validId_returnsContest() throws Exception {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(fieldDayContest));

        mockMvc.perform(get("/api/contests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.contestCode").value("ARRL-FD"))
                .andExpect(jsonPath("$.contestName").value("ARRL Field Day"));

        verify(contestRepository).findById(1L);
    }

    @Test
    @DisplayName("GET /api/contests/{id} - Invalid ID - Returns 404")
    void getContest_invalidId_returns404() throws Exception {
        when(contestRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/contests/999"))
                .andExpect(status().isNotFound());

        verify(contestRepository).findById(999L);
    }

    // ==================== GET CONTEST BY CODE ====================

    @Test
    @DisplayName("GET /api/contests/code/{code} - Valid Code - Returns Contest")
    void getContestByCode_validCode_returnsContest() throws Exception {
        when(contestRepository.findByContestCode("ARRL-FD")).thenReturn(Optional.of(fieldDayContest));

        mockMvc.perform(get("/api/contests/code/ARRL-FD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contestCode").value("ARRL-FD"))
                .andExpect(jsonPath("$.contestName").value("ARRL Field Day"));

        verify(contestRepository).findByContestCode("ARRL-FD");
    }

    @Test
    @DisplayName("GET /api/contests/code/{code} - Invalid Code - Returns 404")
    void getContestByCode_invalidCode_returns404() throws Exception {
        when(contestRepository.findByContestCode("INVALID")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/contests/code/INVALID"))
                .andExpect(status().isNotFound());

        verify(contestRepository).findByContestCode("INVALID");
    }

    // ==================== CREATE CONTEST ====================

    @Test
    @DisplayName("POST /api/contests - Valid Contest - Returns 201 Created")
    void createContest_validContest_returns201() throws Exception {
        Contest newContest = Contest.builder()
                .contestCode("WFD")
                .contestName("Winter Field Day")
                .description("Winter emergency preparedness")
                .isActive(true)
                .build();

        Contest savedContest = Contest.builder()
                .id(3L)
                .contestCode("WFD")
                .contestName("Winter Field Day")
                .description("Winter emergency preparedness")
                .isActive(true)
                .build();

        when(contestRepository.save(any(Contest.class))).thenReturn(savedContest);

        mockMvc.perform(post("/api/contests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newContest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.contestCode").value("WFD"))
                .andExpect(jsonPath("$.contestName").value("Winter Field Day"));

        // Verify save was called (may be called multiple times due to DataInitializationService)
        verify(contestRepository, atLeastOnce()).save(any(Contest.class));
    }

    // ==================== UPDATE CONTEST ====================

    @Test
    @DisplayName("PUT /api/contests/{id} - Valid Contest - Returns Updated Contest")
    void updateContest_validContest_returnsUpdatedContest() throws Exception {
        Contest updatedContest = Contest.builder()
                .id(1L)
                .contestCode("ARRL-FD")
                .contestName("ARRL Field Day Updated")
                .description("Updated description")
                .isActive(true)
                .build();

        when(contestRepository.existsById(1L)).thenReturn(true);
        when(contestRepository.save(any(Contest.class))).thenReturn(updatedContest);

        mockMvc.perform(put("/api/contests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedContest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contestName").value("ARRL Field Day Updated"))
                .andExpect(jsonPath("$.description").value("Updated description"));

        verify(contestRepository).existsById(1L);
        verify(contestRepository).save(any(Contest.class));
    }

    @Test
    @DisplayName("PUT /api/contests/{id} - Non-existent ID - Returns 404")
    void updateContest_nonExistentId_returns404() throws Exception {
        Contest updatedContest = Contest.builder()
                .contestCode("ARRL-FD")
                .contestName("Updated")
                .build();

        when(contestRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(put("/api/contests/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedContest)))
                .andExpect(status().isNotFound());

        verify(contestRepository).existsById(999L);
        verify(contestRepository, never()).save(any(Contest.class));
    }

    // ==================== DELETE CONTEST ====================

    @Test
    @DisplayName("DELETE /api/contests/{id} - Valid ID - Returns 204 No Content")
    void deleteContest_validId_returns204() throws Exception {
        when(contestRepository.existsById(1L)).thenReturn(true);
        doNothing().when(contestRepository).deleteById(1L);

        mockMvc.perform(delete("/api/contests/1"))
                .andExpect(status().isNoContent());

        verify(contestRepository).existsById(1L);
        verify(contestRepository).deleteById(1L);
    }

    @Test
    @DisplayName("DELETE /api/contests/{id} - Non-existent ID - Returns 404")
    void deleteContest_nonExistentId_returns404() throws Exception {
        when(contestRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/contests/999"))
                .andExpect(status().isNotFound());

        verify(contestRepository).existsById(999L);
        verify(contestRepository, never()).deleteById(anyLong());
    }
}
