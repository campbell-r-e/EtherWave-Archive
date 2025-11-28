package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_participants", indexes = {
    @Index(name = "idx_log_participant_log", columnList = "log_id"),
    @Index(name = "idx_log_participant_user", columnList = "user_id"),
    @Index(name = "idx_log_participant_role", columnList = "role")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"log_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;

    @Column(name = "station_callsign", length = 20)
    private String stationCallsign; // For multi-op contests, which station they operate

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    // Participant roles
    public enum ParticipantRole {
        CREATOR,    // Log owner - full control
        STATION,    // Can log QSOs, edit own QSOs
        VIEWER      // Read-only access
    }

    // Helper methods
    public boolean canEdit() {
        return ParticipantRole.CREATOR.equals(role) || ParticipantRole.STATION.equals(role);
    }

    public boolean canDelete() {
        return ParticipantRole.CREATOR.equals(role);
    }

    public boolean canManageParticipants() {
        return ParticipantRole.CREATOR.equals(role);
    }

    public boolean canExport() {
        return ParticipantRole.CREATOR.equals(role);
    }

    public boolean isCreator() {
        return ParticipantRole.CREATOR.equals(role);
    }

    public boolean isStation() {
        return ParticipantRole.STATION.equals(role);
    }

    public boolean isViewer() {
        return ParticipantRole.VIEWER.equals(role);
    }
}
