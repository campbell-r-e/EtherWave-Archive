package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Log Participant Repository Integration Tests")
class LogParticipantRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private LogParticipantRepository logParticipantRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;
    private User participant1;
    private User participant2;
    private Log testLog1;
    private Log testLog2;

    @BeforeEach
    void setUp() {
        logParticipantRepository.deleteAll();
        logRepository.deleteAll();
        userRepository.deleteAll();

        creator = userRepository.save(TestDataBuilder.aValidUser().username("creator").email("creator@test.com").callsign("W1CRT").build());
        participant1 = userRepository.save(TestDataBuilder.aValidUser().username("participant1").email("p1@test.com").callsign("W1PT1").build());
        participant2 = userRepository.save(TestDataBuilder.aValidUser().username("participant2").email("p2@test.com").callsign("W1PT2").build());

        testLog1 = logRepository.save(TestDataBuilder.aValidLog(creator).name("Log 1").build());
        testLog2 = logRepository.save(TestDataBuilder.aValidLog(creator).name("Log 2").build());
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid Participant - Persists Successfully")
    void save_validParticipant_persistsSuccessfully() {
        // Arrange
        LogParticipant participant = LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build();

        // Act
        LogParticipant saved = logParticipantRepository.save(participant);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLog()).isEqualTo(testLog1);
        assertThat(saved.getUser()).isEqualTo(participant1);
        assertThat(saved.getRole()).isEqualTo("STATION");
    }

    @Test
    @DisplayName("findById - Existing Participant - Returns Participant")
    void findById_existingParticipant_returnsParticipant() {
        // Arrange
        LogParticipant participant = logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());

        // Act
        Optional<LogParticipant> found = logParticipantRepository.findById(participant.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isEqualTo(participant1);
    }

    // ==================== FIND BY LOG TESTS ====================

    @Test
    @DisplayName("findByLogId - Returns All Participants for Log")
    void findByLogId_returnsAllParticipantsForLog() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("VIEWER")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog2)
                .user(participant1)
                .role("STATION")
                .build());

        // Act
        List<LogParticipant> participants = logParticipantRepository.findByLogId(testLog1.getId());

        // Assert
        assertThat(participants).hasSize(2);
        assertThat(participants).allMatch(p -> p.getLog().equals(testLog1));
    }

    @Test
    @DisplayName("findByLogIdAndRole - Returns Participants by Role")
    void findByLogIdAndRole_returnsParticipantsByRole() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("VIEWER")
                .build());

        // Act
        List<LogParticipant> stationParticipants = logParticipantRepository.findByLogIdAndRole(testLog1.getId(), "STATION");
        List<LogParticipant> viewerParticipants = logParticipantRepository.findByLogIdAndRole(testLog1.getId(), "VIEWER");

        // Assert
        assertThat(stationParticipants).hasSize(1);
        assertThat(stationParticipants.get(0).getUser()).isEqualTo(participant1);
        assertThat(viewerParticipants).hasSize(1);
        assertThat(viewerParticipants.get(0).getUser()).isEqualTo(participant2);
    }

    // ==================== FIND BY USER TESTS ====================

    @Test
    @DisplayName("findByUserId - Returns All Logs for User")
    void findByUserId_returnsAllLogsForUser() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog2)
                .user(participant1)
                .role("VIEWER")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("STATION")
                .build());

        // Act
        List<LogParticipant> participations = logParticipantRepository.findByUserId(participant1.getId());

        // Assert
        assertThat(participations).hasSize(2);
        assertThat(participations).allMatch(p -> p.getUser().equals(participant1));
    }

    @Test
    @DisplayName("findByUserIdAndRole - Returns User's Participations by Role")
    void findByUserIdAndRole_returnsUsersParticipationsByRole() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog2)
                .user(participant1)
                .role("VIEWER")
                .build());

        // Act
        List<LogParticipant> stationRoles = logParticipantRepository.findByUserIdAndRole(participant1.getId(), "STATION");
        List<LogParticipant> viewerRoles = logParticipantRepository.findByUserIdAndRole(participant1.getId(), "VIEWER");

        // Assert
        assertThat(stationRoles).hasSize(1);
        assertThat(viewerRoles).hasSize(1);
    }

    // ==================== FIND BY LOG AND USER TESTS ====================

    @Test
    @DisplayName("findByLogIdAndUserId - Returns Specific Participation")
    void findByLogIdAndUserId_returnsSpecificParticipation() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());

        // Act
        Optional<LogParticipant> found = logParticipantRepository.findByLogIdAndUserId(testLog1.getId(), participant1.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getLog()).isEqualTo(testLog1);
        assertThat(found.get().getUser()).isEqualTo(participant1);
    }

    @Test
    @DisplayName("findByLogIdAndUserId - Non-Existent Participation - Returns Empty")
    void findByLogIdAndUserId_nonExistentParticipation_returnsEmpty() {
        // Act
        Optional<LogParticipant> found = logParticipantRepository.findByLogIdAndUserId(testLog1.getId(), participant1.getId());

        // Assert
        assertThat(found).isEmpty();
    }

    // ==================== ROLE TESTS ====================

    @Test
    @DisplayName("findByRole - Returns All Participants with Role")
    void findByRole_returnsAllParticipantsWithRole() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog2)
                .user(participant2)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("VIEWER")
                .build());

        // Act
        List<LogParticipant> stationParticipants = logParticipantRepository.findByRole("STATION");

        // Assert
        assertThat(stationParticipants).hasSize(2);
        assertThat(stationParticipants).allMatch(p -> p.getRole().equals("STATION"));
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("countByLogId - Returns Count of Participants for Log")
    void countByLogId_returnsCountOfParticipantsForLog() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("VIEWER")
                .build());

        // Act
        long count = logParticipantRepository.countByLogId(testLog1.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByUserId - Returns Count of User's Participations")
    void countByUserId_returnsCountOfUsersParticipations() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog2)
                .user(participant1)
                .role("VIEWER")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("STATION")
                .build());

        // Act
        long count = logParticipantRepository.countByUserId(participant1.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByLogIdAndRole - Returns Count by Role")
    void countByLogIdAndRole_returnsCountByRole() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(creator)
                .role("VIEWER")
                .build());

        // Act
        long stationCount = logParticipantRepository.countByLogIdAndRole(testLog1.getId(), "STATION");
        long viewerCount = logParticipantRepository.countByLogIdAndRole(testLog1.getId(), "VIEWER");

        // Assert
        assertThat(stationCount).isEqualTo(2);
        assertThat(viewerCount).isEqualTo(1);
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("existsByLogIdAndUserId - Returns True if Participation Exists")
    void existsByLogIdAndUserId_returnsTrueIfParticipationExists() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());

        // Act
        boolean exists = logParticipantRepository.existsByLogIdAndUserId(testLog1.getId(), participant1.getId());
        boolean notExists = logParticipantRepository.existsByLogIdAndUserId(testLog1.getId(), participant2.getId());

        // Assert
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes Participant")
    void deleteById_removesParticipant() {
        // Arrange
        LogParticipant participant = logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        Long participantId = participant.getId();

        // Act
        logParticipantRepository.deleteById(participantId);

        // Assert
        assertThat(logParticipantRepository.findById(participantId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByLogIdAndUserId - Removes Specific Participation")
    void deleteByLogIdAndUserId_removesSpecificParticipation() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());

        // Act
        logParticipantRepository.deleteByLogIdAndUserId(testLog1.getId(), participant1.getId());

        // Assert
        assertThat(logParticipantRepository.findByLogIdAndUserId(testLog1.getId(), participant1.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteByLogId - Removes All Participants for Log")
    void deleteByLogId_removesAllParticipantsForLog() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant2)
                .role("VIEWER")
                .build());

        // Act
        logParticipantRepository.deleteByLogId(testLog1.getId());

        // Assert
        assertThat(logParticipantRepository.findByLogId(testLog1.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteByUserId - Removes All Participations for User")
    void deleteByUserId_removesAllParticipationsForUser() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog2)
                .user(participant1)
                .role("VIEWER")
                .build());

        // Act
        logParticipantRepository.deleteByUserId(participant1.getId());

        // Assert
        assertThat(logParticipantRepository.findByUserId(participant1.getId())).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("save - Update Participant Role - Updates Successfully")
    void save_updateParticipantRole_updatesSuccessfully() {
        // Arrange
        LogParticipant participant = logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("VIEWER")
                .build());
        Long participantId = participant.getId();

        // Act
        participant.setRole("STATION");
        logParticipantRepository.save(participant);

        // Assert
        LogParticipant updated = logParticipantRepository.findById(participantId).orElseThrow();
        assertThat(updated.getRole()).isEqualTo("STATION");
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("save - Duplicate Log and User - Throws Exception")
    void save_duplicateLogAndUser_throwsException() {
        // Arrange
        logParticipantRepository.save(LogParticipant.builder()
                .log(testLog1)
                .user(participant1)
                .role("STATION")
                .build());

        // Act & Assert
        assertThatThrownBy(() -> {
            logParticipantRepository.save(LogParticipant.builder()
                    .log(testLog1)
                    .user(participant1)
                    .role("VIEWER")
                    .build());
        }).isInstanceOf(Exception.class);
    }
}
