package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for map data operations including clustering and location data
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MapDataService {

    private final QSOLocationRepository qsoLocationRepository;
    private final QSORepository qsoRepository;
    private final MapClusterRepository mapClusterRepository;
    private final MaidenheadConverter maidenheadConverter;
    private final DistanceCalculator distanceCalculator;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final int CLUSTERING_THRESHOLD = 10000;
    private static final Map<Integer, Integer> ZOOM_PIXEL_RADIUS = Map.ofEntries(
        Map.entry(0, 80), Map.entry(1, 80), Map.entry(2, 80), Map.entry(3, 80),
        Map.entry(4, 80), Map.entry(5, 80), Map.entry(6, 80), Map.entry(7, 80),
        Map.entry(8, 80), Map.entry(9, 65), Map.entry(10, 65), Map.entry(11, 65),
        Map.entry(12, 65), Map.entry(13, 50), Map.entry(14, 50), Map.entry(15, 50),
        Map.entry(16, 0), Map.entry(17, 0), Map.entry(18, 0)  // No clustering at highest zoom levels
    );

    /**
     * Get QSO location data with adaptive clustering
     */
    @Transactional(readOnly = true)
    public MapDataResponse getQSOLocations(Long logId, int zoomLevel, MapFilters filters, String bounds) {
        log.info("Getting QSO locations for log {} at zoom level {}", logId, zoomLevel);

        // Generate filter hash for caching
        String filterHash = generateFilterHash(filters);

        // Get all QSOs for log with filters
        List<QSO> qsos = getFilteredQSOs(logId, filters);

        // Check if we need clustering
        boolean shouldCluster = qsos.size() > CLUSTERING_THRESHOLD && ZOOM_PIXEL_RADIUS.get(zoomLevel) > 0;

        if (shouldCluster) {
            // Try cache first
            List<MapCluster> cached = mapClusterRepository.findByLogIdAndZoomAndFilterHash(logId, zoomLevel, filterHash);
            if (!cached.isEmpty() && !isCacheStale(cached.get(0))) {
                log.info("Cache hit for log {} zoom {} - {} clusters", logId, zoomLevel, cached.size());
                return buildClusteredResponse(cached);
            }

            // Cache miss - compute clusters
            log.info("Cache miss for log {} zoom {} - computing clusters", logId, zoomLevel);
            List<ClusterData> clusters = computeClusters(qsos, zoomLevel, filterHash);

            // Save to cache
            // saveClustersToDB(logId, zoomLevel, filterHash, clusters);

            return ClusteredMapDataResponse.builder()
                .type("clustered")
                .clusters(clusters)
                .metadata(MapMetadata.builder()
                    .totalQsos(qsos.size())
                    .filteredQsos(qsos.size())
                    .clusteringApplied(true)
                    .cacheHit(false)
                    .build())
                .build();
        } else {
            // Return individual QSO markers
            log.info("Returning {} individual QSO markers", qsos.size());
            List<QSOLocationData> individual = qsos.stream()
                .map(this::convertToLocationData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            return IndividualMapDataResponse.builder()
                .type("individual")
                .individual(individual)
                .metadata(MapMetadata.builder()
                    .totalQsos(qsos.size())
                    .filteredQsos(individual.size())
                    .clusteringApplied(false)
                    .cacheHit(false)
                    .build())
                .build();
        }
    }

    /**
     * Get QSO location by QSO ID
     */
    @Transactional(readOnly = true)
    public QSOLocation getQSOLocationByQsoId(Long qsoId) {
        Optional<QSOLocation> location = qsoLocationRepository.findByQsoId(qsoId);
        if (location.isPresent()) {
            return location.get();
        }

        // Try to create it if it doesn't exist
        Optional<QSO> qso = Optional.ofNullable(qsoRepository.findById(qsoId).orElse(null));
        if (qso.isPresent()) {
            return getOrCreateQSOLocation(qso.get());
        }

        return null;
    }

    /**
     * Get or create QSO location data
     */
    @Transactional
    public QSOLocation getOrCreateQSOLocation(QSO qso) {
        Optional<QSOLocation> existing = qsoLocationRepository.findByQsoId(qso.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        return createQSOLocation(qso);
    }

    /**
     * Create QSOLocation from QSO
     */
    @Transactional
    public QSOLocation createQSOLocation(QSO qso) {
        log.debug("Creating QSOLocation for QSO {}", qso.getId());

        // Get operator location (hierarchical fallback)
        LocationData operatorLoc = getOperatorLocation(qso);

        // Get contact location from grid square
        LocationData contactLoc = getContactLocation(qso);

        if (contactLoc == null) {
            log.warn("No contact location for QSO {} - skipping", qso.getId());
            return null;
        }

        // Calculate distance
        DistanceCalculator.DistanceResult distance = null;
        if (operatorLoc != null) {
            distance = distanceCalculator.calculate(
                operatorLoc.getLat(), operatorLoc.getLon(),
                contactLoc.getLat(), contactLoc.getLon()
            );
        }

        QSOLocation qsoLocation = QSOLocation.builder()
            .qso(qso)
            .operatorLat(operatorLoc != null ? operatorLoc.getLat() : null)
            .operatorLon(operatorLoc != null ? operatorLoc.getLon() : null)
            .operatorGrid(operatorLoc != null ? operatorLoc.getGrid() : null)
            .locationSource(operatorLoc != null ? operatorLoc.getSource() : null)
            .contactLat(contactLoc.getLat())
            .contactLon(contactLoc.getLon())
            .contactGrid(qso.getGridSquare())
            .contactDxcc(qso.getDxcc() != null ? qso.getDxcc().toString() : null)
            .contactContinent(qso.getCountry() != null ? getContinent(qso.getCountry()) : null)
            .contactCqZone(qso.getCqZone())
            .contactItuZone(qso.getItuZone())
            .distanceKm(distance != null ? distance.getDistanceKm() : null)
            .distanceMi(distance != null ? distance.getDistanceMi() : null)
            .bearing(distance != null ? distance.getBearing() : null)
            .build();

        return qsoLocationRepository.save(qsoLocation);
    }

    /**
     * Get operator location with hierarchical fallback
     */
    private LocationData getOperatorLocation(QSO qso) {
        // 1. Try station location
        if (qso.getStation() != null) {
            Station station = stationRepository.findById(qso.getStation().getId()).orElse(null);
            if (station != null && station.getLatitude() != null && station.getLongitude() != null) {
                return LocationData.builder()
                    .lat(BigDecimal.valueOf(station.getLatitude()))
                    .lon(BigDecimal.valueOf(station.getLongitude()))
                    .grid(station.getMaidenheadGrid())
                    .source(QSOLocation.LocationSource.STATION)
                    .build();
            }
        }

        // 2. Try user default location via operator's callsign
        if (qso.getOperator() != null && qso.getOperator().getCallsign() != null) {
            var userOpt = userRepository.findByCallsign(qso.getOperator().getCallsign());
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                if (user.getDefaultLatitude() != null && user.getDefaultLongitude() != null) {
                    return LocationData.builder()
                        .lat(BigDecimal.valueOf(user.getDefaultLatitude()))
                        .lon(BigDecimal.valueOf(user.getDefaultLongitude()))
                        .grid(user.getDefaultGrid())
                        .source(QSOLocation.LocationSource.USER)
                        .build();
                }
            }
        }

        // 3. Session location would be stored in session context
        // Not implemented yet

        return null;
    }

    /**
     * Get contact location from grid square
     */
    private LocationData getContactLocation(QSO qso) {
        if (qso.getGridSquare() == null || qso.getGridSquare().isBlank()) {
            return null;
        }

        try {
            MaidenheadConverter.GridLocation gridLoc = maidenheadConverter.fromMaidenhead(qso.getGridSquare());
            return LocationData.builder()
                .lat(gridLoc.getLat())
                .lon(gridLoc.getLon())
                .grid(qso.getGridSquare())
                .source(null)
                .build();
        } catch (Exception e) {
            log.warn("Failed to convert grid square {} for QSO {}: {}", qso.getGridSquare(), qso.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Compute clusters for QSOs
     */
    private List<ClusterData> computeClusters(List<QSO> qsos, int zoomLevel, String filterHash) {
        int pixelRadius = ZOOM_PIXEL_RADIUS.get(zoomLevel);

        // Convert pixel radius to lat/lon degrees (approximate)
        // At equator: 1 pixel ≈ 156543.03 meters / (256 * 2^zoom)
        // pixelRadius pixels ≈ pixelRadius * 156543.03 / (256 * 2^zoom) meters
        double metersPerPixel = 156543.03 / (256 * Math.pow(2, zoomLevel));
        double clusterRadiusMeters = pixelRadius * metersPerPixel;
        double clusterRadiusKm = clusterRadiusMeters / 1000.0;

        // Simple clustering algorithm: group nearby QSOs
        List<ClusterData> clusters = new ArrayList<>();
        Set<Long> clustered = new HashSet<>();

        for (QSO qso : qsos) {
            if (clustered.contains(qso.getId())) {
                continue;
            }

            QSOLocation loc = qsoLocationRepository.findByQsoId(qso.getId()).orElse(null);
            if (loc == null || loc.getContactLat() == null || loc.getContactLon() == null) {
                continue;
            }

            // Find nearby QSOs
            List<QSO> nearbyQSOs = new ArrayList<>();
            nearbyQSOs.add(qso);
            clustered.add(qso.getId());

            for (QSO other : qsos) {
                if (clustered.contains(other.getId())) {
                    continue;
                }

                QSOLocation otherLoc = qsoLocationRepository.findByQsoId(other.getId()).orElse(null);
                if (otherLoc == null || otherLoc.getContactLat() == null || otherLoc.getContactLon() == null) {
                    continue;
                }

                // Check distance
                boolean isNearby = distanceCalculator.isWithinDistance(
                    loc.getContactLat(), loc.getContactLon(),
                    otherLoc.getContactLat(), otherLoc.getContactLon(),
                    BigDecimal.valueOf(clusterRadiusKm)
                );

                if (isNearby) {
                    nearbyQSOs.add(other);
                    clustered.add(other.getId());
                }
            }

            // Create cluster
            clusters.add(buildCluster(nearbyQSOs, loc.getContactLat(), loc.getContactLon()));
        }

        log.info("Created {} clusters from {} QSOs", clusters.size(), qsos.size());
        return clusters;
    }

    /**
     * Build cluster data from group of QSOs
     */
    private ClusterData buildCluster(List<QSO> qsos, BigDecimal lat, BigDecimal lon) {
        Map<String, Integer> stations = new HashMap<>();
        Map<String, Integer> bands = new HashMap<>();
        Map<String, Integer> modes = new HashMap<>();

        for (QSO qso : qsos) {
            // Station breakdown
            String stationKey = qso.getIsGota() ? "GOTA" : String.valueOf(qso.getStationNumber());
            stations.put(stationKey, stations.getOrDefault(stationKey, 0) + 1);

            // Band breakdown
            if (qso.getBand() != null) {
                bands.put(qso.getBand(), bands.getOrDefault(qso.getBand(), 0) + 1);
            }

            // Mode breakdown
            if (qso.getMode() != null) {
                modes.put(qso.getMode(), modes.getOrDefault(qso.getMode(), 0) + 1);
            }
        }

        return ClusterData.builder()
            .lat(lat)
            .lon(lon)
            .count(qsos.size())
            .stations(stations)
            .bands(bands)
            .modes(modes)
            .build();
    }

    /**
     * Convert QSO to location data
     */
    private QSOLocationData convertToLocationData(QSO qso) {
        QSOLocation loc = qsoLocationRepository.findByQsoId(qso.getId()).orElse(null);
        if (loc == null) {
            // Try to create it
            loc = createQSOLocation(qso);
        }

        if (loc == null || loc.getContactLat() == null || loc.getContactLon() == null) {
            return null;
        }

        return QSOLocationData.builder()
            .qsoId(qso.getId())
            .callsign(qso.getCallsign())
            .lat(loc.getContactLat())
            .lon(loc.getContactLon())
            .grid(qso.getGridSquare())
            .dxcc(loc.getContactDxcc())
            .continent(loc.getContactContinent())
            .band(qso.getBand())
            .mode(qso.getMode())
            .station(qso.getStationNumber())
            .timestamp(qso.getCreatedAt())
            .confirmed(isConfirmed(qso))
            .distance(loc.getDistanceKm())
            .build();
    }

    /**
     * Check if QSO is confirmed
     */
    private boolean isConfirmed(QSO qso) {
        return "Y".equalsIgnoreCase(qso.getQslRcvd()) || "Y".equalsIgnoreCase(qso.getLotwRcvd());
    }

    /**
     * Get filtered QSOs based on map filters
     */
    public List<QSO> getFilteredQSOs(Long logId, MapFilters filters) {
        // Start with all QSOs for log
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        if (filters == null) {
            return qsos;
        }

        return qsos.stream()
            .filter(qso -> filters.getBand() == null || filters.getBand().equalsIgnoreCase(qso.getBand()))
            .filter(qso -> filters.getMode() == null || filters.getMode().equalsIgnoreCase(qso.getMode()))
            .filter(qso -> filters.getStation() == null || filters.getStation().equals(qso.getStationNumber()))
            .filter(qso -> filters.getOperator() == null ||
                (qso.getOperator() != null && filters.getOperator().equalsIgnoreCase(qso.getOperator().getCallsign())))
            .filter(qso -> filters.getState() == null || filters.getState().equalsIgnoreCase(qso.getState()))
            .filter(qso -> filters.getContinent() == null ||
                filters.getContinent().equalsIgnoreCase(getContinent(qso.getCountry())))
            .filter(qso -> filters.getConfirmed() == null || filters.getConfirmed() == isConfirmed(qso))
            .filter(qso -> filters.getDateFrom() == null || !qso.getQsoDate().isBefore(filters.getDateFrom()))
            .filter(qso -> filters.getDateTo() == null || !qso.getQsoDate().isAfter(filters.getDateTo()))
            .collect(Collectors.toList());
    }

    /**
     * Generate filter hash for caching
     */
    private String generateFilterHash(MapFilters filters) {
        if (filters == null) {
            return "default";
        }

        try {
            String filterJson = objectMapper.writeValueAsString(filters);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(filterJson.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.warn("Failed to generate filter hash: {}", e.getMessage());
            return "error";
        }
    }

    /**
     * Check if cache is stale (older than 5 minutes)
     */
    private boolean isCacheStale(MapCluster cluster) {
        return cluster.getUpdatedAt().plusMinutes(5).isBefore(java.time.LocalDateTime.now());
    }

    /**
     * Build clustered response from cached clusters
     */
    private MapDataResponse buildClusteredResponse(List<MapCluster> cached) {
        List<ClusterData> clusters = cached.stream()
            .map(this::convertClusterToData)
            .collect(Collectors.toList());

        int totalQsos = clusters.stream().mapToInt(ClusterData::getCount).sum();

        return ClusteredMapDataResponse.builder()
            .type("clustered")
            .clusters(clusters)
            .metadata(MapMetadata.builder()
                .totalQsos(totalQsos)
                .filteredQsos(totalQsos)
                .clusteringApplied(true)
                .cacheHit(true)
                .build())
            .build();
    }

    /**
     * Convert MapCluster entity to ClusterData DTO
     */
    private ClusterData convertClusterToData(MapCluster cluster) {
        try {
            return ClusterData.builder()
                .lat(cluster.getLat())
                .lon(cluster.getLon())
                .count(cluster.getQsoCount())
                .stations(objectMapper.readValue(cluster.getStationBreakdown(), Map.class))
                .bands(objectMapper.readValue(cluster.getBandBreakdown(), Map.class))
                .modes(objectMapper.readValue(cluster.getModeBreakdown(), Map.class))
                .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse cluster breakdown JSON", e);
            return ClusterData.builder()
                .lat(cluster.getLat())
                .lon(cluster.getLon())
                .count(cluster.getQsoCount())
                .stations(Map.of())
                .bands(Map.of())
                .modes(Map.of())
                .build();
        }
    }

    /**
     * Get continent code from country name (simple mapping)
     */
    private String getContinent(String country) {
        if (country == null) return null;

        // Simple continent mapping - this should be expanded with full DXCC data
        return switch (country.toUpperCase()) {
            case "USA", "UNITED STATES", "CANADA", "MEXICO" -> "NA";
            case "GERMANY", "FRANCE", "UNITED KINGDOM", "SPAIN", "ITALY" -> "EU";
            case "JAPAN", "CHINA", "INDIA", "SOUTH KOREA" -> "AS";
            case "AUSTRALIA", "NEW ZEALAND" -> "OC";
            case "BRAZIL", "ARGENTINA", "CHILE" -> "SA";
            case "SOUTH AFRICA", "EGYPT", "KENYA" -> "AF";
            default -> "NA"; // Default to North America
        };
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class MapFilters {
        private String band;
        private String mode;
        private Integer station;
        private String operator;
        private String dxcc;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private Boolean confirmed;
        private String continent;
        private String state;
        private String exchange;
    }

    @Data
    @Builder
    public static class LocationData {
        private BigDecimal lat;
        private BigDecimal lon;
        private String grid;
        private QSOLocation.LocationSource source;
    }

    @Data
    @Builder
    public static class ClusterData {
        private BigDecimal lat;
        private BigDecimal lon;
        private int count;
        private Map<String, Integer> stations;
        private Map<String, Integer> bands;
        private Map<String, Integer> modes;
    }

    @Data
    @Builder
    public static class QSOLocationData {
        private Long qsoId;
        private String callsign;
        private BigDecimal lat;
        private BigDecimal lon;
        private String grid;
        private String dxcc;
        private String continent;
        private String band;
        private String mode;
        private Integer station;
        private java.time.LocalDateTime timestamp;
        private boolean confirmed;
        private BigDecimal distance;
    }

    @Data
    @Builder
    public static class MapMetadata {
        private int totalQsos;
        private int filteredQsos;
        private boolean clusteringApplied;
        private boolean cacheHit;
    }

    // Response types
    public interface MapDataResponse {
        String getType();
        MapMetadata getMetadata();
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ClusteredMapDataResponse implements MapDataResponse {
        private String type;
        private List<ClusterData> clusters;
        private MapMetadata metadata;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class IndividualMapDataResponse implements MapDataResponse {
        private String type;
        private List<QSOLocationData> individual;
        private MapMetadata metadata;
    }
}
