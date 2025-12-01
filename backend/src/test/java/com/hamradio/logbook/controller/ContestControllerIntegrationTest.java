package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.config.TestConfig;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.repository.ContestRepository;
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

@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("Contest API Integration Tests")
class ContestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContestRepository contestRepository;

    @BeforeEach
    void setUp() {
        // Don't delete all - some may be pre-seeded. Just clean test data.
    }

    @Test
    @DisplayName("Should create contest successfully")
    void shouldCreateContest() throws Exception {
        Contest contest = new Contest();
        contest.setContestCode("TEST-2025");
        contest.setContestName("Test Contest 2025");
        contest.setIsActive(true);

        mockMvc.perform(post("/api/contests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contestCode").value("TEST-2025"))
                .andExpect(jsonPath("$.contestName").value("Test Contest 2025"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should get all contests")
    void shouldGetAllContests() throws Exception {
        mockMvc.perform(get("/api/contests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should get active contests")
    void shouldGetActiveContests() throws Exception {
        Contest active = new Contest();
        active.setContestCode("ACTIVE-2025");
        active.setContestName("Active Contest");
        active.setIsActive(true);
        contestRepository.save(active);

        Contest inactive = new Contest();
        inactive.setContestCode("INACTIVE-2025");
        inactive.setContestName("Inactive Contest");
        inactive.setIsActive(false);
        contestRepository.save(inactive);

        mockMvc.perform(get("/api/contests/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].isActive", everyItem(is(true))));
    }

    @Test
    @DisplayName("Should get contest by ID")
    void shouldGetContestById() throws Exception {
        Contest contest = new Contest();
        contest.setContestCode("GET-2025");
        contest.setContestName("Get Test");
        contest.setIsActive(true);
        Contest saved = contestRepository.save(contest);

        mockMvc.perform(get("/api/contests/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contestCode").value("GET-2025"));
    }

    @Test
    @DisplayName("Should get contest by code")
    void shouldGetContestByCode() throws Exception {
        Contest contest = new Contest();
        contest.setContestCode("CODE-2025");
        contest.setContestName("Code Test");
        contest.setIsActive(true);
        contestRepository.save(contest);

        mockMvc.perform(get("/api/contests/code/CODE-2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contestName").value("Code Test"));
    }

    @Test
    @DisplayName("Should update contest successfully")
    void shouldUpdateContest() throws Exception {
        Contest contest = new Contest();
        contest.setContestCode("UPDATE-2025");
        contest.setContestName("Old Name");
        contest.setIsActive(true);
        Contest saved = contestRepository.save(contest);

        Contest updated = new Contest();
        updated.setContestCode("UPDATE-2025");
        updated.setContestName("New Name");
        updated.setIsActive(false);

        mockMvc.perform(put("/api/contests/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contestName").value("New Name"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("Should delete contest successfully")
    void shouldDeleteContest() throws Exception {
        Contest contest = new Contest();
        contest.setContestCode("DELETE-2025");
        contest.setContestName("To Delete");
        contest.setIsActive(true);
        Contest saved = contestRepository.save(contest);

        mockMvc.perform(delete("/api/contests/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/contests/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent contest")
    void shouldReturn404ForNonExistentContest() throws Exception {
        mockMvc.perform(get("/api/contests/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent contest code")
    void shouldReturn404ForNonExistentCode() throws Exception {
        mockMvc.perform(get("/api/contests/code/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent contest")
    void shouldReturn404WhenUpdatingNonExistentContest() throws Exception {
        Contest contest = new Contest();
        contest.setContestCode("TEST");
        contest.setContestName("Test");

        mockMvc.perform(put("/api/contests/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent contest")
    void shouldReturn404WhenDeletingNonExistentContest() throws Exception {
        mockMvc.perform(delete("/api/contests/99999"))
                .andExpect(status().isNotFound());
    }
}
