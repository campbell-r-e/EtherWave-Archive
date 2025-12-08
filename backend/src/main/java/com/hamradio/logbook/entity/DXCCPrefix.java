package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DXCC entity prefix database loaded from CTY.DAT
 * Used for callsign lookup and geographic mapping
 */
@Entity
@Table(name = "dxcc_prefixes", indexes = {
    @Index(name = "idx_dxcc_prefix", columnList = "prefix", unique = true),
    @Index(name = "idx_dxcc_code", columnList = "dxcc_code"),
    @Index(name = "idx_dxcc_continent", columnList = "continent")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DXCCPrefix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String prefix;

    @Column(name = "dxcc_code", nullable = false)
    private Integer dxccCode;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(length = 2)
    private String continent; // AF, AS, EU, NA, OC, SA

    @Column(name = "cq_zone")
    private Integer cqZone;

    @Column(name = "itu_zone")
    private Integer ituZone;

    // Geographic center of entity
    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lon;

    // True for exact match prefixes (e.g., =K1ABC style)
    @Column(name = "exact_match")
    @Builder.Default
    private Boolean exactMatch = false;
}
