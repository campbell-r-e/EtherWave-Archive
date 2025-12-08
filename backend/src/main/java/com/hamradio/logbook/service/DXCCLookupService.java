package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.DXCCPrefix;
import com.hamradio.logbook.repository.DXCCPrefixRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for looking up DXCC entity information from callsigns
 * Uses longest-match algorithm for accurate prefix matching
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DXCCLookupService {

    private final DXCCPrefixRepository dxccPrefixRepository;

    /**
     * Look up DXCC information for a callsign
     * Uses longest-match algorithm:
     * 1. Check for exact match (=W1AW)
     * 2. Find longest matching prefix (W1AW → W1A, W1, W)
     *
     * @param callsign The callsign to look up
     * @return DXCC information or null if not found
     */
    @Cacheable(value = "dxccLookup", key = "#callsign")
    public DXCCInfo lookup(String callsign) {
        if (callsign == null || callsign.isBlank()) {
            return null;
        }

        // Normalize callsign (uppercase, remove spaces)
        callsign = callsign.toUpperCase().trim().replaceAll("\\s+", "");

        log.debug("Looking up DXCC for callsign: {}", callsign);

        // Step 1: Check for exact match
        Optional<DXCCPrefix> exactMatch = dxccPrefixRepository.findExactMatch(callsign);
        if (exactMatch.isPresent()) {
            log.debug("Found exact match for {}: {}", callsign, exactMatch.get().getEntityName());
            return convertToInfo(exactMatch.get());
        }

        // Step 2: Find longest matching prefix
        // Query database for all prefixes that match the start of the callsign
        List<DXCCPrefix> matchingPrefixes = dxccPrefixRepository.findMatchingPrefixes(callsign);

        if (matchingPrefixes.isEmpty()) {
            log.debug("No DXCC match found for callsign: {}", callsign);
            return null;
        }

        // The query returns prefixes ordered by length descending, so first match is longest
        DXCCPrefix bestMatch = matchingPrefixes.get(0);
        log.debug("Found prefix match for {}: {} ({})", callsign, bestMatch.getPrefix(), bestMatch.getEntityName());

        return convertToInfo(bestMatch);
    }

    /**
     * Look up DXCC information by DXCC code
     */
    @Cacheable(value = "dxccByCode", key = "#dxccCode")
    public DXCCInfo lookupByCode(Integer dxccCode) {
        if (dxccCode == null) {
            return null;
        }

        List<DXCCPrefix> prefixes = dxccPrefixRepository.findByDxccCode(dxccCode);
        if (prefixes.isEmpty()) {
            return null;
        }

        // Return first prefix (they all represent the same entity)
        return convertToInfo(prefixes.get(0));
    }

    /**
     * Get all DXCC entities for a continent
     */
    public List<DXCCInfo> getEntitiesByContinent(String continent) {
        if (continent == null || continent.isBlank()) {
            return List.of();
        }

        List<DXCCPrefix> prefixes = dxccPrefixRepository.findByContinent(continent.toUpperCase());

        // Group by DXCC code to avoid duplicates
        return prefixes.stream()
            .collect(java.util.stream.Collectors.toMap(
                DXCCPrefix::getDxccCode,
                this::convertToInfo,
                (existing, replacement) -> existing
            ))
            .values()
            .stream()
            .toList();
    }

    /**
     * Convert DXCCPrefix entity to DXCCInfo DTO
     */
    private DXCCInfo convertToInfo(DXCCPrefix prefix) {
        return DXCCInfo.builder()
            .dxccCode(prefix.getDxccCode())
            .entityName(prefix.getEntityName())
            .continent(prefix.getContinent())
            .cqZone(prefix.getCqZone())
            .ituZone(prefix.getItuZone())
            .latitude(prefix.getLat())
            .longitude(prefix.getLon())
            .primaryPrefix(prefix.getPrefix())
            .build();
    }

    /**
     * Get location for callsign (for QSO mapping)
     * Returns the geographic center of the DXCC entity
     */
    public LocationInfo getLocationForCallsign(String callsign) {
        DXCCInfo dxcc = lookup(callsign);
        if (dxcc == null || dxcc.getLatitude() == null || dxcc.getLongitude() == null) {
            return null;
        }

        return LocationInfo.builder()
            .latitude(dxcc.getLatitude())
            .longitude(dxcc.getLongitude())
            .entityName(dxcc.getEntityName())
            .continent(dxcc.getContinent())
            .dxccCode(dxcc.getDxccCode())
            .build();
    }

    /**
     * Validate if a callsign is likely valid
     * Basic check: should have at least one number and one letter
     */
    public boolean isValidCallsign(String callsign) {
        if (callsign == null || callsign.isBlank()) {
            return false;
        }

        callsign = callsign.trim();

        // Should have at least one digit and one letter
        boolean hasDigit = callsign.matches(".*\\d.*");
        boolean hasLetter = callsign.matches(".*[A-Za-z].*");

        // Typical length is 3-7 characters (though some special cases exist)
        boolean validLength = callsign.length() >= 3 && callsign.length() <= 10;

        return hasDigit && hasLetter && validLength;
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class DXCCInfo {
        private Integer dxccCode;
        private String entityName;
        private String continent;
        private Integer cqZone;
        private Integer ituZone;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String primaryPrefix;
    }

    @Data
    @Builder
    public static class LocationInfo {
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String entityName;
        private String continent;
        private Integer dxccCode;
    }
}
