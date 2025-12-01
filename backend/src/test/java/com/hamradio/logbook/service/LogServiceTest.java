package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.log.LogRequest;
import com.hamradio.logbook.dto.log.LogResponse;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.exception.ResourceNotFoundException;
import com.hamradio.logbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogService Unit Tests")
class LogServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private LogParticipantRepository logParticipantRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QSORepository qsoRepository;

    @InjectMocks
    private LogService logService;

    private User testUser;
    private Log testLog;
    private Contest testContest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCallsign("W1TEST");

        testLog = new Log();
        testLog.setId(1L);
        testLog.setName("Test Log");
        testLog.setDescription("Test Description");
        testLog.setType(Log.LogType.PERSONAL);
        testLog.setCreator(testUser);
        testLog.setActive(true);
        testLog.setEditable(true);
        testLog.setIsPublic(false);

        testContest = new Contest();
        testContest.setId(1L);
        testContest.setContestName("Test Contest");
    }

    @Test
    @DisplayName("Should get logs for user successfully")
    void shouldGetLogsForUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findLogsByUser(testUser)).thenReturn(List.of(testLog));
        when(logParticipantRepository.findActiveParticipantsByLogId(anyLong())).thenReturn(List.of());
        when(qsoRepository.countByLogId(anyLong())).thenReturn(0L);

        List<LogResponse> result = logService.getLogsForUser("testuser");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Log", result.get(0).getName());
        verify(logRepository).findLogsByUser(testUser);
    }

    @Test
    @DisplayName("Should get log by ID successfully")
    void shouldGetLogById() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findActiveParticipantsByLogId(anyLong())).thenReturn(List.of());
        when(qsoRepository.countByLogId(anyLong())).thenReturn(0L);

        LogResponse result = logService.getLogById(1L, "testuser");

        assertNotNull(result);
        assertEquals("Test Log", result.getName());
        verify(logRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when log not found")
    void shouldThrowExceptionWhenLogNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            logService.getLogById(999L, "testuser")
        );
    }

    @Test
    @DisplayName("Should throw exception when user has no access")
    void shouldThrowExceptionWhenUserHasNoAccess() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        testLog.setIsPublic(false);

        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogAndUser(testLog, otherUser)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
            logService.getLogById(1L, "otheruser")
        );
    }

    @Test
    @DisplayName("Should create log successfully")
    void shouldCreateLog() {
        LogRequest request = new LogRequest();
        request.setName("New Log");
        request.setDescription("New Description");
        request.setType(Log.LogType.PERSONAL);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);
        when(logParticipantRepository.save(any(LogParticipant.class))).thenReturn(new LogParticipant());
        when(logParticipantRepository.findActiveParticipantsByLogId(anyLong())).thenReturn(List.of());
        when(qsoRepository.countByLogId(anyLong())).thenReturn(0L);

        LogResponse result = logService.createLog(request, "testuser");

        assertNotNull(result);
        verify(logRepository).save(any(Log.class));
        verify(logParticipantRepository).save(any(LogParticipant.class));
    }

    @Test
    @DisplayName("Should create log with contest successfully")
    void shouldCreateLogWithContest() {
        LogRequest request = new LogRequest();
        request.setName("Contest Log");
        request.setType(Log.LogType.SHARED);
        request.setContestId(1L);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(contestRepository.findById(1L)).thenReturn(Optional.of(testContest));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);
        when(logParticipantRepository.save(any(LogParticipant.class))).thenReturn(new LogParticipant());
        when(logParticipantRepository.findActiveParticipantsByLogId(anyLong())).thenReturn(List.of());
        when(qsoRepository.countByLogId(anyLong())).thenReturn(0L);

        LogResponse result = logService.createLog(request, "testuser");

        assertNotNull(result);
        verify(contestRepository).findById(1L);
        verify(logRepository).save(any(Log.class));
    }

    @Test
    @DisplayName("Should update log successfully")
    void shouldUpdateLog() {
        LogRequest request = new LogRequest();
        request.setName("Updated Log");
        request.setDescription("Updated Description");
        request.setType(Log.LogType.SHARED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);
        when(logParticipantRepository.findActiveParticipantsByLogId(anyLong())).thenReturn(List.of());
        when(qsoRepository.countByLogId(anyLong())).thenReturn(0L);

        LogResponse result = logService.updateLog(1L, request, "testuser");

        assertNotNull(result);
        verify(logRepository).save(any(Log.class));
    }

    @Test
    @DisplayName("Should throw exception when non-creator tries to update")
    void shouldThrowExceptionWhenNonCreatorUpdates() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        LogRequest request = new LogRequest();
        request.setName("Updated Log");

        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));

        assertThrows(SecurityException.class, () ->
            logService.updateLog(1L, request, "otheruser")
        );
    }

    @Test
    @DisplayName("Should delete log successfully")
    void shouldDeleteLog() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);

        assertDoesNotThrow(() -> logService.deleteLog(1L, "testuser"));

        verify(logRepository).save(testLog);
        assertFalse(testLog.getActive());
    }

    @Test
    @DisplayName("Should freeze log successfully")
    void shouldFreezeLog() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);
        when(logParticipantRepository.findActiveParticipantsByLogId(anyLong())).thenReturn(List.of());
        when(qsoRepository.countByLogId(anyLong())).thenReturn(0L);

        LogResponse result = logService.freezeLog(1L, "testuser");

        assertNotNull(result);
        verify(logRepository).save(testLog);
        assertFalse(testLog.isEditable());
    }

    @Test
    @DisplayName("Should unfreeze log successfully")
    void shouldUnfreezeLog() {
        testLog.setEditable(false);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);
        when(logParticipantRepository.findActiveParticipantsByLogId(anyLong())).thenReturn(List.of());
        when(qsoRepository.countByLogId(anyLong())).thenReturn(0L);

        LogResponse result = logService.unfreezeLog(1L, "testuser");

        assertNotNull(result);
        verify(logRepository).save(testLog);
        assertTrue(testLog.isEditable());
    }

    @Test
    @DisplayName("Should get log participants successfully")
    void shouldGetLogParticipants() {
        LogParticipant participant = new LogParticipant();
        participant.setUser(testUser);
        participant.setLog(testLog);
        participant.setRole(LogParticipant.ParticipantRole.CREATOR);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findActiveParticipantsByLogId(1L)).thenReturn(List.of(participant));

        List<LogParticipant> result = logService.getLogParticipants(1L, "testuser");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(logParticipantRepository).findActiveParticipantsByLogId(1L);
    }

    @Test
    @DisplayName("Should remove participant successfully")
    void shouldRemoveParticipant() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("participant");

        LogParticipant participant = new LogParticipant();
        participant.setId(2L);
        participant.setUser(otherUser);
        participant.setLog(testLog);
        participant.setRole(LogParticipant.ParticipantRole.STATION);
        participant.setActive(true);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(logParticipantRepository.save(any(LogParticipant.class))).thenReturn(participant);

        assertDoesNotThrow(() -> logService.removeParticipant(1L, 2L, "testuser"));

        verify(logParticipantRepository).save(participant);
        assertFalse(participant.getActive());
    }

    @Test
    @DisplayName("Should throw exception when trying to remove creator")
    void shouldThrowExceptionWhenRemovingCreator() {
        LogParticipant creatorParticipant = new LogParticipant();
        creatorParticipant.setId(1L);
        creatorParticipant.setUser(testUser);
        creatorParticipant.setRole(LogParticipant.ParticipantRole.CREATOR);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findById(1L)).thenReturn(Optional.of(creatorParticipant));

        assertThrows(IllegalArgumentException.class, () ->
            logService.removeParticipant(1L, 1L, "testuser")
        );
    }

    @Test
    @DisplayName("Should leave log successfully")
    void shouldLeaveLog() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("participant");

        LogParticipant participant = new LogParticipant();
        participant.setUser(otherUser);
        participant.setLog(testLog);
        participant.setRole(LogParticipant.ParticipantRole.STATION);
        participant.setActive(true);

        when(userRepository.findByUsername("participant")).thenReturn(Optional.of(otherUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogAndUser(testLog, otherUser)).thenReturn(Optional.of(participant));
        when(logParticipantRepository.save(any(LogParticipant.class))).thenReturn(participant);

        assertDoesNotThrow(() -> logService.leaveLog(1L, "participant"));

        verify(logParticipantRepository).save(participant);
        assertFalse(participant.getActive());
    }

    @Test
    @DisplayName("Should throw exception when creator tries to leave")
    void shouldThrowExceptionWhenCreatorLeaves() {
        LogParticipant creatorParticipant = new LogParticipant();
        creatorParticipant.setUser(testUser);
        creatorParticipant.setRole(LogParticipant.ParticipantRole.CREATOR);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogAndUser(testLog, testUser)).thenReturn(Optional.of(creatorParticipant));

        assertThrows(IllegalArgumentException.class, () ->
            logService.leaveLog(1L, "testuser")
        );
    }

    @Test
    @DisplayName("Should check hasAccess returns true for public log")
    void shouldHasAccessReturnTrueForPublicLog() {
        testLog.setIsPublic(true);
        User anyUser = new User();
        anyUser.setId(999L);

        boolean result = logService.hasAccess(testLog, anyUser);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should check hasAccess returns true for creator")
    void shouldHasAccessReturnTrueForCreator() {
        testLog.setIsPublic(false);

        boolean result = logService.hasAccess(testLog, testUser);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should check hasAccess returns true for active participant")
    void shouldHasAccessReturnTrueForActiveParticipant() {
        User participant = new User();
        participant.setId(2L);

        LogParticipant logParticipant = new LogParticipant();
        logParticipant.setActive(true);

        testLog.setIsPublic(false);

        when(logParticipantRepository.findByLogAndUser(testLog, participant)).thenReturn(Optional.of(logParticipant));

        boolean result = logService.hasAccess(testLog, participant);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should check hasAccess returns false for non-participant")
    void shouldHasAccessReturnFalseForNonParticipant() {
        User nonParticipant = new User();
        nonParticipant.setId(2L);

        testLog.setIsPublic(false);

        when(logParticipantRepository.findByLogAndUser(testLog, nonParticipant)).thenReturn(Optional.empty());

        boolean result = logService.hasAccess(testLog, nonParticipant);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should check isCreator returns true for creator")
    void shouldIsCreatorReturnTrueForCreator() {
        boolean result = logService.isCreator(testLog, testUser);
        assertTrue(result);
    }

    @Test
    @DisplayName("Should check isCreator returns false for non-creator")
    void shouldIsCreatorReturnFalseForNonCreator() {
        User otherUser = new User();
        otherUser.setId(2L);

        boolean result = logService.isCreator(testLog, otherUser);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should check canEdit returns false when log is not editable")
    void shouldCanEditReturnFalseWhenLogNotEditable() {
        testLog.setEditable(false);

        boolean result = logService.canEdit(testLog, testUser);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should check canEdit returns true for creator")
    void shouldCanEditReturnTrueForCreator() {
        boolean result = logService.canEdit(testLog, testUser);
        assertTrue(result);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            logService.getLogsForUser("nonexistent")
        );
    }
}
