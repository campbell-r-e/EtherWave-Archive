package com.hamradio.logbook.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing temporary session-based location overrides
 * Stores operator locations in memory for the current session
 *
 * NOTE: This is a simplified in-memory implementation.
 * A production implementation would use:
 * - HTTP session storage (HttpSession)
 * - Redis cache with TTL
 * - Database session table
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionLocationService {

    private final MaidenheadConverter maidenheadConverter;

    // In-memory storage: logId -> SessionLocation
    // NOTE: This is NOT persistent and will be lost on server restart
    // Consider using Redis or database for production
    private final Map<Long, SessionLocation> sessionLocations = new ConcurrentHashMap<>();

    /**
     * Set temporary session location for a log
     * This overrides station and user default locations for the current session
     *
     * @param logId Log ID
     * @param latitude Latitude
     * @param longitude Longitude
     * @param grid Maidenhead grid (optional, auto-calculated if null)
     * @return Session location response
     */
    public SessionLocationResponse setSessionLocation(
            Long logId,
            Double latitude,
            Double longitude,
            String grid
    ) {
        log.info("Setting session location for log {}: lat={}, lon={}", logId, latitude, longitude);

        // Auto-calculate grid if not provided
        if (grid == null || grid.isBlank()) {
            grid = maidenheadConverter.toMaidenhead(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                6 // Default to 6-character precision
            );
            log.debug("Auto-calculated grid: {}", grid);
        }

        SessionLocation location = SessionLocation.builder()
            .logId(logId)
            .latitude(latitude)
            .longitude(longitude)
            .grid(grid)
            .timestamp(System.currentTimeMillis())
            .build();

        sessionLocations.put(logId, location);

        return SessionLocationResponse.builder()
            .success(true)
            .logId(logId)
            .latitude(latitude)
            .longitude(longitude)
            .grid(grid)
            .message("Session location set successfully (temporary, in-memory)")
            .build();
    }

    /**
     * Get session location for a log
     *
     * @param logId Log ID
     * @return Session location or null if not set
     */
    public SessionLocation getSessionLocation(Long logId) {
        SessionLocation location = sessionLocations.get(logId);

        // Check if location has expired (1 hour TTL)
        if (location != null) {
            long age = System.currentTimeMillis() - location.getTimestamp();
            if (age > 3600000) { // 1 hour in milliseconds
                log.debug("Session location for log {} has expired, removing", logId);
                sessionLocations.remove(logId);
                return null;
            }
        }

        return location;
    }

    /**
     * Clear session location for a log
     *
     * @param logId Log ID
     */
    public void clearSessionLocation(Long logId) {
        log.info("Clearing session location for log {}", logId);
        sessionLocations.remove(logId);
    }

    /**
     * Clear all session locations (for cleanup/testing)
     */
    public void clearAllSessionLocations() {
        log.info("Clearing all session locations");
        sessionLocations.clear();
    }

    /**
     * Get count of active session locations
     */
    public int getActiveSessionCount() {
        // Remove expired locations
        long now = System.currentTimeMillis();
        sessionLocations.entrySet().removeIf(entry -> {
            long age = now - entry.getValue().getTimestamp();
            return age > 3600000; // 1 hour
        });

        return sessionLocations.size();
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class SessionLocation {
        private Long logId;
        private Double latitude;
        private Double longitude;
        private String grid;
        private long timestamp; // Unix timestamp in milliseconds
    }

    @Data
    @Builder
    public static class SessionLocationResponse {
        private boolean success;
        private Long logId;
        private Double latitude;
        private Double longitude;
        private String grid;
        private String message;
    }
}
