package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores rig telemetry data over time
 * Optional feature for tracking rig state, debugging, and analytics
 */
@Entity
@Table(name = "rig_telemetry", indexes = {
    @Index(name = "idx_rig_telemetry_station", columnList = "station_id"),
    @Index(name = "idx_rig_telemetry_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RigTelemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "frequency_khz")
    private Long frequencyKhz;

    @Column(length = 20)
    private String mode;

    @Column(name = "ptt_active")
    private Boolean pttActive;

    @Column(name = "s_meter")
    private Integer sMeter;

    @Column(name = "alc_level")
    private Integer alcLevel;

    @Column(precision = 4, scale = 2)
    private BigDecimal swr;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
