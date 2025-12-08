package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.entity.QSOLocation;
import com.hamradio.logbook.repository.QSOLocationRepository;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.service.MapDataService.MapFilters;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for exporting map data in various formats
 * Supports: GeoJSON, KML, CSV, ADIF
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MapExportService {

    private final QSOLocationRepository qsoLocationRepository;
    private final QSORepository qsoRepository;
    private final ObjectMapper objectMapper;

    /**
     * Export map data to GeoJSON format
     * Standard geographic format used by mapping libraries
     */
    public ExportResult exportToGeoJSON(Long logId, MapFilters filters) {
        log.info("Exporting map data to GeoJSON for log {}", logId);

        List<QSOLocation> locations = qsoLocationRepository.findByLogId(logId);

        // Build GeoJSON FeatureCollection
        Map<String, Object> featureCollection = new LinkedHashMap<>();
        featureCollection.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();
        for (QSOLocation location : locations) {
            if (location.getContactLat() == null || location.getContactLon() == null) {
                continue;
            }

            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");

            // Geometry (Point)
            Map<String, Object> geometry = new LinkedHashMap<>();
            geometry.put("type", "Point");
            geometry.put("coordinates", Arrays.asList(
                location.getContactLon().doubleValue(),
                location.getContactLat().doubleValue()
            ));
            feature.put("geometry", geometry);

            // Properties
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("callsign", location.getQso().getCallsign());
            properties.put("grid", location.getContactGrid());
            properties.put("distance_km", location.getDistanceKm());
            properties.put("bearing", location.getBearing());
            properties.put("qso_id", location.getQso().getId());

            // Add operator location if available
            if (location.getOperatorLat() != null && location.getOperatorLon() != null) {
                properties.put("operator_lat", location.getOperatorLat());
                properties.put("operator_lon", location.getOperatorLon());
                properties.put("operator_grid", location.getOperatorGrid());
            }

            feature.put("properties", properties);
            features.add(feature);
        }

        featureCollection.put("features", features);

        // Metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("generated_at", new Date().toString());
        metadata.put("log_id", logId);
        metadata.put("total_features", features.size());
        featureCollection.put("metadata", metadata);

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(featureCollection);

            return ExportResult.builder()
                .format("GeoJSON")
                .content(json)
                .filename("qso_map_" + logId + ".geojson")
                .mimeType("application/geo+json")
                .success(true)
                .build();
        } catch (Exception e) {
            log.error("Failed to export to GeoJSON", e);
            return ExportResult.builder()
                .format("GeoJSON")
                .success(false)
                .error("Failed to generate GeoJSON: " + e.getMessage())
                .build();
        }
    }

    /**
     * Export map data to KML format
     * Google Earth / Google Maps format
     */
    public ExportResult exportToKML(Long logId, MapFilters filters) {
        log.info("Exporting map data to KML for log {}", logId);

        List<QSOLocation> locations = qsoLocationRepository.findByLogId(logId);

        StringBuilder kml = new StringBuilder();
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kml.append("  <Document>\n");
        kml.append("    <name>QSO Map - Log ").append(logId).append("</name>\n");
        kml.append("    <description>Amateur Radio QSO locations exported from EtherWave Archive</description>\n");

        // Define styles
        kml.append("    <Style id=\"qsoMarker\">\n");
        kml.append("      <IconStyle>\n");
        kml.append("        <color>ff0080ff</color>\n");
        kml.append("        <scale>1.0</scale>\n");
        kml.append("        <Icon>\n");
        kml.append("          <href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n");
        kml.append("        </Icon>\n");
        kml.append("      </IconStyle>\n");
        kml.append("    </Style>\n");

        // Add placemarks
        for (QSOLocation location : locations) {
            if (location.getContactLat() == null || location.getContactLon() == null) {
                continue;
            }

            kml.append("    <Placemark>\n");
            kml.append("      <name>").append(escapeXML(location.getQso().getCallsign())).append("</name>\n");
            kml.append("      <description><![CDATA[\n");
            kml.append("        Grid: ").append(location.getContactGrid()).append("<br/>\n");
            kml.append("        Distance: ").append(location.getDistanceKm()).append(" km<br/>\n");
            kml.append("        Bearing: ").append(location.getBearing()).append("°<br/>\n");
            kml.append("      ]]></description>\n");
            kml.append("      <styleUrl>#qsoMarker</styleUrl>\n");
            kml.append("      <Point>\n");
            kml.append("        <coordinates>");
            kml.append(location.getContactLon()).append(",");
            kml.append(location.getContactLat()).append(",0");
            kml.append("</coordinates>\n");
            kml.append("      </Point>\n");
            kml.append("    </Placemark>\n");
        }

        kml.append("  </Document>\n");
        kml.append("</kml>\n");

        return ExportResult.builder()
            .format("KML")
            .content(kml.toString())
            .filename("qso_map_" + logId + ".kml")
            .mimeType("application/vnd.google-earth.kml+xml")
            .success(true)
            .build();
    }

    /**
     * Export map data to CSV format
     * Simple spreadsheet format
     */
    public ExportResult exportToCSV(Long logId, MapFilters filters) {
        log.info("Exporting map data to CSV for log {}", logId);

        List<QSOLocation> locations = qsoLocationRepository.findByLogId(logId);

        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Callsign,Contact Grid,Contact Lat,Contact Lon,");
        csv.append("Operator Grid,Operator Lat,Operator Lon,");
        csv.append("Distance (km),Distance (mi),Bearing,Location Source\n");

        // Data rows
        for (QSOLocation location : locations) {
            csv.append(escapeCsv(location.getQso().getCallsign())).append(",");
            csv.append(escapeCsv(location.getContactGrid())).append(",");
            csv.append(location.getContactLat()).append(",");
            csv.append(location.getContactLon()).append(",");
            csv.append(escapeCsv(location.getOperatorGrid())).append(",");
            csv.append(location.getOperatorLat() != null ? location.getOperatorLat() : "").append(",");
            csv.append(location.getOperatorLon() != null ? location.getOperatorLon() : "").append(",");
            csv.append(location.getDistanceKm()).append(",");
            csv.append(location.getDistanceMi()).append(",");
            csv.append(location.getBearing()).append(",");
            csv.append(location.getLocationSource()).append("\n");
        }

        return ExportResult.builder()
            .format("CSV")
            .content(csv.toString())
            .filename("qso_map_" + logId + ".csv")
            .mimeType("text/csv")
            .success(true)
            .build();
    }

    /**
     * Export map data to ADIF format
     * Amateur Data Interchange Format (standard ham radio log format)
     */
    public ExportResult exportToADIF(Long logId, MapFilters filters) {
        log.info("Exporting map data to ADIF for log {}", logId);

        List<QSO> qsos = qsoRepository.findAllByLogId(logId);
        List<QSOLocation> locations = qsoLocationRepository.findByLogId(logId);

        // Create location lookup map
        Map<Long, QSOLocation> locationMap = locations.stream()
            .collect(Collectors.toMap(
                loc -> loc.getQso().getId(),
                loc -> loc
            ));

        StringBuilder adif = new StringBuilder();

        // ADIF header
        adif.append("ADIF Export from EtherWave Archive\n");
        adif.append("<ADIF_VER:5>3.1.4\n");
        adif.append("<PROGRAMID:17>EtherWave Archive\n");
        adif.append("<PROGRAMVERSION:5>1.0.0\n");
        adif.append("<EOH>\n\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HHmm");

        // Export each QSO
        for (QSO qso : qsos) {
            QSOLocation location = locationMap.get(qso.getId());

            // Callsign
            adif.append("<CALL:").append(qso.getCallsign().length())
                .append(">").append(qso.getCallsign()).append(" ");

            // Frequency
            if (qso.getFrequencyKhz() != null) {
                String freqMhz = String.format("%.4f", qso.getFrequencyKhz() / 1000.0);
                adif.append("<FREQ:").append(freqMhz.length())
                    .append(">").append(freqMhz).append(" ");
            }

            // Band
            if (qso.getBand() != null) {
                adif.append("<BAND:").append(qso.getBand().length())
                    .append(">").append(qso.getBand()).append(" ");
            }

            // Mode
            adif.append("<MODE:").append(qso.getMode().length())
                .append(">").append(qso.getMode()).append(" ");

            // Date and Time
            String dateStr = qso.getQsoDate().format(dateFormatter);
            adif.append("<QSO_DATE:8>").append(dateStr).append(" ");

            String timeStr = qso.getTimeOn().format(timeFormatter);
            adif.append("<TIME_ON:4>").append(timeStr).append(" ");

            // RST
            if (qso.getRstSent() != null) {
                adif.append("<RST_SENT:").append(qso.getRstSent().length())
                    .append(">").append(qso.getRstSent()).append(" ");
            }

            if (qso.getRstRcvd() != null) {
                adif.append("<RST_RCVD:").append(qso.getRstRcvd().length())
                    .append(">").append(qso.getRstRcvd()).append(" ");
            }

            // Grid square
            if (qso.getGridSquare() != null) {
                adif.append("<GRIDSQUARE:").append(qso.getGridSquare().length())
                    .append(">").append(qso.getGridSquare()).append(" ");
            }

            // Location data from QSOLocation
            if (location != null && location.getContactLat() != null) {
                String lat = String.format("%.6f", location.getContactLat());
                adif.append("<LAT:").append(lat.length())
                    .append(">").append(lat).append(" ");

                String lon = String.format("%.6f", location.getContactLon());
                adif.append("<LON:").append(lon.length())
                    .append(">").append(lon).append(" ");

                if (location.getDistanceKm() != null) {
                    String dist = String.format("%.2f", location.getDistanceKm());
                    adif.append("<DISTANCE:").append(dist.length())
                        .append(">").append(dist).append(" ");
                }
            }

            // CQ Zone
            if (qso.getCqZone() != null) {
                String cq = qso.getCqZone().toString();
                adif.append("<CQZ:").append(cq.length())
                    .append(">").append(cq).append(" ");
            }

            // ITU Zone
            if (qso.getItuZone() != null) {
                String itu = qso.getItuZone().toString();
                adif.append("<ITUZ:").append(itu.length())
                    .append(">").append(itu).append(" ");
            }

            // State
            if (qso.getState() != null) {
                adif.append("<STATE:").append(qso.getState().length())
                    .append(">").append(qso.getState()).append(" ");
            }

            // Country
            if (qso.getCountry() != null) {
                adif.append("<COUNTRY:").append(qso.getCountry().length())
                    .append(">").append(qso.getCountry()).append(" ");
            }

            // End of record
            adif.append("<EOR>\n\n");
        }

        return ExportResult.builder()
            .format("ADIF")
            .content(adif.toString())
            .filename("qso_map_" + logId + ".adi")
            .mimeType("text/plain")
            .success(true)
            .build();
    }

    // ===== Helper Methods =====

    /**
     * Escape XML special characters
     */
    private String escapeXML(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class ExportResult {
        private String format;
        private String content;
        private String filename;
        private String mimeType;
        private boolean success;
        private String error;
        private long recordCount;
    }
}
