package com.hamradio.logbook.dto.log;

import com.hamradio.logbook.entity.Invitation;
import com.hamradio.logbook.entity.LogParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {

    private Long id;
    private Long logId;
    private String logName;
    private Long inviterId;
    private String inviterUsername;
    private Long inviteeId;
    private String inviteeUsername;
    private String inviteeCallsign;
    private LogParticipant.ParticipantRole proposedRole;
    private String stationCallsign;
    private Invitation.InvitationStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean canRespond; // Computed field based on status and expiration

    public static InvitationResponse fromInvitation(Invitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId())
                .logId(invitation.getLog().getId())
                .logName(invitation.getLog().getName())
                .inviterId(invitation.getInviter().getId())
                .inviterUsername(invitation.getInviter().getUsername())
                .inviteeId(invitation.getInvitee().getId())
                .inviteeUsername(invitation.getInvitee().getUsername())
                .inviteeCallsign(invitation.getInvitee().getCallsign())
                .proposedRole(invitation.getProposedRole())
                .stationCallsign(invitation.getStationCallsign())
                .status(invitation.getStatus())
                .message(invitation.getMessage())
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .canRespond(invitation.canRespond())
                .build();
    }
}
