package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.log.InvitationRequest;
import com.hamradio.logbook.dto.log.InvitationResponse;
import com.hamradio.logbook.entity.Invitation;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.InvitationRepository;
import com.hamradio.logbook.repository.LogParticipantRepository;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService Unit Tests")
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private LogRepository logRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogParticipantRepository logParticipantRepository;

    @Mock
    private LogService logService;

    @InjectMocks
    private InvitationService invitationService;

    private User inviter;
    private User invitee;
    private Log testLog;
    private Invitation testInvitation;
    private InvitationRequest testRequest;

    @BeforeEach
    void setUp() {
        inviter = new User();
        inviter.setId(1L);
        inviter.setUsername("inviter");
        inviter.setCallsign("W1INV");

        invitee = new User();
        invitee.setId(2L);
        invitee.setUsername("invitee");
        invitee.setCallsign("W2INV");

        testLog = new Log();
        testLog.setId(1L);
        testLog.setName("Test Log");
        testLog.setCreator(inviter);

        testInvitation = new Invitation();
        testInvitation.setId(1L);
        testInvitation.setLog(testLog);
        testInvitation.setInviter(inviter);
        testInvitation.setInvitee(invitee);
        testInvitation.setProposedRole(LogParticipant.ParticipantRole.STATION);
        testInvitation.setStatus(Invitation.InvitationStatus.PENDING);
        testInvitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        testRequest = new InvitationRequest();
        testRequest.setLogId(1L);
        testRequest.setInviteeUsername("invitee");
        testRequest.setProposedRole(LogParticipant.ParticipantRole.STATION);
        testRequest.setExpiresAt(LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("Should create invitation successfully")
    void shouldCreateInvitation() {
        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.isCreator(testLog, inviter)).thenReturn(true);
        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(logParticipantRepository.findByLogAndUser(testLog, invitee)).thenReturn(Optional.empty());
        when(invitationRepository.existsByLogAndInviteeAndStatus(testLog, invitee, Invitation.InvitationStatus.PENDING))
                .thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);

        InvitationResponse result = invitationService.createInvitation(testRequest, "inviter");

        assertNotNull(result);
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    @DisplayName("Should fail to create invitation when user is not creator")
    void shouldFailToCreateInvitationWhenNotCreator() {
        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.isCreator(testLog, inviter)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                invitationService.createInvitation(testRequest, "inviter"));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when invitee is already a participant")
    void shouldFailWhenInviteeAlreadyParticipant() {
        LogParticipant existingParticipant = new LogParticipant();
        existingParticipant.setActive(true);

        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.isCreator(testLog, inviter)).thenReturn(true);
        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(logParticipantRepository.findByLogAndUser(testLog, invitee))
                .thenReturn(Optional.of(existingParticipant));

        assertThrows(IllegalArgumentException.class, () ->
                invitationService.createInvitation(testRequest, "inviter"));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when pending invitation already exists")
    void shouldFailWhenPendingInvitationExists() {
        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.isCreator(testLog, inviter)).thenReturn(true);
        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(logParticipantRepository.findByLogAndUser(testLog, invitee)).thenReturn(Optional.empty());
        when(invitationRepository.existsByLogAndInviteeAndStatus(testLog, invitee, Invitation.InvitationStatus.PENDING))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
                invitationService.createInvitation(testRequest, "inviter"));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get pending invitations for user")
    void shouldGetPendingInvitationsForUser() {
        List<Invitation> invitations = Arrays.asList(testInvitation);

        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findPendingInvitationsForUser(invitee)).thenReturn(invitations);

        List<InvitationResponse> result = invitationService.getPendingInvitationsForUser("invitee");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(invitationRepository).findPendingInvitationsForUser(invitee);
    }

    @Test
    @DisplayName("Should get sent invitations")
    void shouldGetSentInvitations() {
        List<Invitation> invitations = Arrays.asList(testInvitation);

        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(invitationRepository.findByInviter(inviter)).thenReturn(invitations);

        List<InvitationResponse> result = invitationService.getSentInvitations("inviter");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(invitationRepository).findByInviter(inviter);
    }

    @Test
    @DisplayName("Should get invitations for log")
    void shouldGetInvitationsForLog() {
        List<Invitation> invitations = Arrays.asList(testInvitation);

        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.isCreator(testLog, inviter)).thenReturn(true);
        when(invitationRepository.findByLog(testLog)).thenReturn(invitations);

        List<InvitationResponse> result = invitationService.getInvitationsForLog(1L, "inviter");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(invitationRepository).findByLog(testLog);
    }

    @Test
    @DisplayName("Should fail to get invitations for log when not creator")
    void shouldFailToGetInvitationsForLogWhenNotCreator() {
        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(logService.isCreator(testLog, inviter)).thenReturn(false);

        assertThrows(SecurityException.class, () ->
                invitationService.getInvitationsForLog(1L, "inviter"));

        verify(invitationRepository, never()).findByLog(any());
    }

    @Test
    @DisplayName("Should accept invitation successfully")
    void shouldAcceptInvitation() {
        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);
        when(logParticipantRepository.save(any(LogParticipant.class))).thenReturn(new LogParticipant());

        InvitationResponse result = invitationService.acceptInvitation(1L, "invitee");

        assertNotNull(result);
        verify(invitationRepository).save(any(Invitation.class));
        verify(logParticipantRepository).save(any(LogParticipant.class));
    }

    @Test
    @DisplayName("Should fail to accept invitation when not for user")
    void shouldFailToAcceptInvitationWhenNotForUser() {
        User otherUser = new User();
        otherUser.setId(3L);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        assertThrows(SecurityException.class, () ->
                invitationService.acceptInvitation(1L, "other"));

        verify(invitationRepository, never()).save(any());
        verify(logParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should decline invitation successfully")
    void shouldDeclineInvitation() {
        when(userRepository.findByUsername("invitee")).thenReturn(Optional.of(invitee));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);

        InvitationResponse result = invitationService.declineInvitation(1L, "invitee");

        assertNotNull(result);
        verify(invitationRepository).save(any(Invitation.class));
        verify(logParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to decline invitation when not for user")
    void shouldFailToDeclineInvitationWhenNotForUser() {
        User otherUser = new User();
        otherUser.setId(3L);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        assertThrows(SecurityException.class, () ->
                invitationService.declineInvitation(1L, "other"));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cancel invitation successfully")
    void shouldCancelInvitation() {
        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));
        when(invitationRepository.save(any(Invitation.class))).thenReturn(testInvitation);

        InvitationResponse result = invitationService.cancelInvitation(1L, "inviter");

        assertNotNull(result);
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    @DisplayName("Should fail to cancel invitation when not inviter")
    void shouldFailToCancelInvitationWhenNotInviter() {
        User otherUser = new User();
        otherUser.setId(3L);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        assertThrows(SecurityException.class, () ->
                invitationService.cancelInvitation(1L, "other"));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail to cancel non-pending invitation")
    void shouldFailToCancelNonPendingInvitation() {
        testInvitation.setStatus(Invitation.InvitationStatus.ACCEPTED);

        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        assertThrows(IllegalStateException.class, () ->
                invitationService.cancelInvitation(1L, "inviter"));

        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get specific invitation")
    void shouldGetSpecificInvitation() {
        when(userRepository.findByUsername("inviter")).thenReturn(Optional.of(inviter));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        InvitationResponse result = invitationService.getInvitation(1L, "inviter");

        assertNotNull(result);
        verify(invitationRepository).findById(1L);
    }

    @Test
    @DisplayName("Should fail to get invitation when not inviter or invitee")
    void shouldFailToGetInvitationWhenNoAccess() {
        User otherUser = new User();
        otherUser.setId(3L);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(testInvitation));

        assertThrows(SecurityException.class, () ->
                invitationService.getInvitation(1L, "other"));
    }

    @Test
    @DisplayName("Should cleanup expired invitations")
    void shouldCleanupExpiredInvitations() {
        Invitation expiredInvitation = new Invitation();
        expiredInvitation.setId(2L);
        expiredInvitation.setStatus(Invitation.InvitationStatus.PENDING);
        expiredInvitation.setExpiresAt(LocalDateTime.now().minusDays(1));

        List<Invitation> pendingInvitations = Arrays.asList(testInvitation, expiredInvitation);

        when(invitationRepository.findByStatus(Invitation.InvitationStatus.PENDING))
                .thenReturn(pendingInvitations);
        when(invitationRepository.save(any(Invitation.class))).thenReturn(expiredInvitation);

        int result = invitationService.cleanupExpiredInvitations();

        assertEquals(1, result);
        verify(invitationRepository).save(expiredInvitation);
    }

    @Test
    @DisplayName("Should return zero when no expired invitations")
    void shouldReturnZeroWhenNoExpiredInvitations() {
        List<Invitation> pendingInvitations = Arrays.asList(testInvitation);

        when(invitationRepository.findByStatus(Invitation.InvitationStatus.PENDING))
                .thenReturn(pendingInvitations);

        int result = invitationService.cleanupExpiredInvitations();

        assertEquals(0, result);
        verify(invitationRepository, never()).save(any());
    }
}
