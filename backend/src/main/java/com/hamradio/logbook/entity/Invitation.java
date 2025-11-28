package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitations", indexes = {
    @Index(name = "idx_invitation_log", columnList = "log_id"),
    @Index(name = "idx_invitation_invitee", columnList = "invitee_id"),
    @Index(name = "idx_invitation_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter; // Who sent the invitation

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee; // Who received the invitation

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_role", nullable = false, length = 20)
    private LogParticipant.ParticipantRole proposedRole;

    @Column(name = "station_callsign", length = 20)
    private String stationCallsign; // Optional station assignment

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(length = 500)
    private String message; // Optional message from inviter

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Optional expiration

    // Invitation statuses
    public enum InvitationStatus {
        PENDING,    // Waiting for response
        ACCEPTED,   // User accepted invitation
        DECLINED,   // User declined invitation
        CANCELLED,  // Inviter cancelled invitation
        EXPIRED     // Invitation expired
    }

    // Helper methods
    public boolean isPending() {
        return InvitationStatus.PENDING.equals(status);
    }

    public boolean isAccepted() {
        return InvitationStatus.ACCEPTED.equals(status);
    }

    public boolean isDeclined() {
        return InvitationStatus.DECLINED.equals(status);
    }

    public boolean isCancelled() {
        return InvitationStatus.CANCELLED.equals(status);
    }

    public boolean isExpired() {
        if (expiresAt == null) return false;
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canRespond() {
        return isPending() && !isExpired();
    }

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = InvitationStatus.DECLINED;
        this.respondedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
        this.respondedAt = LocalDateTime.now();
    }
}
