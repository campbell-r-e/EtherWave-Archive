package com.hamradio.logbook.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Distance and bearing calculator using Haversine formula
 * Calculates great-circle distances between two points on Earth
 */
@Service
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double EARTH_RADIUS_MI = 3958.8;
    private static final BigDecimal KM_TO_MI = BigDecimal.valueOf(0.621371);

    /**
     * Calculate distance between two points using Haversine formula
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers and miles
     */
    public DistanceResult calculate(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1.doubleValue())) *
                   Math.cos(Math.toRadians(lat2.doubleValue())) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distanceKm = EARTH_RADIUS_KM * c;
        double distanceMi = EARTH_RADIUS_MI * c;

        BigDecimal bearing = calculateBearing(lat1, lon1, lat2, lon2);

        return new DistanceResult(
            BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP),
            BigDecimal.valueOf(distanceMi).setScale(2, RoundingMode.HALF_UP),
            bearing
        );
    }

    /**
     * Calculate bearing from point 1 to point 2
     *
     * @return Bearing in degrees (0-360)
     */
    public BigDecimal calculateBearing(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double y = Math.sin(dLon) * Math.cos(lat2Rad);
        double x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                   Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon);

        double bearing = Math.toDegrees(Math.atan2(y, x));

        // Normalize to 0-360
        bearing = (bearing + 360) % 360;

        return BigDecimal.valueOf(bearing).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate distance from Maidenhead grid squares
     */
    public DistanceResult calculateFromGrids(String grid1, String grid2, MaidenheadConverter converter) {
        MaidenheadConverter.GridLocation loc1 = converter.fromMaidenhead(grid1);
        MaidenheadConverter.GridLocation loc2 = converter.fromMaidenhead(grid2);

        return calculate(loc1.getLat(), loc1.getLon(), loc2.getLat(), loc2.getLon());
    }

    /**
     * Calculate antipodal point (opposite side of Earth)
     * Useful for propagation calculations
     */
    public AntipodalPoint getAntipodalPoint(BigDecimal lat, BigDecimal lon) {
        BigDecimal antipodalLat = lat.negate();
        BigDecimal antipodalLon = lon.add(BigDecimal.valueOf(180));

        // Normalize longitude to -180 to 180
        if (antipodalLon.compareTo(BigDecimal.valueOf(180)) > 0) {
            antipodalLon = antipodalLon.subtract(BigDecimal.valueOf(360));
        }

        return new AntipodalPoint(antipodalLat, antipodalLon);
    }

    /**
     * Check if a point is within a certain distance of another point
     */
    public boolean isWithinDistance(BigDecimal lat1, BigDecimal lon1,
                                   BigDecimal lat2, BigDecimal lon2,
                                   BigDecimal maxDistanceKm) {
        DistanceResult result = calculate(lat1, lon1, lat2, lon2);
        return result.getDistanceKm().compareTo(maxDistanceKm) <= 0;
    }

    /**
     * Calculate bounding box for a given point and radius
     * Returns min/max lat/lon for rectangular search area
     */
    public BoundingBox getBoundingBox(BigDecimal lat, BigDecimal lon, BigDecimal radiusKm) {
        // Rough approximation: 1 degree latitude ≈ 111 km
        // 1 degree longitude ≈ 111 km * cos(latitude)
        double latDelta = radiusKm.doubleValue() / 111.0;
        double lonDelta = radiusKm.doubleValue() / (111.0 * Math.cos(Math.toRadians(lat.doubleValue())));

        return new BoundingBox(
            lat.subtract(BigDecimal.valueOf(latDelta)).setScale(6, RoundingMode.HALF_UP),
            lat.add(BigDecimal.valueOf(latDelta)).setScale(6, RoundingMode.HALF_UP),
            lon.subtract(BigDecimal.valueOf(lonDelta)).setScale(6, RoundingMode.HALF_UP),
            lon.add(BigDecimal.valueOf(lonDelta)).setScale(6, RoundingMode.HALF_UP)
        );
    }

    @Data
    @AllArgsConstructor
    public static class DistanceResult {
        private BigDecimal distanceKm;
        private BigDecimal distanceMi;
        private BigDecimal bearing;
    }

    @Data
    @AllArgsConstructor
    public static class AntipodalPoint {
        private BigDecimal lat;
        private BigDecimal lon;
    }

    @Data
    @AllArgsConstructor
    public static class BoundingBox {
        private BigDecimal minLat;
        private BigDecimal maxLat;
        private BigDecimal minLon;
        private BigDecimal maxLon;
    }
}
