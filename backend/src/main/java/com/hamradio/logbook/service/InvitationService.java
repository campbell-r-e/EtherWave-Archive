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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final LogRepository logRepository;
    private final UserRepository userRepository;
    private final LogParticipantRepository logParticipantRepository;
    private final LogService logService;

    /**
     * Create and send an invitation
     */
    @Transactional
    public InvitationResponse createInvitation(InvitationRequest request, String inviterUsername) {
        User inviter = getUserByUsername(inviterUsername);
        Log log = getLogByIdOrThrow(request.getLogId());

        // Check if inviter is the log creator
        if (!logService.isCreator(log, inviter)) {
            throw new SecurityException("Only the log creator can send invitations");
        }

        // Personal logs cannot have participants
        if (log.getType() == Log.LogType.PERSONAL) {
            throw new IllegalArgumentException(
                    "Cannot invite participants to a personal log. Convert it to a shared log first.");
        }

        // Find invitee by username, email, or callsign
        User invitee = findUserByIdentifier(request.getInviteeUsername());

        // Check if invitee is already a participant
        if (logParticipantRepository.findByLogAndUser(log, invitee)
                .filter(LogParticipant::getActive)
                .isPresent()) {
            throw new IllegalArgumentException("User is already a participant of this log");
        }

        // Check if there's already a pending invitation
        if (invitationRepository.existsByLogAndInviteeAndStatus(log, invitee, Invitation.InvitationStatus.PENDING)) {
            throw new IllegalArgumentException("A pending invitation already exists for this user");
        }

        // Create invitation
        Invitation invitation = new Invitation();
        invitation.setLog(log);
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setProposedRole(request.getProposedRole());
        invitation.setStationCallsign(request.getStationCallsign());
        invitation.setMessage(request.getMessage());
        invitation.setStatus(Invitation.InvitationStatus.PENDING);
        invitation.setExpiresAt(request.getExpiresAt());

        invitation = invitationRepository.save(invitation);

        InvitationService.log.info("Created invitation for user '{}' to join log '{}' (ID: {}) with role {}",
            invitee.getUsername(), log.getName(), log.getId(), request.getProposedRole());

        return InvitationResponse.fromInvitation(invitation);
    }

    /**
     * Get all pending invitations for a user
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getPendingInvitationsForUser(String username) {
        User user = getUserByUsername(username);
        List<Invitation> invitations = invitationRepository.findPendingInvitationsForUser(user);

        return invitations.stream()
                .map(InvitationResponse::fromInvitation)
                .collect(Collectors.toList());
    }

    /**
     * Get all invitations sent by a user
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getSentInvitations(String username) {
        User user = getUserByUsername(username);
        List<Invitation> invitations = invitationRepository.findByInviter(user);

        return invitations.stream()
                .map(InvitationResponse::fromInvitation)
                .collect(Collectors.toList());
    }

    /**
     * Get all invitations for a specific log
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getInvitationsForLog(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Only creator can view all invitations for the log
        if (!logService.isCreator(log, user)) {
            throw new SecurityException("Only the log creator can view all invitations");
        }

        List<Invitation> invitations = invitationRepository.findByLog(log);

        return invitations.stream()
                .map(InvitationResponse::fromInvitation)
                .collect(Collectors.toList());
    }

    /**
     * Accept an invitation
     */
    @Transactional
    public InvitationResponse acceptInvitation(Long invitationId, String username) {
        User user = getUserByUsername(username);
        Invitation invitation = getInvitationByIdOrThrow(invitationId);

        // Verify this invitation is for the current user
        if (!invitation.getInvitee().getId().equals(user.getId())) {
            throw new SecurityException("This invitation is not for you");
        }

        // Check if invitation can be responded to
        if (!invitation.canRespond()) {
            throw new IllegalStateException("This invitation cannot be accepted (status: " +
                    invitation.getStatus() + ", expired: " + invitation.isExpired() + ")");
        }

        // Update invitation status
        invitation.setStatus(Invitation.InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);

        // Create LogParticipant record
        LogParticipant participant = new LogParticipant();
        participant.setLog(invitation.getLog());
        participant.setUser(user);
        participant.setRole(invitation.getProposedRole());
        participant.setStationCallsign(invitation.getStationCallsign());
        participant.setActive(true);
        logParticipantRepository.save(participant);

        log.info("User '{}' accepted invitation to join log '{}' (ID: {}) with role {}",
                username, invitation.getLog().getName(), invitation.getLog().getId(), invitation.getProposedRole());

        return InvitationResponse.fromInvitation(invitation);
    }

    /**
     * Decline an invitation
     */
    @Transactional
    public InvitationResponse declineInvitation(Long invitationId, String username) {
        User user = getUserByUsername(username);
        Invitation invitation = getInvitationByIdOrThrow(invitationId);

        // Verify this invitation is for the current user
        if (!invitation.getInvitee().getId().equals(user.getId())) {
            throw new SecurityException("This invitation is not for you");
        }

        // Check if invitation can be responded to
        if (!invitation.canRespond()) {
            throw new IllegalStateException("This invitation cannot be declined (status: " +
                    invitation.getStatus() + ", expired: " + invitation.isExpired() + ")");
        }

        // Update invitation status
        invitation.setStatus(Invitation.InvitationStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation = invitationRepository.save(invitation);

        log.info("User '{}' declined invitation to join log '{}' (ID: {})",
                username, invitation.getLog().getName(), invitation.getLog().getId());

        return InvitationResponse.fromInvitation(invitation);
    }

    /**
     * Cancel an invitation (by inviter only)
     */
    @Transactional
    public InvitationResponse cancelInvitation(Long invitationId, String username) {
        User user = getUserByUsername(username);
        Invitation invitation = getInvitationByIdOrThrow(invitationId);

        // Verify this invitation was sent by the current user
        if (!invitation.getInviter().getId().equals(user.getId())) {
            throw new SecurityException("You can only cancel invitations you sent");
        }

        // Only pending invitations can be cancelled
        if (invitation.getStatus() != Invitation.InvitationStatus.PENDING) {
            throw new IllegalStateException("Only pending invitations can be cancelled");
        }

        // Update invitation status
        invitation.setStatus(Invitation.InvitationStatus.CANCELLED);
        invitation = invitationRepository.save(invitation);

        log.info("User '{}' cancelled invitation to '{}' for log '{}' (ID: {})",
                username, invitation.getInvitee().getUsername(),
                invitation.getLog().getName(), invitation.getLog().getId());

        return InvitationResponse.fromInvitation(invitation);
    }

    /**
     * Get a specific invitation by ID
     */
    @Transactional(readOnly = true)
    public InvitationResponse getInvitation(Long invitationId, String username) {
        User user = getUserByUsername(username);
        Invitation invitation = getInvitationByIdOrThrow(invitationId);

        // Check if user is either inviter or invitee
        if (!invitation.getInviter().getId().equals(user.getId()) &&
            !invitation.getInvitee().getId().equals(user.getId())) {
            throw new SecurityException("You do not have access to this invitation");
        }

        return InvitationResponse.fromInvitation(invitation);
    }

    /**
     * Clean up expired invitations (to be called periodically)
     */
    @Transactional
    public int cleanupExpiredInvitations() {
        List<Invitation> allPending = invitationRepository.findByStatus(Invitation.InvitationStatus.PENDING);
        int expiredCount = 0;

        for (Invitation invitation : allPending) {
            if (invitation.isExpired()) {
                invitation.setStatus(Invitation.InvitationStatus.EXPIRED);
                invitationRepository.save(invitation);
                expiredCount++;
            }
        }

        if (expiredCount > 0) {
            log.info("Marked {} expired invitations", expiredCount);
        }

        return expiredCount;
    }

    /**
     * Find user by username or callsign
     */
    private User findUserByIdentifier(String identifier) {
        // Try username first, then callsign
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByCallsign(identifier))
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with username or callsign: " + identifier));
    }

    /**
     * Helper: Get user by username or throw exception
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Helper: Get log by ID or throw exception
     */
    private Log getLogByIdOrThrow(Long logId) {
        return logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
    }

    /**
     * Helper: Get invitation by ID or throw exception
     */
    private Invitation getInvitationByIdOrThrow(Long invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + invitationId));
    }
}
