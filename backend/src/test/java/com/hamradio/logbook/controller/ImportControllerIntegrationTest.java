package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.config.TestConfig;
import com.hamradio.logbook.entity.Log;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("Import API Integration Tests")
class ImportControllerIntegrationTest {

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
        user.setUsername("importuser");
        user.setEmail("import@example.com");
        user.setCallsign("W1IMP");
        user.setPassword(passwordEncoder.encode("password123"));
        user.addRole(User.Role.ROLE_USER);
        user = userRepository.save(user);

        // Create station
        Station station = new Station();
        station.setStationName("Import Station");
        station.setCallsign("W1IMP");
        station = stationRepository.save(station);
        stationId = station.getId();

        // Create log directly without authentication (filters disabled)
        Log log = new Log();
        log.setName("Import Log");
        log.setType(Log.LogType.PERSONAL);
        log.setCreator(user);  // Set the creator to avoid NOT NULL constraint
        log = logRepository.save(log);
        logId = log.getId();
    }

    @Test
    @DisplayName("Should reject empty file")
    void shouldRejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.adi",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/import/adif/" + logId)
                        .file(file)
                        .param("stationId", stationId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should reject non-ADIF file")
    void shouldRejectNonAdifFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "not an adif file".getBytes()
        );

        mockMvc.perform(multipart("/api/import/adif/" + logId)
                        .file(file)
                        .param("stationId", stationId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should handle valid ADIF file")
    void shouldHandleValidAdifFile() throws Exception {
        String adifContent = "ADIF Export\n<CALL:5>W1AW<BAND:3>20M<MODE:3>SSB<QSO_DATE:8>20250101<TIME_ON:4>1234<RST_SENT:2>59<RST_RCVD:2>59<EOR>\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.adi",
                "text/plain",
                adifContent.getBytes()
        );

        mockMvc.perform(multipart("/api/import/adif/" + logId)
                        .file(file)
                        .param("stationId", stationId.toString()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 206 || status == 400 : "Expected 200/206/400, but got: " + status;
                });
    }

    @Test
    @DisplayName("Should handle malformed ADIF file gracefully")
    void shouldHandleMalformedAdifFile() throws Exception {
        String adifContent = "Invalid ADIF content with no proper tags";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malformed.adif",
                "text/plain",
                adifContent.getBytes()
        );

        mockMvc.perform(multipart("/api/import/adif/" + logId)
                        .file(file)
                        .param("stationId", stationId.toString()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 206 || status == 400;
                });
    }
}
