package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.log.InvitationRequest;
import com.hamradio.logbook.dto.log.InvitationResponse;
import com.hamradio.logbook.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * Get all pending invitations for the current user
     */
    @GetMapping("/pending")
    public ResponseEntity<List<InvitationResponse>> getPendingInvitations(Authentication authentication) {
        String username = authentication.getName();
        List<InvitationResponse> invitations = invitationService.getPendingInvitationsForUser(username);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Get all invitations sent by the current user
     */
    @GetMapping("/sent")
    public ResponseEntity<List<InvitationResponse>> getSentInvitations(Authentication authentication) {
        String username = authentication.getName();
        List<InvitationResponse> invitations = invitationService.getSentInvitations(username);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Get all invitations for a specific log
     */
    @GetMapping("/log/{logId}")
    public ResponseEntity<List<InvitationResponse>> getInvitationsForLog(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        List<InvitationResponse> invitations = invitationService.getInvitationsForLog(logId, username);
        return ResponseEntity.ok(invitations);
    }

    /**
     * Get a specific invitation by ID
     */
    @GetMapping("/{invitationId}")
    public ResponseEntity<InvitationResponse> getInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        String username = authentication.getName();
        InvitationResponse invitation = invitationService.getInvitation(invitationId, username);
        return ResponseEntity.ok(invitation);
    }

    /**
     * Create and send a new invitation
     */
    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(
            @Valid @RequestBody InvitationRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        InvitationResponse invitation = invitationService.createInvitation(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    /**
     * Accept an invitation
     */
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        String username = authentication.getName();
        InvitationResponse invitation = invitationService.acceptInvitation(invitationId, username);
        return ResponseEntity.ok(invitation);
    }

    /**
     * Decline an invitation
     */
    @PostMapping("/{invitationId}/decline")
    public ResponseEntity<InvitationResponse> declineInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        String username = authentication.getName();
        InvitationResponse invitation = invitationService.declineInvitation(invitationId, username);
        return ResponseEntity.ok(invitation);
    }

    /**
     * Cancel an invitation (by inviter)
     */
    @PostMapping("/{invitationId}/cancel")
    public ResponseEntity<InvitationResponse> cancelInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        String username = authentication.getName();
        InvitationResponse invitation = invitationService.cancelInvitation(invitationId, username);
        return ResponseEntity.ok(invitation);
    }

    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        ErrorResponse error = new ErrorResponse("FORBIDDEN", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse("CONFLICT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Error response DTO
     */
    private record ErrorResponse(String code, String message) {}
}
