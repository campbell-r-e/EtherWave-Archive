package com.hamradio.logbook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DistanceCalculator Unit Tests")
class DistanceCalculatorTest {

    private DistanceCalculator calculator;
    private MaidenheadConverter converter;

    @BeforeEach
    void setUp() {
        calculator = new DistanceCalculator();
        converter = new MaidenheadConverter();
    }

    // ===== calculate() tests =====

    @Test
    @DisplayName("Should calculate distance between New York and Los Angeles (~3940 km)")
    void shouldCalculateNYtoLA() {
        // New York: 40.7128°N, -74.0060°W
        // Los Angeles: 34.0522°N, -118.2437°W
        BigDecimal lat1 = BigDecimal.valueOf(40.7128);
        BigDecimal lon1 = BigDecimal.valueOf(-74.0060);
        BigDecimal lat2 = BigDecimal.valueOf(34.0522);
        BigDecimal lon2 = BigDecimal.valueOf(-118.2437);

        DistanceCalculator.DistanceResult result = calculator.calculate(lat1, lon1, lat2, lon2);

        assertNotNull(result);
        // NY to LA is about 3940 km
        double distKm = result.getDistanceKm().doubleValue();
        assertTrue(distKm > 3900 && distKm < 4000,
            "Expected ~3940 km, got: " + distKm);
        // Distance in miles should be consistent
        double distMi = result.getDistanceMi().doubleValue();
        assertTrue(distMi > 2400 && distMi < 2500,
            "Expected ~2445 mi, got: " + distMi);
    }

    @Test
    @DisplayName("Should calculate zero distance for same point")
    void shouldCalculateZeroDistanceForSamePoint() {
        BigDecimal lat = BigDecimal.valueOf(40.7);
        BigDecimal lon = BigDecimal.valueOf(-74.0);

        DistanceCalculator.DistanceResult result = calculator.calculate(lat, lon, lat, lon);

        assertEquals(0.00, result.getDistanceKm().doubleValue(), 0.01);
        assertEquals(0.00, result.getDistanceMi().doubleValue(), 0.01);
    }

    @Test
    @DisplayName("Should calculate distance between London and Paris (~340 km)")
    void shouldCalculateLondonToParis() {
        // London: 51.5074°N, -0.1278°W
        // Paris: 48.8566°N, 2.3522°E
        BigDecimal lat1 = BigDecimal.valueOf(51.5074);
        BigDecimal lon1 = BigDecimal.valueOf(-0.1278);
        BigDecimal lat2 = BigDecimal.valueOf(48.8566);
        BigDecimal lon2 = BigDecimal.valueOf(2.3522);

        DistanceCalculator.DistanceResult result = calculator.calculate(lat1, lon1, lat2, lon2);

        double distKm = result.getDistanceKm().doubleValue();
        assertTrue(distKm > 330 && distKm < 350,
            "Expected ~340 km, got: " + distKm);
    }

    @Test
    @DisplayName("Should include bearing in result")
    void shouldIncludeBearingInResult() {
        BigDecimal lat1 = BigDecimal.valueOf(40.7);
        BigDecimal lon1 = BigDecimal.valueOf(-74.0);
        BigDecimal lat2 = BigDecimal.valueOf(34.0);
        BigDecimal lon2 = BigDecimal.valueOf(-118.2);

        DistanceCalculator.DistanceResult result = calculator.calculate(lat1, lon1, lat2, lon2);

        assertNotNull(result.getBearing());
        // LA is to the southwest of NY, bearing should be around 270-290 degrees
        double bearing = result.getBearing().doubleValue();
        assertTrue(bearing >= 0 && bearing < 360,
            "Bearing should be 0-360, got: " + bearing);
    }

    // ===== calculateBearing() tests =====

    @Test
    @DisplayName("Should calculate northward bearing as ~0°")
    void shouldCalculateNorthBearing() {
        BigDecimal lat1 = BigDecimal.valueOf(40.0);
        BigDecimal lon1 = BigDecimal.valueOf(-74.0);
        BigDecimal lat2 = BigDecimal.valueOf(50.0); // same longitude, further north
        BigDecimal lon2 = BigDecimal.valueOf(-74.0);

        BigDecimal bearing = calculator.calculateBearing(lat1, lon1, lat2, lon2);

        double b = bearing.doubleValue();
        assertTrue(b < 5.0 || b > 355.0, // ~0°
            "Expected ~0°, got: " + b);
    }

