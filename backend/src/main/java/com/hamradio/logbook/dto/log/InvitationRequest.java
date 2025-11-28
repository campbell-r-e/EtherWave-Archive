package com.hamradio.logbook.dto.log;

import com.hamradio.logbook.entity.LogParticipant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationRequest {

    @NotNull(message = "Log ID is required")
    private Long logId;

    @NotNull(message = "Invitee username is required")
    private String inviteeUsername; // Can be username, email, or callsign

    @NotNull(message = "Role is required")
    private LogParticipant.ParticipantRole proposedRole;

    @Size(max = 20, message = "Station callsign must not exceed 20 characters")
    private String stationCallsign;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    private String message;

    private LocalDateTime expiresAt; // Optional expiration
}
