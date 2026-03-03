package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.StationRepository;
import com.hamradio.logbook.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for managing station and user locations
 * Handles the hierarchical location fallback system
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LocationManagementService {

    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final MaidenheadConverter maidenheadConverter;

    /**
     * Update station location
     *
     * @param stationId Station ID
     * @param latitude Latitude
     * @param longitude Longitude
     * @param grid Maidenhead grid (optional, auto-calculated if null)
     * @param locationName Location name (e.g., "Field Day Site 2025")
     * @return Updated station location
     */
    @Transactional
    public LocationUpdateResponse updateStationLocation(
            Long stationId,
            Double latitude,
            Double longitude,
            String grid,
            String locationName
    ) {
        log.info("Updating station {} location to lat={}, lon={}", stationId, latitude, longitude);

        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationId));

        // Update coordinates
        station.setLatitude(latitude);
        station.setLongitude(longitude);

        // Auto-calculate grid if not provided
        if (grid == null || grid.isBlank()) {
            grid = maidenheadConverter.toMaidenhead(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                6 // Default to 6-character precision
            );
            log.debug("Auto-calculated grid: {}", grid);
        }

        station.setMaidenheadGrid(grid);

        if (locationName != null) {
            station.setLocationName(locationName);
        }

        Station updated = stationRepository.save(station);

        return LocationUpdateResponse.builder()
                .success(true)
                .latitude(updated.getLatitude())
                .longitude(updated.getLongitude())
                .grid(updated.getMaidenheadGrid())
                .locationName(updated.getLocationName())
                .source("STATION")
                .message("Station location updated successfully")
                .build();
    }

    /**
     * Update user default location by username
     */
    @Transactional
    public LocationUpdateResponse updateUserLocation(
            String username,
            Double latitude,
            Double longitude,
            String grid
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return updateUserLocation(user.getId(), latitude, longitude, grid);
    }

    /**
     * Update user default location
     *
     * @param userId User ID
     * @param latitude Latitude
     * @param longitude Longitude
     * @param grid Maidenhead grid (optional, auto-calculated if null)
     * @return Updated user location
     */
    @Transactional
    public LocationUpdateResponse updateUserLocation(
            Long userId,
            Double latitude,
            Double longitude,
            String grid
    ) {
        log.info("Updating user {} default location to lat={}, lon={}", userId, latitude, longitude);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Update coordinates
        user.setDefaultLatitude(latitude);
        user.setDefaultLongitude(longitude);

        // Auto-calculate grid if not provided
        if (grid == null || grid.isBlank()) {
            grid = maidenheadConverter.toMaidenhead(
                BigDecimal.valueOf(latitude),
                BigDecimal.valueOf(longitude),
                6 // Default to 6-character precision
            );
            log.debug("Auto-calculated grid: {}", grid);
        }

        user.setDefaultGrid(grid);

        User updated = userRepository.save(user);

        return LocationUpdateResponse.builder()
                .success(true)
                .latitude(updated.getDefaultLatitude())
                .longitude(updated.getDefaultLongitude())
                .grid(updated.getDefaultGrid())
                .source("USER")
                .message("User default location updated successfully")
                .build();
    }

    /**
     * Get station location
     */
    @Transactional(readOnly = true)
    public LocationInfo getStationLocation(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found: " + stationId));

        if (station.getLatitude() == null || station.getLongitude() == null) {
            return null;
        }

        return LocationInfo.builder()
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .grid(station.getMaidenheadGrid())
                .locationName(station.getLocationName())
                .source("STATION")
                .build();
    }

    /**
     * Get user default location
     */
    @Transactional(readOnly = true)
    public LocationInfo getUserLocation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getDefaultLatitude() == null || user.getDefaultLongitude() == null) {
            return null;
        }

        return LocationInfo.builder()
                .latitude(user.getDefaultLatitude())
                .longitude(user.getDefaultLongitude())
                .grid(user.getDefaultGrid())
                .source("USER")
                .build();
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class LocationUpdateResponse {
        private boolean success;
        private Double latitude;
        private Double longitude;
        private String grid;
        private String locationName;
        private String source; // STATION, USER, SESSION
        private String message;
    }

    @Data
    @Builder
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
        private String grid;
        private String locationName;
        private String source;
    }
}
