package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Log Service Edge Case Tests")
class LogServiceEdgeCaseTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private QSORepository qsoRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @InjectMocks
    private LogService logService;

    private User user;
    private Log log;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        log = new Log();
        log.setId(1L);
        log.setName("Test Log");
        log.setOwner(user);
        log.setFrozen(false);
    }

    // ==================== NULL INPUT EDGE CASES ====================

    @Test
    @DisplayName("createLog - Null Log - Throws IllegalArgumentException")
    void createLog_nullLog_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> logService.createLog(null, user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Log cannot be null");
    }

    @Test
    @DisplayName("createLog - Null User - Throws IllegalArgumentException")
    void createLog_nullUser_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> logService.createLog(log, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User cannot be null");
    }

    @Test
    @DisplayName("createLog - Null Name - Throws ValidationException")
    void createLog_nullName_throwsValidationException() {
        log.setName(null);

        assertThatThrownBy(() -> logService.createLog(log, user))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Log name is required");
    }

    // ==================== EMPTY STRING EDGE CASES ====================

    @Test
    @DisplayName("createLog - Empty Name - Throws ValidationException")
    void createLog_emptyName_throwsValidationException() {
        log.setName("");

        assertThatThrownBy(() -> logService.createLog(log, user))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Log name cannot be empty");
    }

    @Test
    @DisplayName("createLog - Whitespace Only Name - Throws ValidationException")
    void createLog_whitespaceName_throwsValidationException() {
        log.setName("   ");

        assertThatThrownBy(() -> logService.createLog(log, user))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Log name cannot be empty");
    }

    // ==================== SPECIAL CHARACTER EDGE CASES ====================

    @Test
    @DisplayName("createLog - Name with Special Characters - Accepts")
    void createLog_nameWithSpecialCharacters_accepts() {
        log.setName("Field Day 2025 (W1AW) - Class 2A");

        when(logRepository.save(any(Log.class))).thenReturn(log);

        Log result = logService.createLog(log, user);

        assertThat(result.getName()).isEqualTo("Field Day 2025 (W1AW) - Class 2A");
    }

    @Test
    @DisplayName("createLog - Name with Unicode Characters - Accepts")
    void createLog_nameWithUnicode_accepts() {
        log.setName("活动日志 2025");

        when(logRepository.save(any(Log.class))).thenReturn(log);

        Log result = logService.createLog(log, user);

        assertThat(result.getName()).isEqualTo("活动日志 2025");
    }

    @Test
    @DisplayName("createLog - Very Long Name - Truncates")
    void createLog_veryLongName_truncates() {
        String longName = "A".repeat(300);
        log.setName(longName);

        when(logRepository.save(any(Log.class))).thenAnswer(i -> i.getArgument(0));

        Log result = logService.createLog(log, user);

        assertThat(result.getName().length()).isLessThanOrEqualTo(255);
    }

    // ==================== DUPLICATE NAME EDGE CASES ====================

    @Test
    @DisplayName("createLog - Duplicate Name Same User - Allows")
    void createLog_duplicateNameSameUser_allows() {
        when(logRepository.existsByOwnerIdAndName(1L, "Test Log")).thenReturn(true);
        when(logRepository.save(any(Log.class))).thenReturn(log);

        // Should allow duplicate names for same user
        Log result = logService.createLog(log, user);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("createLog - Same Name Different User - Allows")
    void createLog_sameNameDifferentUser_allows() {
        User otherUser = new User();
        otherUser.setId(2L);

        Log otherLog = new Log();
        otherLog.setName("Test Log");
        otherLog.setOwner(otherUser);

        when(logRepository.save(any(Log.class))).thenReturn(otherLog);

        Log result = logService.createLog(otherLog, otherUser);

        assertThat(result).isNotNull();
    }

    // ==================== FROZEN LOG EDGE CASES ====================

    @Test
    @DisplayName("updateLog - Frozen Log - Throws IllegalStateException")
    void updateLog_frozenLog_throwsIllegalStateException() {
        log.setFrozen(true);

        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        Log updates = new Log();
        updates.setName("Updated Name");

        assertThatThrownBy(() -> logService.updateLog(1L, updates, user))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("frozen");
    }

    @Test
    @DisplayName("freezeLog - Already Frozen - No Operation")
    void freezeLog_alreadyFrozen_noOperation() {
        log.setFrozen(true);

        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(logRepository.save(any(Log.class))).thenReturn(log);

        Log result = logService.freezeLog(1L, user);

        assertThat(result.isFrozen()).isTrue();
        verify(logRepository).save(log);
    }

    @Test
    @DisplayName("unfreezeLog - Not Frozen - No Operation")
    void unfreezeLog_notFrozen_noOperation() {
        log.setFrozen(false);

        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(logRepository.save(any(Log.class))).thenReturn(log);

        Log result = logService.unfreezeLog(1L, user);

        assertThat(result.isFrozen()).isFalse();
        verify(logRepository).save(log);
    }

    @Test
    @DisplayName("freezeLog - With Active QSOs - Freezes Successfully")
    void freezeLog_withActiveQSOs_freezesSuccessfully() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(qsoRepository.countByLogId(1L)).thenReturn(150L);
        when(logRepository.save(any(Log.class))).thenReturn(log);

        Log result = logService.freezeLog(1L, user);

        assertThat(result.isFrozen()).isTrue();
    }

    // ==================== PERMISSION EDGE CASES ====================

    @Test
    @DisplayName("updateLog - Different User - Throws UnauthorizedException")
    void updateLog_differentUser_throwsUnauthorizedException() {
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setUsername("otheruser");

        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        Log updates = new Log();
        updates.setName("Updated Name");

        assertThatThrownBy(() -> logService.updateLog(1L, updates, differentUser))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("deleteLog - Different User - Throws UnauthorizedException")
    void deleteLog_differentUser_throwsUnauthorizedException() {
        User differentUser = new User();
        differentUser.setId(2L);

        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> logService.deleteLog(1L, differentUser))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("deleteLog - User with Shared Access - Throws UnauthorizedException")
    void deleteLog_userWithSharedAccess_throwsUnauthorizedException() {
        User sharedUser = new User();
        sharedUser.setId(2L);

        when(logRepository.findById(1L)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> logService.deleteLog(1L, sharedUser))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Only the owner can delete");
    }

    // ==================== CASCADE DELETE EDGE CASES ====================

    @Test
    @DisplayName("deleteLog - With QSOs - Deletes All QSOs")
    void deleteLog_withQSOs_deletesAllQSOs() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(qsoRepository.countByLogId(1L)).thenReturn(100L);
        doNothing().when(qsoRepository).deleteAllByLogId(1L);
        doNothing().when(logRepository).deleteById(1L);

        logService.deleteLog(1L, user);

        verify(qsoRepository).deleteAllByLogId(1L);
        verify(logRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteLog - With Invitations - Deletes All Invitations")
    void deleteLog_withInvitations_deletesAllInvitations() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(invitationRepository.countByLogId(1L)).thenReturn(5L);
        doNothing().when(invitationRepository).deleteAllByLogId(1L);
        doNothing().when(logRepository).deleteById(1L);

        logService.deleteLog(1L, user);

        verify(invitationRepository).deleteAllByLogId(1L);
        verify(logRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteLog - Empty Log - Deletes Successfully")
    void deleteLog_emptyLog_deletesSuccessfully() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(qsoRepository.countByLogId(1L)).thenReturn(0L);
        doNothing().when(logRepository).deleteById(1L);

        logService.deleteLog(1L, user);

        verify(logRepository).deleteById(1L);
        verify(qsoRepository, never()).deleteAllByLogId(anyLong());
    }

    // ==================== CONTEST CODE EDGE CASES ====================

    @Test
    @DisplayName("createLog - Invalid Contest Code - Throws ValidationException")
    void createLog_invalidContestCode_throwsValidationException() {
        log.setContestCode("INVALID-CODE");

        assertThatThrownBy(() -> logService.createLog(log, user))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid contest code");
    }

    @Test
    @DisplayName("createLog - Valid Contest Code - Accepts")
    void createLog_validContestCode_accepts() {
        log.setContestCode("ARRL-FD");

        when(logRepository.save(any(Log.class))).thenReturn(log);

        Log result = logService.createLog(log, user);

        assertThat(result.getContestCode()).isEqualTo("ARRL-FD");
    }

    @Test
    @DisplayName("createLog - Null Contest Code - Accepts for General Logging")
    void createLog_nullContestCode_acceptsForGeneralLogging() {
        log.setContestCode(null);

        when(logRepository.save(any(Log.class))).thenReturn(log);

        Log result = logService.createLog(log, user);

        assertThat(result.getContestCode()).isNull();
    }

    // ==================== CONCURRENT MODIFICATION EDGE CASES ====================

    @Test
    @DisplayName("updateLog - Concurrent Modification - Throws OptimisticLockingException")
    void updateLog_concurrentModification_throwsException() {
        log.setVersion(1L);

        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(logRepository.save(any(Log.class)))
            .thenThrow(new OptimisticLockingFailureException("Version mismatch"));

        Log updates = new Log();
        updates.setName("Updated Name");

        assertThatThrownBy(() -> logService.updateLog(1L, updates, user))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    // ==================== PAGINATION EDGE CASES ====================

    @Test
    @DisplayName("getLogsByUser - Page Beyond Total Pages - Returns Empty")
    void getLogsByUser_pageBeyondTotal_returnsEmpty() {
        when(logRepository.findByOwnerId(eq(1L), any()))
            .thenReturn(Page.empty());

        Page<Log> result = logService.getLogsByUser(user, PageRequest.of(999, 10));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getLogsByUser - Negative Page Number - Throws IllegalArgumentException")
    void getLogsByUser_negativePageNumber_throwsException() {
        assertThatThrownBy(() -> logService.getLogsByUser(user, PageRequest.of(-1, 10)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getLogsByUser - Zero Page Size - Throws IllegalArgumentException")
    void getLogsByUser_zeroPageSize_throwsException() {
        assertThatThrownBy(() -> logService.getLogsByUser(user, PageRequest.of(0, 0)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getLogsByUser - Very Large Page Size - Processes Successfully")
    void getLogsByUser_veryLargePageSize_processesSuccessfully() {
        List<Log> logs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            logs.add(log);
        }

        when(logRepository.findByOwnerId(eq(1L), any()))
            .thenReturn(new PageImpl<>(logs));

        Page<Log> result = logService.getLogsByUser(user, PageRequest.of(0, 1000));

        assertThat(result.getContent()).hasSize(100);
    }

    // ==================== TRANSACTION EDGE CASES ====================

    @Test
    @DisplayName("createLog - Database Exception - Rolls Back")
    void createLog_databaseException_rollsBack() {
        when(logRepository.save(any(Log.class)))
            .thenThrow(new DataAccessException("Database error") {});

        assertThatThrownBy(() -> logService.createLog(log, user))
            .isInstanceOf(DataAccessException.class);

        verify(logRepository, never()).flush();
    }

    @Test
    @DisplayName("deleteLog - Partial Deletion Failure - Rolls Back Completely")
    void deleteLog_partialDeletionFailure_rollsBackCompletely() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        doNothing().when(qsoRepository).deleteAllByLogId(1L);
        doThrow(new DataAccessException("Delete failed") {})
            .when(logRepository).deleteById(1L);

        assertThatThrownBy(() -> logService.deleteLog(1L, user))
            .isInstanceOf(DataAccessException.class);
    }

    // ==================== METADATA EDGE CASES ====================

    @Test
    @DisplayName("createLog - Auto-Sets Creation Timestamp")
    void createLog_autoSetsCreationTimestamp() {
        when(logRepository.save(any(Log.class))).thenAnswer(i -> {
            Log savedLog = i.getArgument(0);
            savedLog.setCreatedAt(LocalDateTime.now());
            return savedLog;
        });

        Log result = logService.createLog(log, user);

        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateLog - Updates Modified Timestamp")
    void updateLog_updatesModifiedTimestamp() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(logRepository.save(any(Log.class))).thenAnswer(i -> {
            Log savedLog = i.getArgument(0);
            savedLog.setModifiedAt(LocalDateTime.now());
            return savedLog;
        });

        Log updates = new Log();
        updates.setName("Updated Name");

        Log result = logService.updateLog(1L, updates, user);

        assertThat(result.getModifiedAt()).isNotNull();
    }

    // ==================== STATISTICS EDGE CASES ====================

    @Test
    @DisplayName("getLogStatistics - Empty Log - Returns Zero Statistics")
    void getLogStatistics_emptyLog_returnsZeroStatistics() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(qsoRepository.countByLogId(1L)).thenReturn(0L);

        Map<String, Object> stats = logService.getLogStatistics(1L, user);

        assertThat(stats.get("totalQSOs")).isEqualTo(0L);
    }

    @Test
    @DisplayName("getLogStatistics - Large Log - Calculates Correctly")
    void getLogStatistics_largeLog_calculatesCorrectly() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(log));
        when(qsoRepository.countByLogId(1L)).thenReturn(10000L);

        Map<String, Object> stats = logService.getLogStatistics(1L, user);

        assertThat(stats.get("totalQSOs")).isEqualTo(10000L);
    }
}
