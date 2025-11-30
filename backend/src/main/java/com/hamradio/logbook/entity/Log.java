package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs", indexes = {
    @Index(name = "idx_log_creator", columnList = "creator_id"),
    @Index(name = "idx_log_type", columnList = "log_type"),
    @Index(name = "idx_log_contest", columnList = "contest_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, length = 20)
    private LogType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest; // Optional - for contest logs

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean editable = true; // Can be frozen after contest ends

    @Column(name = "is_public")
    private Boolean isPublic = false; // For future feature: public logs

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Log types
    public enum LogType {
        PERSONAL,    // Single user's personal log
        SHARED       // Multi-user shared log (e.g., Field Day, club station)
    }

    // Helper methods
    public boolean isActive() {
        if (!active) return false;

        LocalDateTime now = LocalDateTime.now();

        // Check if within date range (if specified)
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;

        return true;
    }

    public boolean isEditable() {
        if (!editable) return false;

        // Check if log period has ended
        if (endDate != null && LocalDateTime.now().isAfter(endDate)) {
            return false;
        }

        return true;
    }

    public boolean isPersonal() {
        return LogType.PERSONAL.equals(this.type);
    }

    public boolean isShared() {
        return LogType.SHARED.equals(this.type);
    }
}
