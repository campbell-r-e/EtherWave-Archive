package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.QSORequest;
import com.hamradio.logbook.dto.QSOResponse;
import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.exception.ResourceNotFoundException;
import com.hamradio.logbook.repository.*;
import com.hamradio.logbook.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QSOService Unit Tests")
class QSOServiceTest {

    @Mock
    private QSORepository qsoRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private LogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private QSOValidationService validationService;

    @Mock
    private LogService logService;

    @InjectMocks
    private QSOService qsoService;

    private User testUser;
    private Log testLog;
    private Station testStation;
    private Operator testOperator;
    private Contest testContest;
    private QSO testQSO;
    private QSORequest testRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setCallsign("W1TEST");

        // Create test log
        testLog = new Log();
        testLog.setId(1L);
        testLog.setName("Test Log");
        testLog.setType(Log.LogType.PERSONAL);
        testLog.setCreator(testUser);
        testLog.setEditable(true);

        // Create test station
        testStation = new Station();
        testStation.setId(1L);
        testStation.setStationName("Test Station");
        testStation.setCallsign("W1TEST");

        // Create test operator
        testOperator = new Operator();
        testOperator.setId(1L);
        testOperator.setCallsign("W1TEST");

        // Create test contest
        testContest = new Contest();
        testContest.setId(1L);
        testContest.setContestCode("CQWW");

        // Create test QSO
        testQSO = QSO.builder()
                .id(1L)
                .log(testLog)
                .station(testStation)
                .operator(testOperator)
                .contest(testContest)
                .callsign("W2TEST")
                .frequencyKhz(14250L)
                .mode("SSB")
                .qsoDate(LocalDate.now())
                .timeOn(LocalTime.now())
                .rstSent("59")
                .rstRcvd("59")
                .band("20M")
                .isValid(true)
                .build();

