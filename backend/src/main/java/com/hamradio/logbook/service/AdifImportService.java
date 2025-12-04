package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for importing QSOs from ADIF format
 * ADIF (Amateur Data Interchange Format) is the standard for ham radio logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdifImportService {

    private final QSORepository qsoRepository;
    private final StationRepository stationRepository;
    private final OperatorRepository operatorRepository;
    private final ContestRepository contestRepository;
    private final LogRepository logRepository;
    private final ScoringJobService scoringJobService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ADIF field pattern: <FIELD_NAME:LENGTH[:TYPE]>VALUE
    private static final Pattern FIELD_PATTERN = Pattern.compile("<([^:>]+):([0-9]+)(?::([^>]+))?>([^<]*)");
    private static final Pattern EOR_PATTERN = Pattern.compile("<EOR>", Pattern.CASE_INSENSITIVE);
    private static final Pattern EOH_PATTERN = Pattern.compile("<EOH>", Pattern.CASE_INSENSITIVE);

    // Date/time formatters for ADIF
    private static final DateTimeFormatter DATE_FORMAT_8 = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT_6 = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter TIME_FORMAT_4 = DateTimeFormatter.ofPattern("HHmm");

    /**
     * Import result containing imported QSOs and any errors
     */
    public static class ImportResult {
        public List<QSO> importedQSOs = new ArrayList<>();
        public List<String> errors = new ArrayList<>();
        public int totalRecords = 0;
        public int successCount = 0;
        public int errorCount = 0;
    }

    /**
     * Preview result containing unique station callsigns found in ADIF file
     */
    public static class PreviewResult {
        public Set<String> stationCallsigns = new HashSet<>();
        public int totalRecords = 0;
    }

    /**
     * Preview ADIF file and extract unique station callsigns
     */
    public PreviewResult previewAdif(byte[] fileContent) {
        PreviewResult result = new PreviewResult();

        try {
            String content = new String(fileContent, StandardCharsets.UTF_8);
            List<Map<String, String>> records = parseAdifContent(content);

            result.totalRecords = records.size();

            for (Map<String, String> record : records) {
                String stationCallsign = record.get("STATION_CALLSIGN");
                if (stationCallsign != null && !stationCallsign.isEmpty()) {
                    result.stationCallsigns.add(stationCallsign.toUpperCase());
                }
            }

            log.info("Previewed ADIF file: {} records, {} unique station callsigns",
                    result.totalRecords, result.stationCallsigns.size());

        } catch (Exception e) {
            log.error("Error previewing ADIF file", e);
        }

        return result;
    }

    /**
     * Import QSOs from ADIF file content into a specific log
     */
    @Transactional
    public ImportResult importAdif(byte[] fileContent, Long logId, Long defaultStationId) {
        ImportResult result = new ImportResult();

        // Validate log exists
        Log targetLog = logRepository.findById(logId)
            .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        // Validate default station exists
        Station defaultStation = stationRepository.findById(defaultStationId)
            .orElseThrow(() -> new IllegalArgumentException("Station not found: " + defaultStationId));

        try {
            String content = new String(fileContent, StandardCharsets.UTF_8);
            List<Map<String, String>> records = parseAdifContent(content);

            result.totalRecords = records.size();
            log.info("Parsed {} ADIF records", records.size());

            for (int i = 0; i < records.size(); i++) {
                Map<String, String> record = records.get(i);
                try {
                    QSO qso = createQSOFromAdifRecord(record, targetLog, defaultStation);
                    QSO savedQso = qsoRepository.save(qso);
                    result.importedQSOs.add(savedQso);
                    result.successCount++;
                } catch (Exception e) {
                    String error = String.format("Record %d: %s", i + 1, e.getMessage());
                    result.errors.add(error);
                    result.errorCount++;
                    log.warn("Error importing ADIF record {}: {}", i + 1, e.getMessage());
                }
            }

            log.info("ADIF import completed: {} success, {} errors", result.successCount, result.errorCount);

            // Trigger background scoring recalculation if QSOs were imported
            if (result.successCount > 0) {
                log.info("Starting background score recalculation for log {} after importing {} QSOs",
                        logId, result.successCount);
                scoringJobService.recalculateLogScoresAsync(logId);
            }

        } catch (Exception e) {
            result.errors.add("Failed to parse ADIF file: " + e.getMessage());
            result.errorCount = result.totalRecords;
            log.error("Failed to parse ADIF file", e);
        }

        return result;
    }

    /**
     * Import QSOs from ADIF file with station mapping
     * @param stationMapping Map of STATION_CALLSIGN (from ADIF) to local Station ID
     */
    @Transactional
    public ImportResult importAdifWithMapping(byte[] fileContent, Long logId, Map<String, Long> stationMapping, Long fallbackStationId) {
        ImportResult result = new ImportResult();

        // Validate log exists
        Log targetLog = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        // Validate fallback station exists
        Station fallbackStation = stationRepository.findById(fallbackStationId)
                .orElseThrow(() -> new IllegalArgumentException("Fallback station not found: " + fallbackStationId));

        // Validate all mapped stations exist
        Map<String, Station> stationMap = new HashMap<>();
        for (Map.Entry<String, Long> entry : stationMapping.entrySet()) {
            Station station = stationRepository.findById(entry.getValue())
                    .orElseThrow(() -> new IllegalArgumentException("Station not found: " + entry.getValue()));
            stationMap.put(entry.getKey().toUpperCase(), station);
        }

        try {
            String content = new String(fileContent, StandardCharsets.UTF_8);
            List<Map<String, String>> records = parseAdifContent(content);

            result.totalRecords = records.size();
            log.info("Parsed {} ADIF records with station mapping", records.size());

            for (int i = 0; i < records.size(); i++) {
                Map<String, String> record = records.get(i);
                try {
                    // Determine which station to use for this QSO
                    String stationCallsign = record.get("STATION_CALLSIGN");
                    Station targetStation;

                    if (stationCallsign != null && !stationCallsign.isEmpty()) {
                        String upperCallsign = stationCallsign.toUpperCase();
                        targetStation = stationMap.getOrDefault(upperCallsign, fallbackStation);
                    } else {
                        targetStation = fallbackStation;
                    }

                    QSO qso = createQSOFromAdifRecord(record, targetLog, targetStation);
                    QSO savedQso = qsoRepository.save(qso);
                    result.importedQSOs.add(savedQso);
                    result.successCount++;
                } catch (Exception e) {
                    String error = String.format("Record %d: %s", i + 1, e.getMessage());
                    result.errors.add(error);
                    result.errorCount++;
                    log.warn("Error importing ADIF record {}: {}", i + 1, e.getMessage());
                }
            }

            log.info("ADIF import with mapping completed: {} success, {} errors", result.successCount, result.errorCount);

            // Trigger background scoring recalculation if QSOs were imported
            if (result.successCount > 0) {
                log.info("Starting background score recalculation for log {} after importing {} QSOs",
                        logId, result.successCount);
                scoringJobService.recalculateLogScoresAsync(logId);
            }

        } catch (Exception e) {
            result.errors.add("Failed to parse ADIF file: " + e.getMessage());
            result.errorCount = result.totalRecords;
            log.error("Failed to parse ADIF file", e);
        }

        return result;
    }

    /**
     * Parse ADIF content into list of field maps
     */
    private List<Map<String, String>> parseAdifContent(String content) {
        List<Map<String, String>> records = new ArrayList<>();

        // Find EOH (end of header) marker
        Matcher eohMatcher = EOH_PATTERN.matcher(content);
        int dataStart = 0;
        if (eohMatcher.find()) {
            dataStart = eohMatcher.end();
        }

        // Split content into records by <EOR> marker
        String dataContent = content.substring(dataStart);
        String[] recordStrings = EOR_PATTERN.split(dataContent);

        for (String recordString : recordStrings) {
            if (recordString.trim().isEmpty()) {
                continue;
            }

            Map<String, String> record = parseAdifRecord(recordString);
            if (!record.isEmpty() && record.containsKey("CALL")) {
                records.add(record);
            }
        }

        return records;
    }

    /**
     * Parse a single ADIF record into field map
     */
    private Map<String, String> parseAdifRecord(String recordString) {
        Map<String, String> fields = new HashMap<>();

        Matcher matcher = FIELD_PATTERN.matcher(recordString);
        while (matcher.find()) {
            String fieldName = matcher.group(1).toUpperCase();
            int length = Integer.parseInt(matcher.group(2));
            String value = matcher.group(4);

            // Extract exactly 'length' characters from value
            if (value.length() >= length) {
                value = value.substring(0, length);
            }

            fields.put(fieldName, value.trim());
        }

        return fields;
    }

    /**
     * Create QSO entity from ADIF record
     */
    private QSO createQSOFromAdifRecord(Map<String, String> record, Log targetLog, Station defaultStation) {
        QSO qso = new QSO();

        // Required fields
        String callsign = record.get("CALL");
        if (callsign == null || callsign.isEmpty()) {
            throw new IllegalArgumentException("Missing required field: CALL");
        }
        qso.setCallsign(callsign.toUpperCase());

        // Date and time - convert to UTC
        LocalDate qsoDate = parseDate(record.get("QSO_DATE"));
        if (qsoDate == null) {
            throw new IllegalArgumentException("Missing or invalid QSO_DATE");
        }
        qso.setQsoDate(qsoDate);

        LocalTime timeOn = parseTime(record.get("TIME_ON"));
        if (timeOn == null) {
            throw new IllegalArgumentException("Missing or invalid TIME_ON");
        }
        qso.setTimeOn(timeOn);

        // Optional time off
        LocalTime timeOff = parseTime(record.get("TIME_OFF"));
        qso.setTimeOff(timeOff);

        // Frequency and band
        String freqStr = record.get("FREQ");
        if (freqStr != null && !freqStr.isEmpty()) {
            try {
                double freqMhz = Double.parseDouble(freqStr);
                qso.setFrequencyKhz((long) (freqMhz * 1000));
            } catch (NumberFormatException e) {
                log.warn("Invalid frequency value: {}", freqStr);
            }
        }

        String band = record.get("BAND");
        if (band != null && !band.isEmpty()) {
            qso.setBand(band.toLowerCase());
        }

        // Mode
        String mode = record.get("MODE");
        if (mode != null && !mode.isEmpty()) {
            qso.setMode(mode.toUpperCase());
        } else {
            // Default to SSB if not specified
            qso.setMode("SSB");
        }

        // RST
        qso.setRstSent(record.get("RST_SENT"));
        qso.setRstRcvd(record.get("RST_RCVD"));

        // Power
        String powerStr = record.get("TX_PWR");
        if (powerStr != null && !powerStr.isEmpty()) {
            try {
                qso.setPowerWatts(Integer.parseInt(powerStr.split("\\.")[0])); // Handle decimal values
            } catch (NumberFormatException e) {
                log.warn("Invalid power value: {}", powerStr);
            }
        }

        // Location data
        qso.setGridSquare(record.get("GRIDSQUARE"));
        qso.setState(record.get("STATE"));
        qso.setCounty(record.get("CNTY"));
        qso.setCountry(record.get("COUNTRY"));
        qso.setName(record.get("NAME"));

        // Zones
        String dxccStr = record.get("DXCC");
        if (dxccStr != null && !dxccStr.isEmpty()) {
            try {
                qso.setDxcc(Integer.parseInt(dxccStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid DXCC value: {}", dxccStr);
            }
        }

        String cqZoneStr = record.get("CQZ");
        if (cqZoneStr != null && !cqZoneStr.isEmpty()) {
            try {
                qso.setCqZone(Integer.parseInt(cqZoneStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid CQ Zone value: {}", cqZoneStr);
            }
        }

        String ituZoneStr = record.get("ITUZ");
        if (ituZoneStr != null && !ituZoneStr.isEmpty()) {
            try {
                qso.setItuZone(Integer.parseInt(ituZoneStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid ITU Zone value: {}", ituZoneStr);
            }
        }

        // QSL info
        qso.setQslSent(record.get("QSL_SENT"));
        qso.setQslRcvd(record.get("QSL_RCVD"));
        qso.setLotwSent(record.get("LOTW_QSLSDATE"));
        qso.setLotwRcvd(record.get("LOTW_QSLRDATE"));

        // Notes/comments
        qso.setNotes(record.get("COMMENT"));

        // Station - try to find by callsign, otherwise use default
        String stationCallsign = record.get("STATION_CALLSIGN");
        if (stationCallsign != null && !stationCallsign.isEmpty()) {
            List<Station> stations = stationRepository.findByCallsign(stationCallsign);
            qso.setStation(stations.isEmpty() ? defaultStation : stations.get(0));
        } else {
            qso.setStation(defaultStation);
        }

        // Operator - try to find by callsign
        String operatorCallsign = record.get("OPERATOR");
        if (operatorCallsign != null && !operatorCallsign.isEmpty()) {
            Optional<Operator> operator = operatorRepository.findByCallsign(operatorCallsign);
            operator.ifPresent(qso::setOperator);
        }

        // Contest data
        String contestId = record.get("CONTEST_ID");
        if (contestId != null && !contestId.isEmpty()) {
            Optional<Contest> contest = contestRepository.findByContestCode(contestId);
            contest.ifPresent(qso::setContest);

            // Build contest data JSON
            ObjectNode contestData = objectMapper.createObjectNode();
            if (record.containsKey("CLASS")) {
                contestData.put("class", record.get("CLASS"));
            }
            if (record.containsKey("ARRL_SECT")) {
                contestData.put("section", record.get("ARRL_SECT"));
            }
            if (record.containsKey("SIG_INFO")) {
                contestData.put("park_ref", record.get("SIG_INFO"));
            }
            if (record.containsKey("SOTA_REF")) {
                contestData.put("summit_ref", record.get("SOTA_REF"));
            }

            if (!contestData.isEmpty()) {
                try {
                    qso.setContestData(objectMapper.writeValueAsString(contestData));
                } catch (Exception e) {
                    log.warn("Failed to serialize contest data", e);
                }
            }
        }

        // Set log
        qso.setLog(targetLog);
        qso.setIsValid(true);

        return qso;
    }

    /**
     * Parse ADIF date (YYYYMMDD format) to LocalDate in UTC
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMAT_8);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format: {}", dateStr);
            return null;
        }
    }

    /**
     * Parse ADIF time (HHMMSS or HHMM format) to LocalTime in UTC
     */
    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }

        try {
            if (timeStr.length() == 6) {
                return LocalTime.parse(timeStr, TIME_FORMAT_6);
            } else if (timeStr.length() == 4) {
                return LocalTime.parse(timeStr, TIME_FORMAT_4);
            } else {
                log.warn("Invalid time format (expected HHMMSS or HHMM): {}", timeStr);
                return null;
            }
        } catch (DateTimeParseException e) {
            log.warn("Invalid time format: {}", timeStr);
            return null;
        }
    }
}
