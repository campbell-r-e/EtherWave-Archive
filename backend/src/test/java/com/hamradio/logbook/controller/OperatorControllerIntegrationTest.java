package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Operator;
import com.hamradio.logbook.repository.OperatorRepository;
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
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("Operator API Integration Tests")
class OperatorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OperatorRepository operatorRepository;

    @BeforeEach
    void setUp() {
        operatorRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create operator successfully")
    void shouldCreateOperator() throws Exception {
        Operator operator = new Operator();
        operator.setCallsign("K1ABC");
        operator.setName("John Doe");

        mockMvc.perform(post("/api/operators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operator)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.callsign").value("K1ABC"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Should get all operators")
    void shouldGetAllOperators() throws Exception {
        for (int i = 1; i <= 3; i++) {
            Operator operator = new Operator();
            operator.setCallsign("K" + i + "ABC");
            operator.setName("Operator " + i);
            operatorRepository.save(operator);
        }

        mockMvc.perform(get("/api/operators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Should get operator by ID")
    void shouldGetOperatorById() throws Exception {
        Operator operator = new Operator();
        operator.setCallsign("K2ABC");
        operator.setName("Jane Doe");
        Operator saved = operatorRepository.save(operator);

        mockMvc.perform(get("/api/operators/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callsign").value("K2ABC"))
                .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    @DisplayName("Should get operator by callsign")
    void shouldGetOperatorByCallsign() throws Exception {
        Operator operator = new Operator();
        operator.setCallsign("K3ABC");
        operator.setName("Bob Smith");
        operatorRepository.save(operator);

        mockMvc.perform(get("/api/operators/callsign/K3ABC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callsign").value("K3ABC"))
                .andExpect(jsonPath("$.name").value("Bob Smith"));
    }

    @Test
    @DisplayName("Should update operator successfully")
    void shouldUpdateOperator() throws Exception {
        Operator operator = new Operator();
        operator.setCallsign("K4ABC");
        operator.setName("Old Name");
        Operator saved = operatorRepository.save(operator);

        Operator updated = new Operator();
        updated.setCallsign("K4ABC");
        updated.setName("New Name");

        mockMvc.perform(put("/api/operators/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    @DisplayName("Should delete operator successfully")
    void shouldDeleteOperator() throws Exception {
        Operator operator = new Operator();
        operator.setCallsign("K5ABC");
        operator.setName("To Delete");
        Operator saved = operatorRepository.save(operator);

        mockMvc.perform(delete("/api/operators/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/operators/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent operator")
    void shouldReturn404ForNonExistentOperator() throws Exception {
        mockMvc.perform(get("/api/operators/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 for non-existent callsign")
    void shouldReturn404ForNonExistentCallsign() throws Exception {
        mockMvc.perform(get("/api/operators/callsign/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent operator")
    void shouldReturn404WhenUpdatingNonExistentOperator() throws Exception {
        Operator operator = new Operator();
        operator.setCallsign("K6ABC");
        operator.setName("Test");

        mockMvc.perform(put("/api/operators/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(operator)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent operator")
    void shouldReturn404WhenDeletingNonExistentOperator() throws Exception {
        mockMvc.perform(delete("/api/operators/99999"))
                .andExpect(status().isNotFound());
    }
}