        // Create test request
        testRequest = new QSORequest();
        testRequest.setStationId(1L);
        testRequest.setOperatorId(1L);
        testRequest.setContestId(1L);
        testRequest.setCallsign("w2test");
        testRequest.setFrequencyKhz(14250L);
        testRequest.setMode("SSB");
        testRequest.setQsoDate(LocalDate.now());
        testRequest.setTimeOn(LocalTime.now());
        testRequest.setRstSent("59");
        testRequest.setRstRcvd("59");
        testRequest.setBand("20M");
    }

    @Test
    @DisplayName("Should create QSO successfully")
    void shouldCreateQSO() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(contestRepository.findById(1L)).thenReturn(Optional.of(testContest));

        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);
        when(validationService.validateQSO(any(QSO.class))).thenReturn(validationResult);

        when(qsoRepository.save(any(QSO.class))).thenReturn(testQSO);
        doNothing().when(messagingTemplate).convertAndSend(eq("/topic/qsos"), any(QSOResponse.class));

        QSOResponse result = qsoService.createQSO(testRequest, 1L, "testuser");

        assertNotNull(result);
        assertEquals("W2TEST", result.getCallsign());
        verify(qsoRepository).save(any(QSO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/qsos"), any(QSOResponse.class));
    }

    @Test
    @DisplayName("Should fail to create QSO when user not found")
    void shouldFailToCreateQSOWhenUserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                qsoService.createQSO(testRequest, 1L, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to create QSO when log not found")
    void shouldFailToCreateQSOWhenLogNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                qsoService.createQSO(testRequest, 1L, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to create QSO when user has no permission")
    void shouldFailToCreateQSOWhenNoPermission() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.canEdit(testLog, testUser)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                qsoService.createQSO(testRequest, 1L, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to create QSO when log is frozen")
    void shouldFailToCreateQSOWhenLogFrozen() {
        testLog.setEditable(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                qsoService.createQSO(testRequest, 1L, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to create QSO when station not found")
    void shouldFailToCreateQSOWhenStationNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);
        when(stationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                qsoService.createQSO(testRequest, 1L, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create QSO without operator")
    void shouldCreateQSOWithoutOperator() {
        testRequest.setOperatorId(null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(contestRepository.findById(1L)).thenReturn(Optional.of(testContest));

        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);
        when(validationService.validateQSO(any(QSO.class))).thenReturn(validationResult);

        when(qsoRepository.save(any(QSO.class))).thenReturn(testQSO);
        doNothing().when(messagingTemplate).convertAndSend(eq("/topic/qsos"), any(QSOResponse.class));

        QSOResponse result = qsoService.createQSO(testRequest, 1L, "testuser");

        assertNotNull(result);
        verify(qsoRepository).save(any(QSO.class));
        verify(operatorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should create QSO with validation errors")
    void shouldCreateQSOWithValidationErrors() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(contestRepository.findById(1L)).thenReturn(Optional.of(testContest));

        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(false);
        validationResult.getErrors().add("Invalid frequency");
        validationResult.getWarnings().add("Missing grid square");
        when(validationService.validateQSO(any(QSO.class))).thenReturn(validationResult);

        when(qsoRepository.save(any(QSO.class))).thenReturn(testQSO);
        doNothing().when(messagingTemplate).convertAndSend(eq("/topic/qsos"), any(QSOResponse.class));

        QSOResponse result = qsoService.createQSO(testRequest, 1L, "testuser");

        assertNotNull(result);
        verify(qsoRepository).save(argThat(qso ->
            qso.getValidationErrors() != null &&
            qso.getValidationErrors().contains("Invalid frequency")
        ));
    }

    @Test
    @DisplayName("Should get QSO successfully")
    void shouldGetQSO() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.hasAccess(testLog, testUser)).thenReturn(true);

        QSOResponse result = qsoService.getQSO(1L, "testuser");

        assertNotNull(result);
        assertEquals("W2TEST", result.getCallsign());
        verify(qsoRepository).findById(1L);
    }

    @Test
    @DisplayName("Should fail to get QSO when not found")
    void shouldFailToGetQSOWhenNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                qsoService.getQSO(1L, "testuser"));
    }

    @Test
    @DisplayName("Should fail to get QSO when no access")
    void shouldFailToGetQSOWhenNoAccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.hasAccess(testLog, testUser)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                qsoService.getQSO(1L, "testuser"));
    }

    @Test
    @DisplayName("Should update QSO successfully")
    void shouldUpdateQSO() {
        testRequest.setCallsign("w3test");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);

        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);
        when(validationService.validateQSO(any(QSO.class))).thenReturn(validationResult);

        when(qsoRepository.save(any(QSO.class))).thenReturn(testQSO);

        QSOResponse result = qsoService.updateQSO(1L, testRequest, "testuser");

        assertNotNull(result);
        verify(qsoRepository).save(any(QSO.class));
        verify(qsoRepository).findById(1L);
    }

    @Test
    @DisplayName("Should fail to update QSO when not found")
    void shouldFailToUpdateQSOWhenNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                qsoService.updateQSO(1L, testRequest, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to update QSO when no permission")
    void shouldFailToUpdateQSOWhenNoPermission() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.canEdit(testLog, testUser)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                qsoService.updateQSO(1L, testRequest, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to update QSO when log is frozen")
    void shouldFailToUpdateQSOWhenLogFrozen() {
        testLog.setEditable(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                qsoService.updateQSO(1L, testRequest, "testuser"));

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete QSO successfully")
    void shouldDeleteQSO() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);
        doNothing().when(qsoRepository).deleteById(1L);

        qsoService.deleteQSO(1L, "testuser");

        verify(qsoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should fail to delete QSO when not found")
    void shouldFailToDeleteQSOWhenNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                qsoService.deleteQSO(1L, "testuser"));

        verify(qsoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should fail to delete QSO when no permission")
    void shouldFailToDeleteQSOWhenNoPermission() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.canEdit(testLog, testUser)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                qsoService.deleteQSO(1L, "testuser"));

        verify(qsoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should fail to delete QSO when log is frozen")
    void shouldFailToDeleteQSOWhenLogFrozen() {
        testLog.setEditable(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(logService.canEdit(testLog, testUser)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                qsoService.deleteQSO(1L, "testuser"));

        verify(qsoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should get all QSOs with pagination")
    void shouldGetAllQSOs() {
        List<QSO> qsoList = Arrays.asList(testQSO);
        Page<QSO> qsoPage = new PageImpl<>(qsoList);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.hasAccess(testLog, testUser)).thenReturn(true);
        when(qsoRepository.findByLogId(eq(1L), any(Pageable.class))).thenReturn(qsoPage);

        Page<QSOResponse> result = qsoService.getAllQSOs(1L, 0, 10, "testuser");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(qsoRepository).findByLogId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("Should fail to get all QSOs when log not found")
    void shouldFailToGetAllQSOsWhenLogNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                qsoService.getAllQSOs(1L, 0, 10, "testuser"));
    }

    @Test
    @DisplayName("Should fail to get all QSOs when no access")
    void shouldFailToGetAllQSOsWhenNoAccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.hasAccess(testLog, testUser)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                qsoService.getAllQSOs(1L, 0, 10, "testuser"));
    }

    @Test
    @DisplayName("Should get recent QSOs")
    void shouldGetRecentQSOs() {
        List<QSO> qsoList = Arrays.asList(testQSO);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.hasAccess(testLog, testUser)).thenReturn(true);
        when(qsoRepository.findRecentByLogId(eq(1L), any(Pageable.class))).thenReturn(qsoList);

        List<QSOResponse> result = qsoService.getRecentQSOs(1L, 10, "testuser");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(qsoRepository).findRecentByLogId(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get QSOs by date range")
    void shouldGetQSOsByDateRange() {
        List<QSO> qsoList = Arrays.asList(testQSO);
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.hasAccess(testLog, testUser)).thenReturn(true);
        when(qsoRepository.findByLogIdAndDateRange(1L, startDate, endDate)).thenReturn(qsoList);

        List<QSOResponse> result = qsoService.getQSOsByDateRange(1L, startDate, endDate, "testuser");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(qsoRepository).findByLogIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    @DisplayName("Should get contacted states")
    void shouldGetContactedStates() {
        List<String> states = Arrays.asList("CA", "TX", "NY");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.hasAccess(testLog, testUser)).thenReturn(true);
        when(qsoRepository.findDistinctStatesByLogId(1L)).thenReturn(states);

        List<String> result = qsoService.getContactedStates(1L, "testuser");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("CA"));
        verify(qsoRepository).findDistinctStatesByLogId(1L);
    }

    @Test
    @DisplayName("Should fail to get contacted states when no access")
    void shouldFailToGetContactedStatesWhenNoAccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.hasAccess(testLog, testUser)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                qsoService.getContactedStates(1L, "testuser"));
    }
}
