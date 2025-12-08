package com.hamradio.logbook.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Maidenhead Grid Square Locator System converter
 * Converts between latitude/longitude coordinates and Maidenhead grid squares
 *
 * Format: AA00aa00 (2, 4, 6, or 8 characters)
 * - Field (AA): 20° longitude × 10° latitude
 * - Square (00): 2° longitude × 1° latitude
 * - Subsquare (aa): 5' longitude × 2.5' latitude
 * - Extended (00): 30" longitude × 15" latitude
 */
@Service
public class MaidenheadConverter {

    private static final String FIELD_CHARS = "ABCDEFGHIJKLMNOPQR";
    private static final String SUBSQUARE_CHARS = "abcdefghijklmnopqrstuvwx";

    /**
     * Convert latitude/longitude to Maidenhead grid square
     *
     * @param lat Latitude (-90 to 90)
     * @param lon Longitude (-180 to 180)
     * @param precision Number of characters (2, 4, 6, or 8)
     * @return Maidenhead grid square (e.g., "FN31pr")
     */
    public String toMaidenhead(BigDecimal lat, BigDecimal lon, int precision) {
        if (precision < 2 || precision > 8 || precision % 2 != 0) {
            throw new IllegalArgumentException("Precision must be 2, 4, 6, or 8");
        }

        // Normalize coordinates
        double latitude = lat.doubleValue() + 90.0;  // 0-180
        double longitude = lon.doubleValue() + 180.0; // 0-360

        StringBuilder grid = new StringBuilder();

        // Field (first 2 characters)
        int lonField = (int) (longitude / 20.0);
        int latField = (int) (latitude / 10.0);
        grid.append(FIELD_CHARS.charAt(lonField));
        grid.append(FIELD_CHARS.charAt(latField));

        if (precision >= 4) {
            // Square (next 2 characters)
            int lonSquare = (int) ((longitude % 20.0) / 2.0);
            int latSquare = (int) ((latitude % 10.0) / 1.0);
            grid.append(lonSquare);
            grid.append(latSquare);
        }

        if (precision >= 6) {
            // Subsquare (next 2 characters)
            double lonSubsquare = ((longitude % 2.0) / (5.0 / 60.0));
            double latSubsquare = ((latitude % 1.0) / (2.5 / 60.0));
            grid.append(SUBSQUARE_CHARS.charAt((int) lonSubsquare));
            grid.append(SUBSQUARE_CHARS.charAt((int) latSubsquare));
        }

        if (precision == 8) {
            // Extended subsquare (last 2 characters)
            double lonExt = ((longitude % (5.0 / 60.0)) / (30.0 / 3600.0));
            double latExt = ((latitude % (2.5 / 60.0)) / (15.0 / 3600.0));
            grid.append((int) lonExt);
            grid.append((int) latExt);
        }

        return grid.toString();
    }

    /**
     * Convert latitude/longitude to Maidenhead grid square with auto-detect precision
     * Default precision: 6 characters
     */
    public String toMaidenhead(BigDecimal lat, BigDecimal lon) {
        return toMaidenhead(lat, lon, 6);
    }

