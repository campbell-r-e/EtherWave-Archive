package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for exporting QSOs in ADIF format
 * ADIF (Amateur Data Interchange Format) is the standard for ham radio logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdifExportService {

    private final QSORepository qsoRepository;
    private final StationRepository stationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");

    /**
     * Export all QSOs to ADIF format
     */
    @Transactional(readOnly = true)
    public byte[] exportAllQSOs() {
        List<QSO> qsos = qsoRepository.findAll();
        return exportQSOs(qsos);
    }

    /**
     * Export QSOs for a specific log to ADIF format
     */
    @Transactional(readOnly = true)
    public byte[] exportQSOsByLog(Long logId) {
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);
        return exportQSOs(qsos);
    }

    /**
     * Export QSOs for a date range to ADIF format
     */
    @Transactional(readOnly = true)
    public byte[] exportQSOsByDateRange(LocalDate startDate, LocalDate endDate) {
        List<QSO> qsos = qsoRepository.findByDateRange(startDate, endDate);
        return exportQSOs(qsos);
    }

    /**
     * Export QSOs for a specific log and date range to ADIF format
     */
    @Transactional(readOnly = true)
    public byte[] exportQSOsByLogAndDateRange(Long logId, LocalDate startDate, LocalDate endDate) {
        List<QSO> qsos = qsoRepository.findByLogIdAndDateRange(logId, startDate, endDate);
        return exportQSOs(qsos);
    }

    /**
     * Export GOTA QSOs only for a specific log
     */
    @Transactional(readOnly = true)
    public byte[] exportGotaQSOs(Long logId) {
        // Find all GOTA stations
        List<Station> gotaStations = stationRepository.findAll().stream()
                .filter(s -> s.getIsGota() != null && s.getIsGota())
                .toList();

        if (gotaStations.isEmpty()) {
            log.warn("No GOTA stations found for log {}", logId);
            return exportQSOs(List.of()); // Return empty ADIF file
        }

        // Get QSOs from all GOTA stations
        List<QSO> gotaQsos = new ArrayList<>();
        for (Station gotaStation : gotaStations) {
            List<QSO> stationQsos = qsoRepository.findByLogIdAndStationId(logId, gotaStation.getId());
            gotaQsos.addAll(stationQsos);
        }

        log.info("Exporting {} GOTA QSOs from log {}", gotaQsos.size(), logId);
        return exportQSOs(gotaQsos);
    }

    /**
     * Export non-GOTA QSOs only for a specific log
     */
    @Transactional(readOnly = true)
    public byte[] exportNonGotaQSOs(Long logId) {
        // Find all GOTA stations
        List<Station> gotaStations = stationRepository.findAll().stream()
                .filter(s -> s.getIsGota() != null && s.getIsGota())
                .toList();

        // Get all QSOs from log
        List<QSO> allQsos = qsoRepository.findAllByLogId(logId);

        // Filter out GOTA QSOs
        List<QSO> nonGotaQsos;
        if (gotaStations.isEmpty()) {
            nonGotaQsos = allQsos;
        } else {
            Set<Long> gotaStationIds = gotaStations.stream()
                    .map(Station::getId)
                    .collect(Collectors.toSet());

            nonGotaQsos = allQsos.stream()
                    .filter(qso -> !gotaStationIds.contains(qso.getStation().getId()))
                    .toList();
        }

        log.info("Exporting {} non-GOTA QSOs from log {}", nonGotaQsos.size(), logId);
        return exportQSOs(nonGotaQsos);
    }

    /**
     * Export a list of QSOs to ADIF format
     */
    public byte[] exportQSOs(List<QSO> qsos) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8);

        // Write ADIF header
        writeHeader(writer);

        // Write each QSO
        for (QSO qso : qsos) {
            writeQSO(writer, qso);
        }

        // Write end of file marker
        writer.println("<EOR>");
        writer.flush();

        log.info("Exported {} QSOs to ADIF format", qsos.size());
        return baos.toByteArray();
    }

    /**
     * Write ADIF header
     */
    private void writeHeader(PrintWriter writer) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        writer.println("ADIF Export from Ham Radio Logbook System");
        writer.println("Generated: " + LocalDate.now().format(DATE_FORMAT));
        writer.println("<ADIF_VER:5>3.1.4");
        writer.println("<CREATED_TIMESTAMP:" + timestamp.length() + ">" + timestamp);
        writer.println("<PROGRAMID:20>HamRadioLogbook");
        writer.println("<PROGRAMVERSION:5>1.0.0");
        writer.println("<EOH>");
        writer.println();
    }

    /**
     * Write a single QSO in ADIF format
     */
    private void writeQSO(PrintWriter writer, QSO qso) {
        // Callsign (required)
        writeField(writer, "CALL", qso.getCallsign());

        // Frequency in MHz
        if (qso.getFrequencyKhz() != null) {
            double freqMhz = qso.getFrequencyKhz() / 1000.0;
            writeField(writer, "FREQ", String.format("%.6f", freqMhz));
        }

        // Band
        if (qso.getBand() != null) {
            writeField(writer, "BAND", qso.getBand());
        }

        // Mode
        if (qso.getMode() != null) {
            writeField(writer, "MODE", qso.getMode());
        }

        // QSO Date and Time
        if (qso.getQsoDate() != null) {
            writeField(writer, "QSO_DATE", qso.getQsoDate().format(DATE_FORMAT));
        }

        if (qso.getTimeOn() != null) {
            writeField(writer, "TIME_ON", qso.getTimeOn().format(TIME_FORMAT));
        }

        if (qso.getTimeOff() != null) {
            writeField(writer, "TIME_OFF", qso.getTimeOff().format(TIME_FORMAT));
        }

        // RST
        if (qso.getRstSent() != null) {
            writeField(writer, "RST_SENT", qso.getRstSent());
        }

        if (qso.getRstRcvd() != null) {
            writeField(writer, "RST_RCVD", qso.getRstRcvd());
        }

        // Station info
        if (qso.getStation() != null) {
            writeField(writer, "STATION_CALLSIGN", qso.getStation().getCallsign());
            if (qso.getStation().getGridSquare() != null) {
                writeField(writer, "MY_GRIDSQUARE", qso.getStation().getGridSquare());
            }
        }

        // Operator
        if (qso.getOperator() != null) {
            writeField(writer, "OPERATOR", qso.getOperator().getCallsign());
        }

        // Location data
        if (qso.getName() != null) {
            writeField(writer, "NAME", qso.getName());
        }

        if (qso.getGridSquare() != null) {
            writeField(writer, "GRIDSQUARE", qso.getGridSquare());
        }

        if (qso.getState() != null) {
            writeField(writer, "STATE", qso.getState());
        }

        if (qso.getCounty() != null) {
            writeField(writer, "CNTY", qso.getCounty());
        }

        if (qso.getCountry() != null) {
            writeField(writer, "COUNTRY", qso.getCountry());
        }

        if (qso.getDxcc() != null) {
            writeField(writer, "DXCC", qso.getDxcc().toString());
        }

        if (qso.getCqZone() != null) {
            writeField(writer, "CQZ", qso.getCqZone().toString());
        }

        if (qso.getItuZone() != null) {
            writeField(writer, "ITUZ", qso.getItuZone().toString());
        }

        // Power
        if (qso.getPowerWatts() != null) {
            writeField(writer, "TX_PWR", qso.getPowerWatts().toString());
        }

        // QSL info
        if (qso.getQslSent() != null) {
            writeField(writer, "QSL_SENT", qso.getQslSent());
        }

        if (qso.getQslRcvd() != null) {
            writeField(writer, "QSL_RCVD", qso.getQslRcvd());
        }

        if (qso.getLotwSent() != null) {
            writeField(writer, "LOTW_QSLSDATE", qso.getLotwSent());
        }

        if (qso.getLotwRcvd() != null) {
            writeField(writer, "LOTW_QSLRDATE", qso.getLotwRcvd());
        }

        // Comments
        if (qso.getNotes() != null) {
            writeField(writer, "COMMENT", qso.getNotes());
        }

        // Contest-specific data
        if (qso.getContest() != null && qso.getContestData() != null) {
            writeContestFields(writer, qso);
        }

        // End of record
        writer.println("<EOR>");
        writer.println();
    }

    /**
     * Write contest-specific ADIF fields
     */
    private void writeContestFields(PrintWriter writer, QSO qso) {
        try {
            JsonNode contestData = objectMapper.readTree(qso.getContestData());

            // Contest ID
            writeField(writer, "CONTEST_ID", qso.getContest().getContestCode());

            // Field Day / Winter Field Day
            if (contestData.has("class")) {
                writeField(writer, "CLASS", contestData.get("class").asText());
            }

            if (contestData.has("section")) {
                writeField(writer, "ARRL_SECT", contestData.get("section").asText());
            }

            // POTA
            if (contestData.has("park_ref")) {
                writeField(writer, "SIG", "POTA");
                writeField(writer, "SIG_INFO", contestData.get("park_ref").asText());
                writeField(writer, "MY_SIG", "POTA");
                writeField(writer, "MY_SIG_INFO", contestData.get("park_ref").asText());
            }

            if (contestData.has("hunter_ref")) {
                writeField(writer, "SIG_INFO", contestData.get("hunter_ref").asText());
            }

            // SOTA
            if (contestData.has("summit_ref")) {
                writeField(writer, "SOTA_REF", contestData.get("summit_ref").asText());
            }

        } catch (Exception e) {
            log.warn("Error parsing contest data for QSO {}: {}", qso.getId(), e.getMessage());
        }
    }

    /**
     * Write a single ADIF field
     */
    private void writeField(PrintWriter writer, String fieldName, String value) {
        if (value != null && !value.isEmpty()) {
            writer.print("<" + fieldName + ":" + value.length() + ">" + value + " ");
        }
    }
}