    @Test
    @DisplayName("Should calculate eastward bearing as ~90°")
    void shouldCalculateEastBearing() {
        BigDecimal lat1 = BigDecimal.valueOf(0.0);
        BigDecimal lon1 = BigDecimal.valueOf(0.0);
        BigDecimal lat2 = BigDecimal.valueOf(0.0); // same latitude
        BigDecimal lon2 = BigDecimal.valueOf(10.0); // east

        BigDecimal bearing = calculator.calculateBearing(lat1, lon1, lat2, lon2);

        double b = bearing.doubleValue();
        assertEquals(90.0, b, 1.0, "Expected ~90°, got: " + b);
    }

    @Test
    @DisplayName("Should calculate southward bearing as ~180°")
    void shouldCalculateSouthBearing() {
        BigDecimal lat1 = BigDecimal.valueOf(40.0);
        BigDecimal lon1 = BigDecimal.valueOf(-74.0);
        BigDecimal lat2 = BigDecimal.valueOf(30.0); // same longitude, further south
        BigDecimal lon2 = BigDecimal.valueOf(-74.0);

        BigDecimal bearing = calculator.calculateBearing(lat1, lon1, lat2, lon2);

        double b = bearing.doubleValue();
        assertEquals(180.0, b, 1.0, "Expected ~180°, got: " + b);
    }

    @Test
    @DisplayName("Should calculate westward bearing as ~270°")
    void shouldCalculateWestBearing() {
        BigDecimal lat1 = BigDecimal.valueOf(0.0);
        BigDecimal lon1 = BigDecimal.valueOf(0.0);
        BigDecimal lat2 = BigDecimal.valueOf(0.0);
        BigDecimal lon2 = BigDecimal.valueOf(-10.0); // west

        BigDecimal bearing = calculator.calculateBearing(lat1, lon1, lat2, lon2);

        double b = bearing.doubleValue();
        assertEquals(270.0, b, 1.0, "Expected ~270°, got: " + b);
    }

    @Test
    @DisplayName("Should normalize bearing to 0-360 range")
    void shouldNormalizeBearingTo0to360() {
        BigDecimal lat1 = BigDecimal.valueOf(40.7);
        BigDecimal lon1 = BigDecimal.valueOf(-74.0);
        BigDecimal lat2 = BigDecimal.valueOf(34.0);
        BigDecimal lon2 = BigDecimal.valueOf(-118.2);

        BigDecimal bearing = calculator.calculateBearing(lat1, lon1, lat2, lon2);

        double b = bearing.doubleValue();
        assertTrue(b >= 0 && b < 360, "Bearing should be 0-360, got: " + b);
    }

    // ===== calculateFromGrids() tests =====

    @Test
    @DisplayName("Should calculate distance from grid squares")
    void shouldCalculateFromGrids() {
        // FN31 is New England area, EM72 is Texas area
        DistanceCalculator.DistanceResult result =
            calculator.calculateFromGrids("FN31", "EM72", converter);

        assertNotNull(result);
        double distKm = result.getDistanceKm().doubleValue();
        // Should be > 1000 km and < 3000 km (rough range from NE to TX)
        assertTrue(distKm > 1000 && distKm < 3000,
            "Expected 1000-3000 km, got: " + distKm);
    }

    @Test
    @DisplayName("Should calculate zero distance for same grid")
    void shouldCalculateZeroForSameGrid() {
        DistanceCalculator.DistanceResult result =
            calculator.calculateFromGrids("FN31", "FN31", converter);

        assertNotNull(result);
        assertEquals(0.00, result.getDistanceKm().doubleValue(), 0.01);
    }

    // ===== isWithinDistance() tests =====

    @Test
    @DisplayName("Should return true when within distance")
    void shouldReturnTrueWhenWithinDistance() {
        BigDecimal lat1 = BigDecimal.valueOf(40.0);
        BigDecimal lon1 = BigDecimal.valueOf(-74.0);
        BigDecimal lat2 = BigDecimal.valueOf(40.5);
        BigDecimal lon2 = BigDecimal.valueOf(-74.0);

        boolean result = calculator.isWithinDistance(lat1, lon1, lat2, lon2,
            BigDecimal.valueOf(100)); // 100 km

        assertTrue(result, "Points ~55 km apart should be within 100 km");
    }

