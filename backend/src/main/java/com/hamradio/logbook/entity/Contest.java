package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a ham radio contest configuration
 * Contains rules and validator class for contest-specific validation
 */
@Entity
@Table(name = "contests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contest_code", nullable = false, unique = true, length = 50)
    private String contestCode;

    @Column(name = "contest_name", nullable = false, length = 200)
    private String contestName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "validator_class", length = 255)
    private String validatorClass;

    /**
     * JSON configuration defining required/optional fields and scoring rules
     * Example: {"required_fields": ["class", "section"], "valid_classes": ["1A", "2A"]}
     */
    @Column(name = "rules_config", columnDefinition = "TEXT")
    private String rulesConfig;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
