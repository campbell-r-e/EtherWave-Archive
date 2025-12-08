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
 * Server-side clustering cache (per log, zoom level)
 * Lazy caching strategy with filter hash for invalidation
 */
@Entity
@Table(name = "map_clusters", indexes = {
    @Index(name = "idx_cluster_log_zoom", columnList = "log_id, zoom_level"),
    @Index(name = "idx_cluster_bounds", columnList = "min_lat, max_lat, min_lon, max_lon")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"log_id", "zoom_level", "lat", "lon", "filter_hash"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false)
    private Log log;

    @Column(name = "zoom_level", nullable = false)
    private Integer zoomLevel; // 0-18

    // Cluster center
    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lon;

    // Cluster metadata
    @Column(name = "qso_count", nullable = false)
    private Integer qsoCount;

    // JSON: {"1": 45, "2": 30, "GOTA": 12}
    @Column(name = "station_breakdown", columnDefinition = "TEXT")
    private String stationBreakdown;

    // JSON: {"20M": 30, "40M": 25, "80M": 20}
    @Column(name = "band_breakdown", columnDefinition = "TEXT")
    private String bandBreakdown;

    // JSON: {"SSB": 50, "CW": 30, "FT8": 15}
    @Column(name = "mode_breakdown", columnDefinition = "TEXT")
    private String modeBreakdown;

    // Bounding box of clustered QSOs
    @Column(name = "min_lat", precision = 9, scale = 6)
    private BigDecimal minLat;

    @Column(name = "max_lat", precision = 9, scale = 6)
    private BigDecimal maxLat;

    @Column(name = "min_lon", precision = 9, scale = 6)
    private BigDecimal minLon;

    @Column(name = "max_lon", precision = 9, scale = 6)
    private BigDecimal maxLon;

    // Filter state hash (for cache invalidation)
    @Column(name = "filter_hash", length = 64)
    private String filterHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