    @Test
    @DisplayName("Should return false when outside distance")
    void shouldReturnFalseWhenOutsideDistance() {
        // New York to Los Angeles (~3940 km)
        BigDecimal lat1 = BigDecimal.valueOf(40.7);
        BigDecimal lon1 = BigDecimal.valueOf(-74.0);
        BigDecimal lat2 = BigDecimal.valueOf(34.0);
        BigDecimal lon2 = BigDecimal.valueOf(-118.2);

        boolean result = calculator.isWithinDistance(lat1, lon1, lat2, lon2,
            BigDecimal.valueOf(100)); // 100 km

        assertFalse(result, "NY to LA should not be within 100 km");
    }

    @Test
    @DisplayName("Should return true when distance exactly equals max")
    void shouldReturnTrueWhenExactlyAtMaxDistance() {
        // Identical points, 0 km distance
        BigDecimal lat = BigDecimal.valueOf(40.0);
        BigDecimal lon = BigDecimal.valueOf(-74.0);

        boolean result = calculator.isWithinDistance(lat, lon, lat, lon,
            BigDecimal.valueOf(0));

        assertTrue(result, "Same point should be within 0 km");
    }

    // ===== getBoundingBox() tests =====

    @Test
    @DisplayName("Should return valid bounding box for 100km radius")
    void shouldGetBoundingBox100km() {
        BigDecimal lat = BigDecimal.valueOf(40.0);
        BigDecimal lon = BigDecimal.valueOf(-74.0);
        BigDecimal radius = BigDecimal.valueOf(100);

        DistanceCalculator.BoundingBox box = calculator.getBoundingBox(lat, lon, radius);

        assertNotNull(box);
        assertTrue(box.getMinLat().compareTo(lat) < 0, "minLat should be south of point");
        assertTrue(box.getMaxLat().compareTo(lat) > 0, "maxLat should be north of point");
        assertTrue(box.getMinLon().compareTo(lon) < 0, "minLon should be west of point");
        assertTrue(box.getMaxLon().compareTo(lon) > 0, "maxLon should be east of point");
    }

    @Test
    @DisplayName("Should have approximately correct bounding box dimensions")
    void shouldHaveCorrectBoundingBoxDimensions() {
        BigDecimal lat = BigDecimal.valueOf(0.0); // equator
        BigDecimal lon = BigDecimal.valueOf(0.0);
        BigDecimal radius = BigDecimal.valueOf(111); // ~1 degree at equator

        DistanceCalculator.BoundingBox box = calculator.getBoundingBox(lat, lon, radius);

        double latRange = box.getMaxLat().doubleValue() - box.getMinLat().doubleValue();
        double lonRange = box.getMaxLon().doubleValue() - box.getMinLon().doubleValue();

        // Should be approximately 2 degrees (2 * ~1 degree)
        assertEquals(2.0, latRange, 0.1, "Lat range should be ~2 degrees");
        assertEquals(2.0, lonRange, 0.1, "Lon range should be ~2 degrees at equator");
    }

    // ===== getAntipodalPoint() tests =====

    @Test
    @DisplayName("Should calculate antipodal point for New York")
    void shouldCalculateAntipodalPoint() {
        BigDecimal lat = BigDecimal.valueOf(40.7);
        BigDecimal lon = BigDecimal.valueOf(-74.0);

        DistanceCalculator.AntipodalPoint antipodal = calculator.getAntipodalPoint(lat, lon);

        // Antipodal of NY: ~-40.7°S, 106°E
        assertEquals(-40.7, antipodal.getLat().doubleValue(), 0.01);
        assertEquals(106.0, antipodal.getLon().doubleValue(), 0.1);
    }

    @Test
    @DisplayName("Should normalize antipodal longitude to -180 to 180")
    void shouldNormalizeAntipodalLongitude() {
        // Point at 30°N, 100°E → antipodal at -30°S, -80°W (should be -80, not 280)
        BigDecimal lat = BigDecimal.valueOf(30.0);
        BigDecimal lon = BigDecimal.valueOf(100.0);

        DistanceCalculator.AntipodalPoint antipodal = calculator.getAntipodalPoint(lat, lon);

        assertTrue(antipodal.getLon().doubleValue() >= -180 && antipodal.getLon().doubleValue() <= 180,
            "Lon should be in -180..180 range, got: " + antipodal.getLon());
    }
}
