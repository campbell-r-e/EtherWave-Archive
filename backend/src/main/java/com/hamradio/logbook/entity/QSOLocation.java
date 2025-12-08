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
 * Cached location data for QSOs to avoid repeated geocoding
 * Stores both operator and contact locations with distance calculations
 */
@Entity
@Table(name = "qso_locations", indexes = {
    @Index(name = "idx_qso_location_qso", columnList = "qso_id", unique = true),
    @Index(name = "idx_qso_location_contact_latlon", columnList = "contact_lat, contact_lon"),
    @Index(name = "idx_qso_location_contact_grid", columnList = "contact_grid"),
    @Index(name = "idx_qso_location_dxcc", columnList = "contact_dxcc")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QSOLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qso_id", nullable = false)
    private QSO qso;

    // Operator location (hierarchical fallback: Station → User → Session → Manual)
    @Column(name = "operator_lat", precision = 9, scale = 6)
    private BigDecimal operatorLat;

    @Column(name = "operator_lon", precision = 9, scale = 6)
    private BigDecimal operatorLon;

    @Column(name = "operator_grid", length = 8)
    private String operatorGrid;

    @Column(name = "location_source", length = 20)
    @Enumerated(EnumType.STRING)
    private LocationSource locationSource;

    // Contact location
    @Column(name = "contact_lat", precision = 9, scale = 6)
    private BigDecimal contactLat;

    @Column(name = "contact_lon", precision = 9, scale = 6)
    private BigDecimal contactLon;

    @Column(name = "contact_grid", length = 8)
    private String contactGrid;

    @Column(name = "contact_dxcc", length = 10)
    private String contactDxcc;

    @Column(name = "contact_continent", length = 2)
    private String contactContinent;

    @Column(name = "contact_cq_zone")
    private Integer contactCqZone;

    @Column(name = "contact_itu_zone")
    private Integer contactItuZone;

    // Distance calculation (cached)
    @Column(name = "distance_km", precision = 10, scale = 2)
    private BigDecimal distanceKm;

    @Column(name = "distance_mi", precision = 10, scale = 2)
    private BigDecimal distanceMi;

    @Column(precision = 5, scale = 2)
    private BigDecimal bearing;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LocationSource {
        STATION,    // From station configuration
        USER,       // From user default location
        SESSION,    // From session override
        MANUAL      // Manually entered
    }
}
