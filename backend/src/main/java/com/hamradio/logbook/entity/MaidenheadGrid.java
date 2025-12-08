package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dynamic grid square database (worked grids + neighbors only)
 * Statistics are calculated per log
 */
@Entity
@Table(name = "maidenhead_grids", indexes = {
    @Index(name = "idx_grid_log", columnList = "log_id"),
    @Index(name = "idx_grid_grid", columnList = "grid"),
    @Index(name = "idx_grid_bounds", columnList = "min_lat, max_lat, min_lon, max_lon")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"grid", "precision", "log_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaidenheadGrid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String grid;

    @Column(nullable = false)
    private Integer precision; // 2, 4, 6, or 8 characters

    // Center coordinates
    @Column(name = "center_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal centerLat;

    @Column(name = "center_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal centerLon;

    // Bounding box
    @Column(name = "min_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal minLat;

    @Column(name = "max_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal maxLat;

    @Column(name = "min_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal minLon;

    @Column(name = "max_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal maxLon;

    // Statistics (per log)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    @Column(name = "qso_count")
    @Builder.Default
    private Integer qsoCount = 0;

    @Column(name = "band_count")
    @Builder.Default
    private Integer bandCount = 0;

    @Column(name = "mode_count")
    @Builder.Default
    private Integer modeCount = 0;

    @Column(name = "first_qso_date")
    private LocalDateTime firstQsoDate;

    @Column(name = "last_qso_date")
    private LocalDateTime lastQsoDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
