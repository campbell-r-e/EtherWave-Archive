package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Invitation;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.InvitationRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Invitation Service Tests")
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private LogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogParticipantRepository logParticipantRepository;

    @InjectMocks
    private InvitationService invitationService;

    private User creator;
    private User invitedUser;
    private Log testLog;
    private Invitation testInvitation;

    @BeforeEach
    void setUp() {
        creator = TestDataBuilder.aValidUser()
                .id(1L)
                .username("creator")
                .email("creator@example.com")
                .callsign("W1CREATOR")
                .build();

        invitedUser = TestDataBuilder.aValidUser()
                .id(2L)
                .username("invitee")
                .email("invitee@example.com")
                .callsign("W2INVITEE")
                .build();

        testLog = TestDataBuilder.aValidLog(creator)
                .id(1L)
                .logName("Shared Field Day Log")
                .build();

        testInvitation = Invitation.builder()
                .id(1L)
                .log(testLog)
                .invitedUser(invitedUser)
                .invitedBy(creator)
                .role(LogParticipant.ParticipantRole.STATION)
                .status(Invitation.InvitationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    // ==================== CREATE INVITATION TESTS ====================

    @Test
    @DisplayName("createInvitation - Valid Invitation - Creates Successfully")
    void createInvitation_validInvitation_createsSuccessfully() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, creator, LogParticipant.ParticipantRole.CREATOR).build()));
        when(invitationRepository.findByLogIdAndInvitedUserId(1L, 2L))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);

        // Act
        Invitation result = invitationService.createInvitation(1L, 2L, 1L, LogParticipant.ParticipantRole.STATION);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLog()).isEqualTo(testLog);
        assertThat(result.getInvitedUser()).isEqualTo(invitedUser);
        assertThat(result.getRole()).isEqualTo(LogParticipant.ParticipantRole.STATION);
        assertThat(result.getStatus()).isEqualTo(Invitation.InvitationStatus.PENDING);
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    @DisplayName("createInvitation - Log Not Found - Throws Exception")
    void createInvitation_logNotFound_throwsException() {
        // Arrange
        when(logRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.createInvitation(999L, 2L, 1L, LogParticipant.ParticipantRole.STATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Log not found");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInvitation - User Not Found - Throws Exception")
    void createInvitation_userNotFound_throwsException() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.createInvitation(1L, 999L, 1L, LogParticipant.ParticipantRole.STATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInvitation - Not Creator - Throws Exception")
    void createInvitation_notCreator_throwsException() {
        // Arrange
        User nonCreator = TestDataBuilder.aValidUser().id(3L).build();
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(nonCreator));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, nonCreator, LogParticipant.ParticipantRole.VIEWER).build()));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.createInvitation(1L, 2L, 3L, LogParticipant.ParticipantRole.STATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the creator can send invitations");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInvitation - Duplicate Invitation - Throws Exception")
    void createInvitation_duplicateInvitation_throwsException() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, creator, LogParticipant.ParticipantRole.CREATOR).build()));
        when(invitationRepository.findByLogIdAndInvitedUserId(1L, 2L))
                .thenReturn(Optional.of(testInvitation)); // Existing invitation

        // Act & Assert
        assertThatThrownBy(() -> invitationService.createInvitation(1L, 2L, 1L, LogParticipant.ParticipantRole.STATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invitation already exists");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInvitation - User Already Participant - Throws Exception")
    void createInvitation_userAlreadyParticipant_throwsException() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, creator, LogParticipant.ParticipantRole.CREATOR).build()));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 2L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, invitedUser, LogParticipant.ParticipantRole.VIEWER).build()));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.createInvitation(1L, 2L, 1L, LogParticipant.ParticipantRole.STATION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User is already a participant");

        verify(invitationRepository, never()).save(any());
    }

    // ==================== ACCEPT INVITATION TESTS ====================

    @Test
    @DisplayName("acceptInvitation - Valid Pending Invitation - Accepts Successfully")
    void acceptInvitation_validPendingInvitation_acceptsSuccessfully() {
        // Arrange
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);
        when(logParticipantRepository.save(any(LogParticipant.class)))
                .thenReturn(TestDataBuilder.aLogParticipant(testLog, invitedUser, LogParticipant.ParticipantRole.STATION).build());

        // Act
        invitationService.acceptInvitation(1L, 2L);

        // Assert
        verify(invitationRepository).save(argThat(inv ->
                inv.getStatus() == Invitation.InvitationStatus.ACCEPTED &&
                inv.getRespondedAt() != null
        ));
        verify(logParticipantRepository).save(argThat(participant ->
                participant.getUser().equals(invitedUser) &&
                participant.getLog().equals(testLog) &&
                participant.getRole() == LogParticipant.ParticipantRole.STATION
        ));
    }

    @Test
    @DisplayName("acceptInvitation - Invitation Not Found - Throws Exception")
    void acceptInvitation_invitationNotFound_throwsException() {
        // Arrange
        when(invitationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(999L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invitation not found");

        verify(logParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Wrong User - Throws Exception")
    void acceptInvitation_wrongUser_throwsException() {
        // Arrange
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        // Act & Assert - User 3 trying to accept invitation for user 2
        assertThatThrownBy(() -> invitationService.acceptInvitation(1L, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not invited");

        verify(logParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Already Accepted - Throws Exception")
    void acceptInvitation_alreadyAccepted_throwsException() {
        // Arrange
        Invitation acceptedInvitation = Invitation.builder()
                .id(1L)
                .log(testLog)
                .invitedUser(invitedUser)
                .status(Invitation.InvitationStatus.ACCEPTED)
                .build();
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(acceptedInvitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been accepted");

        verify(logParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvitation - Expired Invitation - Throws Exception")
    void acceptInvitation_expiredInvitation_throwsException() {
        // Arrange
        Invitation expiredInvitation = Invitation.builder()
                .id(1L)
                .log(testLog)
                .invitedUser(invitedUser)
                .status(Invitation.InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                .build();
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(expiredInvitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.acceptInvitation(1L, 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        verify(logParticipantRepository, never()).save(any());
    }

    // ==================== DECLINE INVITATION TESTS ====================

    @Test
    @DisplayName("declineInvitation - Valid Pending Invitation - Declines Successfully")
    void declineInvitation_validPendingInvitation_declinesSuccessfully() {
        // Arrange
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);

        // Act
        invitationService.declineInvitation(1L, 2L);

        // Assert
        verify(invitationRepository).save(argThat(inv ->
                inv.getStatus() == Invitation.InvitationStatus.DECLINED &&
                inv.getRespondedAt() != null
        ));
        verify(logParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("declineInvitation - Wrong User - Throws Exception")
    void declineInvitation_wrongUser_throwsException() {
        // Arrange
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.declineInvitation(1L, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not invited");

        verify(invitationRepository, never()).save(any());
    }

    // ==================== CANCEL INVITATION TESTS ====================

    @Test
    @DisplayName("cancelInvitation - Creator Cancels Pending Invitation - Cancels Successfully")
    void cancelInvitation_creatorCancelsPendingInvitation_cancelsSuccessfully() {
        // Arrange
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, creator, LogParticipant.ParticipantRole.CREATOR).build()));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);

        // Act
        invitationService.cancelInvitation(1L, 1L);

        // Assert
        verify(invitationRepository).save(argThat(inv ->
                inv.getStatus() == Invitation.InvitationStatus.CANCELLED
        ));
    }

    @Test
    @DisplayName("cancelInvitation - Non-Creator - Throws Exception")
    void cancelInvitation_nonCreator_throwsException() {
        // Arrange
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, invitedUser, LogParticipant.ParticipantRole.VIEWER).build()));

        // Act & Assert
        assertThatThrownBy(() -> invitationService.cancelInvitation(1L, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the creator can cancel");

        verify(invitationRepository, never()).save(any());
    }

    // ==================== GET INVITATIONS TESTS ====================

    @Test
    @DisplayName("getInvitationsForUser - User Has Pending Invitations - Returns List")
    void getInvitationsForUser_userHasPendingInvitations_returnsList() {
        // Arrange
        when(invitationRepository.findByInvitedUserIdAndStatus(2L, Invitation.InvitationStatus.PENDING))
                .thenReturn(List.of(testInvitation));

        // Act
        List<Invitation> result = invitationService.getInvitationsForUser(2L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvitedUser()).isEqualTo(invitedUser);
    }

    @Test
    @DisplayName("getInvitationsForUser - No Pending Invitations - Returns Empty List")
    void getInvitationsForUser_noPendingInvitations_returnsEmptyList() {
        // Arrange
        when(invitationRepository.findByInvitedUserIdAndStatus(2L, Invitation.InvitationStatus.PENDING))
                .thenReturn(List.of());

        // Act
        List<Invitation> result = invitationService.getInvitationsForUser(2L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getInvitationsForLog - Log Has Invitations - Returns List")
    void getInvitationsForLog_logHasInvitations_returnsList() {
        // Arrange
        when(invitationRepository.findByLogId(1L)).thenReturn(List.of(testInvitation));

        // Act
        List<Invitation> result = invitationService.getInvitationsForLog(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLog()).isEqualTo(testLog);
    }

    // ==================== ROLE VALIDATION TESTS ====================

    @Test
    @DisplayName("createInvitation - Invalid Role - Throws Exception")
    void createInvitation_invalidRole_throwsException() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, creator, LogParticipant.ParticipantRole.CREATOR).build()));

        // Act & Assert - Cannot invite as CREATOR
        assertThatThrownBy(() -> invitationService.createInvitation(1L, 2L, 1L, LogParticipant.ParticipantRole.CREATOR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot invite user as CREATOR");

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInvitation - Valid Roles - Accepts All Valid Roles")
    void createInvitation_validRoles_acceptsAllValidRoles() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, creator, LogParticipant.ParticipantRole.CREATOR).build()));
        when(invitationRepository.findByLogIdAndInvitedUserId(1L, 2L))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);

        // Act & Assert - Should accept STATION, OPERATOR, VIEWER roles
        LogParticipant.ParticipantRole[] validRoles = {
                LogParticipant.ParticipantRole.STATION,
                LogParticipant.ParticipantRole.OPERATOR,
                LogParticipant.ParticipantRole.VIEWER
        };

        for (LogParticipant.ParticipantRole role : validRoles) {
            assertThatCode(() -> invitationService.createInvitation(1L, 2L, 1L, role))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== EXPIRATION TESTS ====================

    @Test
    @DisplayName("createInvitation - Sets Expiration Date - 7 Days from Now")
    void createInvitation_setsExpirationDate_7DaysFromNow() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(invitedUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(logParticipantRepository.findByLogIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(TestDataBuilder.aLogParticipant(testLog, creator, LogParticipant.ParticipantRole.CREATOR).build()));
        when(invitationRepository.findByLogIdAndInvitedUserId(1L, 2L))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Invitation result = invitationService.createInvitation(1L, 2L, 1L, LogParticipant.ParticipantRole.STATION);

        // Assert
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(8));
    }

    @Test
    @DisplayName("cleanupExpiredInvitations - Expired Invitations - Updates Status")
    void cleanupExpiredInvitations_expiredInvitations_updatesStatus() {
        // Arrange
        Invitation expiredInvitation = Invitation.builder()
                .id(1L)
                .log(testLog)
                .invitedUser(invitedUser)
                .status(Invitation.InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(invitationRepository.findByStatusAndExpiresAtBefore(
                eq(Invitation.InvitationStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(expiredInvitation));

        // Act
        invitationService.cleanupExpiredInvitations();

        // Assert
        verify(invitationRepository).saveAll(argThat(invitations -> {
            List<Invitation> invList = (List<Invitation>) invitations;
            return invList.size() == 1 &&
                   invList.get(0).getStatus() == Invitation.InvitationStatus.EXPIRED;
        }));
    }
}
