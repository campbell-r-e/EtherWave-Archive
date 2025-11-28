package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Caches callsign lookups from external APIs (QRZ, FCC, etc.)
 * Reduces bandwidth usage and improves response time
 */
@Entity
@Table(name = "callsign_cache", indexes = {
    @Index(name = "idx_callsign_cache_callsign", columnList = "callsign")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallsignCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String callsign;

    @Column(length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String country;

    @Column(name = "license_class", length = 20)
    private String licenseClass;

    @Column(name = "grid_square", length = 10)
    private String gridSquare;

    @Column(name = "lookup_source", length = 50)
    private String lookupSource;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Check if the cached entry has expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
