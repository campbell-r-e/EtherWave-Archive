package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.CallsignInfo;
import com.hamradio.logbook.entity.CallsignCache;
import com.hamradio.logbook.repository.CallsignCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for validating and looking up callsigns
 * Supports QRZ, FCC, and other lookup services with caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallsignValidationService {

    private final CallsignCacheRepository cacheRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${qrz.api.username:}")
    private String qrzUsername;

    @Value("${qrz.api.password:}")
    private String qrzPassword;

    @Value("${callsign.validation.cache.ttl:3600}")
    private long cacheTtlSeconds;

    /**
     * Lookup callsign information with caching
     */
    @Transactional
    public Optional<CallsignInfo> lookupCallsign(String callsign) {
        if (callsign == null || callsign.isEmpty()) {
            return Optional.empty();
        }

        callsign = callsign.toUpperCase().trim();

        // Check cache first
        Optional<CallsignCache> cached = cacheRepository.findValidByCallsign(callsign, LocalDateTime.now());
        if (cached.isPresent()) {
            log.debug("Cache hit for callsign: {}", callsign);
            return Optional.of(fromCache(cached.get()));
        }

        // Cache miss - try external lookups
        log.debug("Cache miss for callsign: {}, attempting external lookup", callsign);

        // Try QRZ if credentials are configured
        if (hasQrzCredentials()) {
            Optional<CallsignInfo> qrzInfo = lookupQRZ(callsign);
            if (qrzInfo.isPresent()) {
                cacheCallsignInfo(callsign, qrzInfo.get(), "QRZ");
                return qrzInfo;
            }
        }

        // Try FCC database (US callsigns only)
        if (isUSCallsign(callsign)) {
            Optional<CallsignInfo> fccInfo = lookupFCC(callsign);
            if (fccInfo.isPresent()) {
                cacheCallsignInfo(callsign, fccInfo.get(), "FCC");
                return fccInfo;
            }
        }

        // No data found
        log.warn("No information found for callsign: {}", callsign);
        return Optional.empty();
    }

    /**
     * Lookup callsign via QRZ API
     */
    private Optional<CallsignInfo> lookupQRZ(String callsign) {
        try {
            // This is a placeholder - actual QRZ API requires XML parsing and session management
            // For production, you'd use QRZ XML API with proper authentication
            log.debug("QRZ lookup not fully implemented - would lookup: {}", callsign);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error looking up callsign on QRZ: {}", callsign, e);
            return Optional.empty();
        }
    }

    /**
     * Lookup callsign via FCC database
     */
    private Optional<CallsignInfo> lookupFCC(String callsign) {
        try {
            // FCC ULS API endpoint
            String url = String.format("https://data.fcc.gov/api/license-view/basicSearch/getLicenses?searchValue=%s&format=json", callsign);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse FCC response (simplified - actual implementation needs JSON parsing)
                // This is a placeholder for demonstration
                log.debug("FCC API response received for: {}", callsign);

                // In production, parse JSON and extract:
                // - name
                // - address
                // - state
                // - license class
                // - grid square (if available)

                return Optional.empty(); // Placeholder
            }
        } catch (Exception e) {
            log.error("Error looking up callsign on FCC: {}", callsign, e);
        }
        return Optional.empty();
    }

    /**
     * Cache callsign information
     */
    private void cacheCallsignInfo(String callsign, CallsignInfo info, String source) {
        CallsignCache cache = CallsignCache.builder()
                .callsign(callsign)
                .name(info.getName())
                .address(info.getAddress())
                .state(info.getState())
                .country(info.getCountry())
                .licenseClass(info.getLicenseClass())
                .gridSquare(info.getGridSquare())
                .lookupSource(source)
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(cacheTtlSeconds))
                .build();

        // Delete existing cache entry if present
        cacheRepository.findByCallsign(callsign).ifPresent(existing ->
                cacheRepository.deleteById(existing.getId())
        );

        cacheRepository.save(cache);
        log.info("Cached callsign info for {} from {}", callsign, source);
    }

    /**
     * Convert cache entity to DTO
     */
    private CallsignInfo fromCache(CallsignCache cache) {
        return CallsignInfo.builder()
                .callsign(cache.getCallsign())
                .name(cache.getName())
                .address(cache.getAddress())
                .state(cache.getState())
                .country(cache.getCountry())
                .licenseClass(cache.getLicenseClass())
                .gridSquare(cache.getGridSquare())
                .lookupSource(cache.getLookupSource())
                .cached(true)
                .build();
    }

    /**
     * Check if QRZ credentials are configured
     */
    private boolean hasQrzCredentials() {
        return qrzUsername != null && !qrzUsername.isEmpty()
                && qrzPassword != null && !qrzPassword.isEmpty();
    }

    /**
     * Check if callsign appears to be a US callsign
     */
    private boolean isUSCallsign(String callsign) {
        if (callsign == null || callsign.length() < 3) {
            return false;
        }
        // US callsigns start with K, N, W, or AA-AL
        char first = callsign.charAt(0);
        return first == 'K' || first == 'N' || first == 'W' ||
                (first == 'A' && callsign.length() > 1 && callsign.charAt(1) >= 'A' && callsign.charAt(1) <= 'L');
    }

    /**
     * Clean up expired cache entries
     */
    @Transactional
    public void cleanupExpiredCache() {
        cacheRepository.deleteExpired(LocalDateTime.now());
        log.info("Cleaned up expired callsign cache entries");
    }
}