    /**
     * Convert Maidenhead grid square to center coordinates
     *
     * @param grid Maidenhead grid square (e.g., "FN31pr")
     * @return GridLocation with center coordinates
     */
    public GridLocation fromMaidenhead(String grid) {
        if (grid == null || grid.length() < 2) {
            throw new IllegalArgumentException("Invalid Maidenhead grid: " + grid);
        }

        grid = grid.toUpperCase();
        int precision = grid.length();

        if (precision % 2 != 0 || precision > 8) {
            throw new IllegalArgumentException("Grid must have 2, 4, 6, or 8 characters");
        }

        double lon = -180.0;
        double lat = -90.0;

        // Field (first 2 characters)
        lon += (FIELD_CHARS.indexOf(grid.charAt(0))) * 20.0;
        lat += (FIELD_CHARS.indexOf(grid.charAt(1))) * 10.0;

        if (precision >= 4) {
            // Square (next 2 characters)
            lon += Character.getNumericValue(grid.charAt(2)) * 2.0;
            lat += Character.getNumericValue(grid.charAt(3)) * 1.0;
        }

        if (precision >= 6) {
            // Subsquare (next 2 characters)
            String lowerGrid = grid.substring(4, 6).toLowerCase();
            lon += (SUBSQUARE_CHARS.indexOf(lowerGrid.charAt(0))) * (5.0 / 60.0);
            lat += (SUBSQUARE_CHARS.indexOf(lowerGrid.charAt(1))) * (2.5 / 60.0);
        }

        if (precision == 8) {
            // Extended subsquare (last 2 characters)
            lon += Character.getNumericValue(grid.charAt(6)) * (30.0 / 3600.0);
            lat += Character.getNumericValue(grid.charAt(7)) * (15.0 / 3600.0);
        }

        // Calculate center of grid square
        double lonStep = getGridSize(precision).lonStep;
        double latStep = getGridSize(precision).latStep;

        double centerLon = lon + (lonStep / 2.0);
        double centerLat = lat + (latStep / 2.0);

        return new GridLocation(
            BigDecimal.valueOf(centerLat).setScale(6, RoundingMode.HALF_UP),
            BigDecimal.valueOf(centerLon).setScale(6, RoundingMode.HALF_UP),
            precision
        );
    }

    /**
     * Get bounding box for a Maidenhead grid square
     */
    public GridBounds getBounds(String grid) {
        GridLocation center = fromMaidenhead(grid);
        GridSize size = getGridSize(grid.length());

        BigDecimal minLat = center.getLat().subtract(BigDecimal.valueOf(size.latStep / 2.0));
        BigDecimal maxLat = center.getLat().add(BigDecimal.valueOf(size.latStep / 2.0));
        BigDecimal minLon = center.getLon().subtract(BigDecimal.valueOf(size.lonStep / 2.0));
        BigDecimal maxLon = center.getLon().add(BigDecimal.valueOf(size.lonStep / 2.0));

        return new GridBounds(
            minLat.setScale(6, RoundingMode.HALF_UP),
            maxLat.setScale(6, RoundingMode.HALF_UP),
            minLon.setScale(6, RoundingMode.HALF_UP),
            maxLon.setScale(6, RoundingMode.HALF_UP)
        );
    }

    /**
     * Auto-detect optimal grid precision based on QSO count
     * - < 100 QSOs: 6 characters (subsquare)
     * - 100-1000 QSOs: 4 characters (square)
     * - > 1000 QSOs: 2 characters (field)
     */
    public int detectPrecision(int qsoCount) {
        if (qsoCount < 100) {
            return 6;
        } else if (qsoCount < 1000) {
            return 4;
        } else {
            return 2;
        }
    }

    /**
     * Get grid square size for given precision
     */
    private GridSize getGridSize(int precision) {
        return switch (precision) {
            case 2 -> new GridSize(20.0, 10.0);           // Field
            case 4 -> new GridSize(2.0, 1.0);             // Square
            case 6 -> new GridSize(5.0 / 60.0, 2.5 / 60.0); // Subsquare
            case 8 -> new GridSize(30.0 / 3600.0, 15.0 / 3600.0); // Extended
            default -> throw new IllegalArgumentException("Invalid precision: " + precision);
        };
    }

    @Data
    @AllArgsConstructor
    public static class GridLocation {
        private BigDecimal lat;
        private BigDecimal lon;
        private int precision;
    }

    @Data
    @AllArgsConstructor
    public static class GridBounds {
        private BigDecimal minLat;
        private BigDecimal maxLat;
        private BigDecimal minLon;
        private BigDecimal maxLon;
    }

    @Data
    @AllArgsConstructor
    private static class GridSize {
        private double lonStep;
        private double latStep;
    }
}
