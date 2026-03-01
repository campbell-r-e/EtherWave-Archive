package com.hamradio.logbook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MaidenheadConverter Unit Tests")
class MaidenheadConverterTest {

    private MaidenheadConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MaidenheadConverter();
    }

    // ===== toMaidenhead tests =====

    @Test
    @DisplayName("Should convert known location to 4-char grid (New York area)")
    void shouldConvertToMaidenhead4CharNewYork() {
        // New York City: ~40.7°N, -74.0°W
        BigDecimal lat = BigDecimal.valueOf(40.7);
        BigDecimal lon = BigDecimal.valueOf(-74.0);
        String grid = converter.toMaidenhead(lat, lon, 4);
        assertEquals("FN30", grid);
    }

    @Test
    @DisplayName("Should convert known location to 6-char grid")
    void shouldConvertToMaidenhead6Char() {
        // W1AW (ARRL HQ) Newington, CT: ~41.715°N, -72.728°W
        BigDecimal lat = BigDecimal.valueOf(41.715);
        BigDecimal lon = BigDecimal.valueOf(-72.728);
        String grid = converter.toMaidenhead(lat, lon, 6);
        // Should be FN31 area
        assertTrue(grid.startsWith("FN31"), "Expected FN31, got: " + grid);
        assertEquals(6, grid.length());
    }

    @Test
    @DisplayName("Should convert to 2-char grid (field)")
    void shouldConvertToMaidenhead2Char() {
        BigDecimal lat = BigDecimal.valueOf(40.0);
        BigDecimal lon = BigDecimal.valueOf(-74.0);
        String grid = converter.toMaidenhead(lat, lon, 2);
        assertEquals(2, grid.length());
        assertEquals("FN", grid);
    }

    @Test
    @DisplayName("Should convert to 8-char grid")
    void shouldConvertToMaidenhead8Char() {
        BigDecimal lat = BigDecimal.valueOf(40.7);
        BigDecimal lon = BigDecimal.valueOf(-74.0);
        String grid = converter.toMaidenhead(lat, lon, 8);
        assertEquals(8, grid.length());
        assertTrue(grid.startsWith("FN30"));
    }

    @Test
    @DisplayName("Should use default 6-char precision for 2-arg method")
    void shouldDefaultTo6CharPrecision() {
        BigDecimal lat = BigDecimal.valueOf(40.7);
        BigDecimal lon = BigDecimal.valueOf(-74.0);
        String grid = converter.toMaidenhead(lat, lon);
        assertEquals(6, grid.length());
    }

    @Test
    @DisplayName("Should handle prime meridian / equator (AA00aa)")
    void shouldHandlePrimeMeridianEquator() {
        BigDecimal lat = BigDecimal.valueOf(0.0);
        BigDecimal lon = BigDecimal.valueOf(0.0);
        String grid = converter.toMaidenhead(lat, lon, 4);
        assertEquals("JJ00", grid);
    }

    @Test
    @DisplayName("Should handle negative latitude (Southern hemisphere)")
    void shouldHandleSouthernHemisphere() {
        // Sydney, Australia: ~-33.9°S, 151.2°E
        BigDecimal lat = BigDecimal.valueOf(-33.9);
        BigDecimal lon = BigDecimal.valueOf(151.2);
        String grid = converter.toMaidenhead(lat, lon, 4);
        assertNotNull(grid);
        assertEquals(4, grid.length());
    }

    @Test
    @DisplayName("Should handle International Date Line (western)")
    void shouldHandleInternationalDateLineWest() {
        BigDecimal lat = BigDecimal.valueOf(20.0);
        BigDecimal lon = BigDecimal.valueOf(-179.9);
        String grid = converter.toMaidenhead(lat, lon, 4);
        assertNotNull(grid);
        assertEquals(4, grid.length());
    }

    @Test
    @DisplayName("Should handle International Date Line (eastern)")
    void shouldHandleInternationalDateLineEast() {
        BigDecimal lat = BigDecimal.valueOf(20.0);
        BigDecimal lon = BigDecimal.valueOf(179.9);
        String grid = converter.toMaidenhead(lat, lon, 4);
        assertNotNull(grid);
        assertEquals(4, grid.length());
    }

    @Test
    @DisplayName("Should throw exception for invalid precision")
    void shouldThrowForInvalidPrecision() {
        BigDecimal lat = BigDecimal.valueOf(40.0);
        BigDecimal lon = BigDecimal.valueOf(-74.0);
        assertThrows(IllegalArgumentException.class,
            () -> converter.toMaidenhead(lat, lon, 3));
        assertThrows(IllegalArgumentException.class,
            () -> converter.toMaidenhead(lat, lon, 1));
        assertThrows(IllegalArgumentException.class,
            () -> converter.toMaidenhead(lat, lon, 10));
    }

    // ===== fromMaidenhead tests =====

    @Test
    @DisplayName("Should convert 6-char grid to center coordinates")
    void shouldConvertFromMaidenhead6Char() {
        MaidenheadConverter.GridLocation loc = converter.fromMaidenhead("FN31pr");
        assertNotNull(loc);
        assertNotNull(loc.getLat());
        assertNotNull(loc.getLon());
        assertEquals(6, loc.getPrecision());
        // FN31pr should be roughly around 41.7°N, 72.7°W
        assertTrue(loc.getLat().doubleValue() > 41.0 && loc.getLat().doubleValue() < 42.0,
            "Lat out of range: " + loc.getLat());
        assertTrue(loc.getLon().doubleValue() > -74.0 && loc.getLon().doubleValue() < -71.0,
            "Lon out of range: " + loc.getLon());
    }

    @Test
    @DisplayName("Should convert 4-char grid to center coordinates")
    void shouldConvertFromMaidenhead4Char() {
        MaidenheadConverter.GridLocation loc = converter.fromMaidenhead("FN30");
        assertNotNull(loc);
        assertEquals(4, loc.getPrecision());
    }

    @Test
    @DisplayName("Should convert 2-char grid to center coordinates")
    void shouldConvertFromMaidenhead2Char() {
        MaidenheadConverter.GridLocation loc = converter.fromMaidenhead("FN");
        assertNotNull(loc);
        assertEquals(2, loc.getPrecision());
    }

    @Test
    @DisplayName("Should convert 8-char grid to center coordinates")
    void shouldConvertFromMaidenhead8Char() {
        MaidenheadConverter.GridLocation loc = converter.fromMaidenhead("FN31pr12");
        assertNotNull(loc);
        assertEquals(8, loc.getPrecision());
    }

    @Test
    @DisplayName("Should handle lowercase grid input")
    void shouldHandleLowercaseGrid() {
        MaidenheadConverter.GridLocation loc = converter.fromMaidenhead("fn31pr");
        assertNotNull(loc);
    }

    @Test
    @DisplayName("Should throw exception for null grid")
    void shouldThrowForNullGrid() {
        assertThrows(IllegalArgumentException.class,
            () -> converter.fromMaidenhead(null));
    }

    @Test
    @DisplayName("Should throw exception for too-short grid")
    void shouldThrowForShortGrid() {
        assertThrows(IllegalArgumentException.class,
            () -> converter.fromMaidenhead("F"));
    }

    @Test
    @DisplayName("Should throw exception for odd-length grid")
    void shouldThrowForOddLengthGrid() {
        assertThrows(IllegalArgumentException.class,
            () -> converter.fromMaidenhead("FN3"));
    }

    // ===== round-trip tests =====

    @Test
    @DisplayName("Round-trip: toMaidenhead then fromMaidenhead should be close")
    void shouldRoundTrip4Char() {
        BigDecimal lat = BigDecimal.valueOf(40.7);
        BigDecimal lon = BigDecimal.valueOf(-74.0);
        String grid = converter.toMaidenhead(lat, lon, 4);
        MaidenheadConverter.GridLocation loc = converter.fromMaidenhead(grid);

        // Center should be within the 4-char grid (within 1 degree)
        double latDiff = Math.abs(loc.getLat().doubleValue() - lat.doubleValue());
        double lonDiff = Math.abs(loc.getLon().doubleValue() - lon.doubleValue());
        assertTrue(latDiff < 1.5, "Lat difference too large: " + latDiff);
        assertTrue(lonDiff < 2.5, "Lon difference too large: " + lonDiff);
    }

    // ===== getBounds tests =====

    @Test
    @DisplayName("Should return valid bounding box for 4-char grid")
    void shouldGetBoundsFor4CharGrid() {
        MaidenheadConverter.GridBounds bounds = converter.getBounds("FN30");
        assertNotNull(bounds);
        assertTrue(bounds.getMinLat().compareTo(bounds.getMaxLat()) < 0,
            "minLat should be less than maxLat");
        assertTrue(bounds.getMinLon().compareTo(bounds.getMaxLon()) < 0,
            "minLon should be less than maxLon");
        // 4-char grid: 2° lon × 1° lat
        double latRange = bounds.getMaxLat().doubleValue() - bounds.getMinLat().doubleValue();
        double lonRange = bounds.getMaxLon().doubleValue() - bounds.getMinLon().doubleValue();
        assertEquals(1.0, latRange, 0.01, "4-char lat range should be 1 degree");
        assertEquals(2.0, lonRange, 0.01, "4-char lon range should be 2 degrees");
    }

    @Test
    @DisplayName("Should return valid bounding box for 6-char grid")
    void shouldGetBoundsFor6CharGrid() {
        MaidenheadConverter.GridBounds bounds = converter.getBounds("FN31pr");
        assertNotNull(bounds);
        assertTrue(bounds.getMinLat().compareTo(bounds.getMaxLat()) < 0);
        assertTrue(bounds.getMinLon().compareTo(bounds.getMaxLon()) < 0);
    }

    @Test
    @DisplayName("Should return valid bounding box for 2-char grid")
    void shouldGetBoundsFor2CharGrid() {
        MaidenheadConverter.GridBounds bounds = converter.getBounds("FN");
        assertNotNull(bounds);
        // 2-char grid: 20° lon × 10° lat
        double latRange = bounds.getMaxLat().doubleValue() - bounds.getMinLat().doubleValue();
        double lonRange = bounds.getMaxLon().doubleValue() - bounds.getMinLon().doubleValue();
        assertEquals(10.0, latRange, 0.01, "2-char lat range should be 10 degrees");
        assertEquals(20.0, lonRange, 0.01, "2-char lon range should be 20 degrees");
    }

    // ===== detectPrecision tests =====

    @Test
    @DisplayName("Should return precision 6 for fewer than 100 QSOs")
    void shouldReturn6For99QSOs() {
        assertEquals(6, converter.detectPrecision(0));
        assertEquals(6, converter.detectPrecision(50));
        assertEquals(6, converter.detectPrecision(99));
    }

    @Test
    @DisplayName("Should return precision 4 for 100 to 999 QSOs")
    void shouldReturn4For100to999QSOs() {
        assertEquals(4, converter.detectPrecision(100));
        assertEquals(4, converter.detectPrecision(500));
        assertEquals(4, converter.detectPrecision(999));
    }

    @Test
    @DisplayName("Should return precision 2 for 1000 or more QSOs")
    void shouldReturn2For1000PlusQSOs() {
        assertEquals(2, converter.detectPrecision(1000));
        assertEquals(2, converter.detectPrecision(5000));
        assertEquals(2, converter.detectPrecision(100000));
    }
}
