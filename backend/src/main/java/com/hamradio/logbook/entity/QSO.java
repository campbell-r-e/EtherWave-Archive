package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a single QSO (contact) in the logbook
 * Supports flexible contest-specific data via JSON field
 */
@Entity
@Table(name = "qsos", indexes = {
    @Index(name = "idx_qso_log", columnList = "log_id"),
    @Index(name = "idx_qso_callsign", columnList = "callsign"),
    @Index(name = "idx_qso_date", columnList = "qso_date"),
    @Index(name = "idx_qso_station", columnList = "station_id"),
    @Index(name = "idx_qso_contest", columnList = "contest_id"),
    @Index(name = "idx_qso_frequency", columnList = "frequency_khz"),
    @Index(name = "idx_qso_station_number", columnList = "station_number"),
    @Index(name = "idx_qso_is_gota", columnList = "is_gota")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QSO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    // Core QSO fields
    @Column(nullable = false, length = 20)
    private String callsign;

    @Column(name = "frequency_khz", nullable = false)
    private Long frequencyKhz;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(name = "qso_date", nullable = false)
    private LocalDate qsoDate;

    @Column(name = "time_on", nullable = false)
    private LocalTime timeOn;

    @Column(name = "time_off")
    private LocalTime timeOff;

    // Signal reports
    @Column(name = "rst_sent", length = 10)
    private String rstSent;

    @Column(name = "rst_rcvd", length = 10)
    private String rstRcvd;

    @Column(length = 10)
    private String band;

    @Column(name = "power_watts")
    private Integer powerWatts;

    // Location data
    @Column(name = "grid_square", length = 10)
    private String gridSquare;

    @Column(length = 50)
    private String county;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String country;

    private Integer dxcc;

    @Column(name = "cq_zone")
    private Integer cqZone;

    @Column(name = "itu_zone")
    private Integer ituZone;

    // Operator info from validation
    @Column(length = 100)
    private String name;

    @Column(name = "license_class", length = 20)
    private String licenseClass;

    // Multi-station contest support
    @Column(name = "station_number")
    private Integer stationNumber; // 1-1000, null if unassigned or not multi-station

    @Column(name = "is_gota", nullable = false)
    private Boolean isGota = false; // Get On The Air station (mutually exclusive with stationNumber for scoring)

    /**
     * Contest-specific data stored as JSON
     * Examples:
     * - Field Day: {"class": "2A", "section": "ORG"}
     * - POTA: {"park_ref": "K-0817", "hunter_ref": "K-4566"}
     * - SOTA: {"summit_ref": "W7W/NG-001", "points": 10}
     */
    @Column(name = "contest_data", columnDefinition = "TEXT")
    private String contestData;

    // QSL and confirmation
    @Column(name = "qsl_sent", length = 1)
    private String qslSent;

    @Column(name = "qsl_rcvd", length = 1)
    private String qslRcvd;

    @Column(name = "lotw_sent", length = 1)
    private String lotwSent;

    @Column(name = "lotw_rcvd", length = 1)
    private String lotwRcvd;

    // Validation and status
    @Column(name = "is_valid")
    private Boolean isValid = true;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    // Scoring fields
    @Column(name = "points")
    private Integer points = 0;

    @Column(name = "is_duplicate")
    private Boolean isDuplicate = false;

    @Column(name = "is_multiplier")
    private Boolean isMultiplier = false;

    // JSON array of multiplier types this QSO counts for: ["STATE", "DXCC", "ARRL_SECT"]
    @Column(name = "multiplier_types", columnDefinition = "TEXT")
    private String multiplierTypes;

    // Comments
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Metadata
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
