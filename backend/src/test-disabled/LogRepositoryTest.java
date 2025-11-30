package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Log Repository Integration Tests")
class LogRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        logRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = userRepository.save(TestDataBuilder.aValidUser().username("user1").email("user1@test.com").build());
        testUser2 = userRepository.save(TestDataBuilder.aValidUser().username("user2").email("user2@test.com").build());
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid Log - Persists Successfully")
    void save_validLog_persistsSuccessfully() {
        // Arrange
        Log log = TestDataBuilder.aValidLog(testUser1).build();

        // Act
        Log saved = logRepository.save(log);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Contest Log 2025");
        assertThat(saved.getCreatedBy()).isEqualTo(testUser1);
    }

    @Test
    @DisplayName("findById - Existing Log - Returns Log")
    void findById_existingLog_returnsLog() {
        // Arrange
        Log log = logRepository.save(TestDataBuilder.aValidLog(testUser1).build());

        // Act
        Optional<Log> found = logRepository.findById(log.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Contest Log 2025");
    }

    // ==================== FIND BY USER TESTS ====================

    @Test
    @DisplayName("findByCreatedById - Returns Logs Created by User")
    void findByCreatedById_returnsLogsCreatedByUser() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Log 1").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Log 2").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser2).name("Log 3").build());

        // Act
        List<Log> logs = logRepository.findByCreatedById(testUser1.getId());

        // Assert
        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> log.getCreatedBy().equals(testUser1));
    }

    @Test
    @DisplayName("findByCreatedById - Paginated - Returns Page of Logs")
    void findByCreatedById_paginated_returnsPageOfLogs() {
        // Arrange
        for (int i = 1; i <= 5; i++) {
            logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Log " + i).build());
        }

        // Act
        Page<Log> page = logRepository.findByCreatedById(testUser1.getId(), PageRequest.of(0, 3));

        // Assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("findByNameContainingIgnoreCase - Returns Matching Logs")
    void findByNameContainingIgnoreCase_returnsMatchingLogs() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Field Day 2025").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Winter Field Day 2025").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("POTA Activation").build());

        // Act
        List<Log> results = logRepository.findByNameContainingIgnoreCase("field");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(log -> log.getName().toLowerCase().contains("field"));
    }

    @Test
    @DisplayName("findByCreatedByIdAndNameContaining - Returns User's Matching Logs")
    void findByCreatedByIdAndNameContaining_returnsUsersMatchingLogs() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Contest 2025").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Contest 2024").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser2).name("Contest 2025").build());

        // Act
        List<Log> results = logRepository.findByCreatedByIdAndNameContaining(testUser1.getId(), "2025");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Contest 2025");
        assertThat(results.get(0).getCreatedBy()).isEqualTo(testUser1);
    }

    // ==================== FREEZE STATUS TESTS ====================

    @Test
    @DisplayName("findByFrozenTrue - Returns Only Frozen Logs")
    void findByFrozenTrue_returnsOnlyFrozenLogs() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Frozen Log").frozen(true).build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("Active Log").frozen(false).build());

        // Act
        List<Log> frozenLogs = logRepository.findByFrozenTrue();

        // Assert
        assertThat(frozenLogs).hasSize(1);
        assertThat(frozenLogs.get(0).getName()).isEqualTo("Frozen Log");
        assertThat(frozenLogs.get(0).isFrozen()).isTrue();
    }

    @Test
    @DisplayName("findByCreatedByIdAndFrozen - Returns User's Logs by Freeze Status")
    void findByCreatedByIdAndFrozen_returnsUsersLogsByFreezeStatus() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("User1 Frozen").frozen(true).build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("User1 Active").frozen(false).build());
        logRepository.save(TestDataBuilder.aValidLog(testUser2).name("User2 Frozen").frozen(true).build());

        // Act
        List<Log> user1FrozenLogs = logRepository.findByCreatedByIdAndFrozen(testUser1.getId(), true);
        List<Log> user1ActiveLogs = logRepository.findByCreatedByIdAndFrozen(testUser1.getId(), false);

        // Assert
        assertThat(user1FrozenLogs).hasSize(1);
        assertThat(user1FrozenLogs.get(0).getName()).isEqualTo("User1 Frozen");
        assertThat(user1ActiveLogs).hasSize(1);
        assertThat(user1ActiveLogs.get(0).getName()).isEqualTo("User1 Active");
    }

    // ==================== CONTEST TESTS ====================

    @Test
    @DisplayName("findByContestCode - Returns Logs for Contest")
    void findByContestCode_returnsLogsForContest() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).contestCode("ARRL-FD").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).contestCode("ARRL-FD").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).contestCode("WFD").build());

        // Act
        List<Log> arrlFdLogs = logRepository.findByContestCode("ARRL-FD");

        // Assert
        assertThat(arrlFdLogs).hasSize(2);
        assertThat(arrlFdLogs).allMatch(log -> log.getContestCode().equals("ARRL-FD"));
    }

    @Test
    @DisplayName("findByContestCodeAndCreatedById - Returns User's Contest Logs")
    void findByContestCodeAndCreatedById_returnsUsersContestLogs() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).contestCode("ARRL-FD").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser2).contestCode("ARRL-FD").build());

        // Act
        List<Log> logs = logRepository.findByContestCodeAndCreatedById("ARRL-FD", testUser1.getId());

        // Assert
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCreatedBy()).isEqualTo(testUser1);
    }

    // ==================== DATE RANGE TESTS ====================

    @Test
    @DisplayName("findByCreatedAtBetween - Returns Logs Created in Date Range")
    void findByCreatedAtBetween_returnsLogsCreatedInDateRange() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Log log1 = TestDataBuilder.aValidLog(testUser1).build();
        log1.setCreatedAt(now.minusDays(10));
        logRepository.save(log1);

        Log log2 = TestDataBuilder.aValidLog(testUser1).build();
        log2.setCreatedAt(now.minusDays(5));
        logRepository.save(log2);

        Log log3 = TestDataBuilder.aValidLog(testUser1).build();
        log3.setCreatedAt(now.minusDays(1));
        logRepository.save(log3);

        // Act
        List<Log> logs = logRepository.findByCreatedAtBetween(
                now.minusDays(7),
                now
        );

        // Assert
        assertThat(logs).hasSize(2);
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("countByCreatedById - Returns Count of User's Logs")
    void countByCreatedById_returnsCountOfUsersLogs() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).build());
        logRepository.save(TestDataBuilder.aValidLog(testUser2).build());

        // Act
        long count = logRepository.countByCreatedById(testUser1.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByContestCode - Returns Count of Contest Logs")
    void countByContestCode_returnsCountOfContestLogs() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).contestCode("ARRL-FD").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).contestCode("ARRL-FD").build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).contestCode("WFD").build());

        // Act
        long count = logRepository.countByContestCode("ARRL-FD");

        // Assert
        assertThat(count).isEqualTo(2);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes Log")
    void deleteById_removesLog() {
        // Arrange
        Log log = logRepository.save(TestDataBuilder.aValidLog(testUser1).build());
        Long logId = log.getId();

        // Act
        logRepository.deleteById(logId);

        // Assert
        assertThat(logRepository.findById(logId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByCreatedById - Removes All User's Logs")
    void deleteByCreatedById_removesAllUsersLogs() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).build());
        logRepository.save(TestDataBuilder.aValidLog(testUser1).build());
        logRepository.save(TestDataBuilder.aValidLog(testUser2).build());

        // Act
        logRepository.deleteByCreatedById(testUser1.getId());

        // Assert
        assertThat(logRepository.countByCreatedById(testUser1.getId())).isZero();
        assertThat(logRepository.countByCreatedById(testUser2.getId())).isEqualTo(1);
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("existsByCreatedByIdAndName - Returns True if Log Exists")
    void existsByCreatedByIdAndName_returnsTrueIfLogExists() {
        // Arrange
        logRepository.save(TestDataBuilder.aValidLog(testUser1).name("My Log").build());

        // Act
        boolean exists = logRepository.existsByCreatedByIdAndName(testUser1.getId(), "My Log");
        boolean notExists = logRepository.existsByCreatedByIdAndName(testUser1.getId(), "Other Log");

        // Assert
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // ==================== SORTING TESTS ====================

    @Test
    @DisplayName("findByCreatedById - Returns Logs Sorted by Created Date Descending")
    void findByCreatedById_returnsLogsSortedByCreatedDateDescending() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        Log log1 = TestDataBuilder.aValidLog(testUser1).name("Oldest").build();
        log1.setCreatedAt(now.minusDays(10));
        logRepository.save(log1);

        Log log2 = TestDataBuilder.aValidLog(testUser1).name("Middle").build();
        log2.setCreatedAt(now.minusDays(5));
        logRepository.save(log2);

        Log log3 = TestDataBuilder.aValidLog(testUser1).name("Newest").build();
        log3.setCreatedAt(now);
        logRepository.save(log3);

        // Act
        List<Log> logs = logRepository.findByCreatedById(testUser1.getId());

        // Assert
        assertThat(logs).hasSize(3);
        assertThat(logs.get(0).getName()).isEqualTo("Newest");
        assertThat(logs.get(1).getName()).isEqualTo("Middle");
        assertThat(logs.get(2).getName()).isEqualTo("Oldest");
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("save - Update Existing Log - Updates Successfully")
    void save_updateExistingLog_updatesSuccessfully() {
        // Arrange
        Log log = logRepository.save(TestDataBuilder.aValidLog(testUser1).build());
        Long logId = log.getId();

        // Act
        log.setName("Updated Name");
        log.setFrozen(true);
        logRepository.save(log);

        // Assert
        Log updated = logRepository.findById(logId).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.isFrozen()).isTrue();
    }
}
