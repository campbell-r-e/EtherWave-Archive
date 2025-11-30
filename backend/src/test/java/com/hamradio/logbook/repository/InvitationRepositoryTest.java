package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Invitation;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Invitation Repository Integration Tests")
class InvitationRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UserRepository userRepository;

    private User inviter;
    private User invitee1;
    private User invitee2;
    private Log testLog;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
        logRepository.deleteAll();
        userRepository.deleteAll();

        inviter = userRepository.save(TestDataBuilder.aValidUser().username("inviter").email("inviter@test.com").callsign("W1INV").build());
        invitee1 = userRepository.save(TestDataBuilder.aValidUser().username("invitee1").email("invitee1@test.com").callsign("W1IV1").build());
        invitee2 = userRepository.save(TestDataBuilder.aValidUser().username("invitee2").email("invitee2@test.com").callsign("W1IV2").build());
        testLog = logRepository.save(TestDataBuilder.aValidLog(inviter).build());
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid Invitation - Persists Successfully")
    void save_validInvitation_persistsSuccessfully() {
        // Arrange
        Invitation invitation = Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        // Act
        Invitation saved = invitationRepository.save(invitation);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLog()).isEqualTo(testLog);
        assertThat(saved.getInvitedUser()).isEqualTo(invitee1);
        assertThat(saved.getProposedRole()).isEqualTo("STATION");
    }

    @Test
    @DisplayName("findById - Existing Invitation - Returns Invitation")
    void findById_existingInvitation_returnsInvitation() {
        // Arrange
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());

        // Act
        Optional<Invitation> found = invitationRepository.findById(invitation.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getInvitedUser()).isEqualTo(invitee1);
    }

    // ==================== FIND BY USER TESTS ====================

    @Test
    @DisplayName("findByInvitee - Returns Invitations for User")
    void findByInvitee_returnsInvitationsForUser() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());

        // Act
        List<Invitation> invitations = invitationRepository.findByInvitee(invitee1.getId());

        // Assert
        assertThat(invitations).hasSize(2);
        assertThat(invitations).allMatch(inv -> inv.getInvitedUser().equals(invitee1));
    }

    @Test
    @DisplayName("findByInviter - Returns Invitations Created by User")
    void findByInviter_returnsInvitationsCreatedByUser() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());

        // Act
        List<Invitation> invitations = invitationRepository.findByInviter(inviter.getId());

        // Assert
        assertThat(invitations).hasSize(2);
        assertThat(invitations).allMatch(inv -> inv.getInvitedBy().equals(inviter));
    }

    // ==================== FIND BY LOG TESTS ====================

    @Test
    @DisplayName("findByLogId - Returns Invitations for Log")
    void findByLogId_returnsInvitationsForLog() {
        // Arrange
        Log anotherLog = logRepository.save(TestDataBuilder.aValidLog(inviter).name("Another Log").build());

        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(anotherLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());

        // Act
        List<Invitation> invitations = invitationRepository.findByLog(testLog.getId());

        // Assert
        assertThat(invitations).hasSize(2);
        assertThat(invitations).allMatch(inv -> inv.getLog().equals(testLog));
    }

    // ==================== STATUS FILTER TESTS ====================

    @Test
    @DisplayName("findByStatus - Returns Invitations by Status")
    void findByStatus_returnsInvitationsByStatus() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("ACCEPTED")
                .build());

        // Act
        List<Invitation> pending = invitationRepository.findByStatus("PENDING");
        List<Invitation> accepted = invitationRepository.findByStatus("ACCEPTED");

        // Assert
        assertThat(pending).hasSize(1);
        assertThat(accepted).hasSize(1);
    }

    @Test
    @DisplayName("findByInviteeAndStatus - Returns User's Invitations by Status")
    void findByInviteeAndStatus_returnsUsersInvitationsByStatus() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("ACCEPTED")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());

        // Act
        List<Invitation> user1Pending = invitationRepository.findByInviteeAndStatus(invitee1.getId(), "PENDING");

        // Assert
        assertThat(user1Pending).hasSize(1);
        assertThat(user1Pending.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("findByLogIdAndStatus - Returns Log's Invitations by Status")
    void findByLogIdAndStatus_returnsLogsInvitationsByStatus() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());

        // Act
        List<Invitation> logPending = invitationRepository.findByLogIdAndStatus(testLog.getId(), "PENDING");

        // Assert
        assertThat(logPending).hasSize(2);
    }

    // ==================== EXPIRATION TESTS ====================

    @Test
    @DisplayName("findByExpiresAtBefore - Returns Expired Invitations")
    void findByExpiresAtBefore_returnsExpiredInvitations() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .expiresAt(now.minusDays(1))
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .expiresAt(now.plusDays(1))
                .build());

        // Act
        List<Invitation> expired = invitationRepository.findByExpiresAtBefore(now);

        // Assert
        assertThat(expired).hasSize(1);
    }

    @Test
    @DisplayName("findByStatusAndExpiresAtBefore - Returns Expired Pending Invitations")
    void findByStatusAndExpiresAtBefore_returnsExpiredPendingInvitations() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .expiresAt(now.minusDays(1))
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("ACCEPTED")
                .expiresAt(now.minusDays(1))
                .build());

        // Act
        List<Invitation> expiredPending = invitationRepository.findByStatusAndExpiresAtBefore("PENDING", now);

        // Assert
        assertThat(expiredPending).hasSize(1);
        assertThat(expiredPending.get(0).getStatus()).isEqualTo("PENDING");
    }

    // ==================== ROLE TESTS ====================

    @Test
    @DisplayName("findByRole - Returns Invitations by Role")
    void findByRole_returnsInvitationsByRole() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());

        // Act
        List<Invitation> stationInvitations = invitationRepository.findByRole("STATION");
        List<Invitation> viewerInvitations = invitationRepository.findByRole("VIEWER");

        // Assert
        assertThat(stationInvitations).hasSize(1);
        assertThat(viewerInvitations).hasSize(1);
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("countByLogId - Returns Count of Invitations for Log")
    void countByLogId_returnsCountOfInvitationsForLog() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());

        // Act
        long count = invitationRepository.countByLogId(testLog.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByInvitedUserIdAndStatus - Returns Count by User and Status")
    void countByInvitedUserIdAndStatus_returnsCountByUserAndStatus() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("ACCEPTED")
                .build());

        // Act
        long pendingCount = invitationRepository.countByInvitedUserIdAndStatus(invitee1.getId(), "PENDING");

        // Assert
        assertThat(pendingCount).isEqualTo(2);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes Invitation")
    void deleteById_removesInvitation() {
        // Arrange
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        Long invitationId = invitation.getId();

        // Act
        invitationRepository.deleteById(invitationId);

        // Assert
        assertThat(invitationRepository.findById(invitationId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByLogId - Removes All Invitations for Log")
    void deleteByLogId_removesAllInvitationsForLog() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee2)
                .inviter(inviter)
                .proposedRole("VIEWER")
                .status("PENDING")
                .build());

        // Act
        invitationRepository.deleteByLogId(testLog.getId());

        // Assert
        assertThat(invitationRepository.findByLog(testLog.getId())).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("save - Update Invitation Status - Updates Successfully")
    void save_updateInvitationStatus_updatesSuccessfully() {
        // Arrange
        Invitation invitation = invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());
        Long invitationId = invitation.getId();

        // Act
        invitation.setStatus("ACCEPTED");
        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        // Assert
        Invitation updated = invitationRepository.findById(invitationId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("ACCEPTED");
        assertThat(updated.getRespondedAt()).isNotNull();
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("existsByLogIdAndInvitedUserId - Returns True if Invitation Exists")
    void existsByLogIdAndInvitedUserId_returnsTrueIfInvitationExists() {
        // Arrange
        invitationRepository.save(Invitation.builder()
                .log(testLog)
                .invitee(invitee1)
                .inviter(inviter)
                .proposedRole("STATION")
                .status("PENDING")
                .build());

        // Act
        boolean exists = invitationRepository.existsByLogIdAndInvitedUserId(testLog.getId(), invitee1.getId());
        boolean notExists = invitationRepository.existsByLogIdAndInvitedUserId(testLog.getId(), invitee2.getId());

        // Assert
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
