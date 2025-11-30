package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.LogParticipantRepository;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.UserRepository;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Log Service Tests")
class LogServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private LogParticipantRepository logParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LogService logService;

    private User testUser;
    private User anotherUser;
    private Log testLog;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser().id(1L).build();
        anotherUser = TestDataBuilder.aValidUser()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .callsign("K2ABC")
                .build();
        testLog = TestDataBuilder.aValidLog(testUser).id(1L).build();
    }

    // ==================== CREATE LOG TESTS ====================

    @Test
    @DisplayName("createLog - Valid Log - Creates Successfully")
    void createLog_validLog_createsSuccessfully() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);
        when(logParticipantRepository.save(any(LogParticipant.class)))
                .thenReturn(TestDataBuilder.aLogParticipant(testLog, testUser, LogParticipant.ParticipantRole.CREATOR).build());

        // Act
        Log result = logService.createLog(testLog, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCreator()).isEqualTo(testUser);
        verify(logRepository).save(any(Log.class));
        verify(logParticipantRepository).save(any(LogParticipant.class));
    }

    @Test
    @DisplayName("createLog - User Not Found - Throws Exception")
    void createLog_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> logService.createLog(testLog, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(logRepository, never()).save(any());
    }

    // ==================== GET LOGS TESTS ====================

    @Test
    @DisplayName("getLogsByUser - Returns User's Logs")
    void getLogsByUser_returnsUserLogs() {
        // Arrange
        LogParticipant participant = TestDataBuilder.aLogParticipant(testLog, testUser, LogParticipant.ParticipantRole.CREATOR).build();
        when(logParticipantRepository.findByUserId(1L)).thenReturn(List.of(participant));

        // Act
        List<Log> result = logService.getLogsByUser(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getLogById - Existing Log - Returns Log")
    void getLogById_existingLog_returnsLog() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));

        // Act
        Optional<Log> result = logService.getLogById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    // ==================== FREEZE/UNFREEZE TESTS ====================

    @Test
    @DisplayName("freezeLog - Valid Log - Freezes Successfully")
    void freezeLog_validLog_freezesSuccessfully() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, testUser, LogParticipant.ParticipantRole.CREATOR).build()));
        when(logRepository.save(any(Log.class))).thenReturn(testLog);

        // Act
        logService.freezeLog(1L, 1L);

        // Assert
        verify(logRepository).save(argThat(log -> log.getIsFrozen()));
    }

    @Test
    @DisplayName("freezeLog - Not Creator - Throws Exception")
    void freezeLog_notCreator_throwsException() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, anotherUser, LogParticipant.ParticipantRole.VIEWER).build()));

        // Act & Assert
        assertThatThrownBy(() -> logService.freezeLog(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the creator can freeze");

        verify(logRepository, never()).save(any());
    }

    @Test
    @DisplayName("unfreezeLog - Frozen Log - Unfreezes Successfully")
    void unfreezeLog_frozenLog_unfreezesSuccessfully() {
        // Arrange
        Log frozenLog = TestDataBuilder.aFrozenLog(testUser).id(1L).build();
        when(logRepository.findById(1L)).thenReturn(Optional.of(frozenLog));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(frozenLog, testUser, LogParticipant.ParticipantRole.CREATOR).build()));
        when(logRepository.save(any(Log.class))).thenReturn(frozenLog);

        // Act
        logService.unfreezeLog(1L, 1L);

        // Assert
        verify(logRepository).save(argThat(log -> !log.getIsFrozen()));
    }

    // ==================== PERMISSION TESTS ====================

    @Test
    @DisplayName("hasWriteAccess - Creator - Returns True")
    void hasWriteAccess_creator_returnsTrue() {
        // Arrange
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, testUser, LogParticipant.ParticipantRole.CREATOR).build()));

        // Act
        boolean hasAccess = logService.hasWriteAccess(1L, 1L);

        // Assert
        assertThat(hasAccess).isTrue();
    }

    @Test
    @DisplayName("hasWriteAccess - Station Role - Returns True")
    void hasWriteAccess_stationRole_returnsTrue() {
        // Arrange
        when(logParticipantRepository.findByLogIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, anotherUser, LogParticipant.ParticipantRole.STATION).build()));

        // Act
        boolean hasAccess = logService.hasWriteAccess(1L, 2L);

        // Assert
        assertThat(hasAccess).isTrue();
    }

    @Test
    @DisplayName("hasWriteAccess - Viewer Role - Returns False")
    void hasWriteAccess_viewerRole_returnsFalse() {
        // Arrange
        when(logParticipantRepository.findByLogIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, anotherUser, LogParticipant.ParticipantRole.VIEWER).build()));

        // Act
        boolean hasAccess = logService.hasWriteAccess(1L, 2L);

        // Assert
        assertThat(hasAccess).isFalse();
    }

    @Test
    @DisplayName("hasWriteAccess - No Participation - Returns False")
    void hasWriteAccess_noParticipation_returnsFalse() {
        // Arrange
        when(logParticipantRepository.findByLogIdAndUserId(1L, 999L))
                .thenReturn(Optional.empty());

        // Act
        boolean hasAccess = logService.hasWriteAccess(1L, 999L);

        // Assert
        assertThat(hasAccess).isFalse();
    }

    // ==================== DELETE LOG TESTS ====================

    @Test
    @DisplayName("deleteLog - Creator - Deletes Successfully")
    void deleteLog_creator_deletesSuccessfully() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, testUser, LogParticipant.ParticipantRole.CREATOR).build()));

        // Act
        logService.deleteLog(1L, 1L);

        // Assert
        verify(logRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteLog - Not Creator - Throws Exception")
    void deleteLog_notCreator_throwsException() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, anotherUser, LogParticipant.ParticipantRole.VIEWER).build()));

        // Act & Assert
        assertThatThrownBy(() -> logService.deleteLog(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the creator can delete");

        verify(logRepository, never()).deleteById(any());
    }

    // ==================== UPDATE LOG TESTS ====================

    @Test
    @DisplayName("updateLog - Valid Update - Updates Successfully")
    void updateLog_validUpdate_updatesSuccessfully() {
        // Arrange
        Log updatedLog = TestDataBuilder.aValidLog(testUser)
                .id(1L)
                .logName("Updated Log Name")
                .build();

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, testUser, LogParticipant.ParticipantRole.CREATOR).build()));
        when(logRepository.save(any(Log.class))).thenReturn(updatedLog);

        // Act
        Log result = logService.updateLog(1L, updatedLog, 1L);

        // Assert
        assertThat(result.getLogName()).isEqualTo("Updated Log Name");
        verify(logRepository).save(any(Log.class));
    }

    @Test
    @DisplayName("updateLog - Frozen Log - Throws Exception")
    void updateLog_frozenLog_throwsException() {
        // Arrange
        Log frozenLog = TestDataBuilder.aFrozenLog(testUser).id(1L).build();
        when(logRepository.findById(1L)).thenReturn(Optional.of(frozenLog));

        // Act & Assert
        assertThatThrownBy(() -> logService.updateLog(1L, testLog, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen");

        verify(logRepository, never()).save(any());
    }
}
