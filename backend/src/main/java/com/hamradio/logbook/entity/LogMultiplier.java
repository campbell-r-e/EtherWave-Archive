package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks multipliers worked per log for contest scoring
 * Examples: states, DXCC entities, ARRL sections, grid squares
 *
 * For per-band multipliers (e.g., WPX), band field is populated.
 * For all-band multipliers (e.g., Field Day sections), band is null.
 */
@Entity
@Table(name = "log_multipliers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_log_mult",
            columnNames = {"log_id", "multiplier_type", "multiplier_value", "band"}
        )
    },
    indexes = {
        @Index(name = "idx_log_mult_log", columnList = "log_id"),
        @Index(name = "idx_log_mult_type", columnList = "multiplier_type"),
        @Index(name = "idx_log_mult_band", columnList = "band")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogMultiplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    /**
     * Type of multiplier
     * Examples: "STATE", "DXCC", "GRID", "ARRL_SECT", "CQ_ZONE", "ITU_ZONE"
     */
    @Column(name = "multiplier_type", nullable = false, length = 20)
    private String multiplierType;

    /**
     * Value of the multiplier
     * Examples: "CT" (state), "K" (DXCC), "FN31" (grid), "ORG" (ARRL section)
     */
    @Column(name = "multiplier_value", nullable = false, length = 50)
    private String multiplierValue;

    /**
     * Band for per-band multipliers
     * NULL for all-band multipliers (e.g., Field Day)
     * Examples: "20M", "10M", "2M"
     */
    @Column(name = "band", length = 10)
    private String band;

    /**
     * First QSO that worked this multiplier
     * Used to flag the QSO as a multiplier and for verification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_qso_id")
    private QSO firstQso;

    /**
     * When this multiplier was first worked
     */
    @Column(name = "worked_date")
    private LocalDateTime workedDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
