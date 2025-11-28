package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.QSORepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting contest logs in Cabrillo format
 * Cabrillo is the standard format for contest log submissions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CabrilloExportService {

    private final QSORepository qsoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    /**
     * Export QSOs for a contest in Cabrillo format
     */
    public byte[] exportContestLog(Contest contest, String callsign, String operators, String category) {
        List<QSO> qsos = qsoRepository.findByContest(contest, null).getContent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8);

        // Write Cabrillo header
        writeHeader(writer, contest, callsign, operators, category, qsos.size());

        // Write QSO records
        for (QSO qso : qsos) {
            writeQSO(writer, qso, contest);
        }

        // Write footer
        writer.println("END-OF-LOG:");
        writer.flush();

        log.info("Exported {} QSOs to Cabrillo format for contest {}", qsos.size(), contest.getContestCode());
        return baos.toByteArray();
    }

    /**
     * Write Cabrillo header
     */
    private void writeHeader(PrintWriter writer, Contest contest, String callsign,
                             String operators, String category, int qsoCount) {
        writer.println("START-OF-LOG: 3.0");
        writer.println("CREATED-BY: Ham Radio Logbook System v1.0");
        writer.println("CONTEST: " + contest.getContestName());

        if (callsign != null) {
            writer.println("CALLSIGN: " + callsign);
        }

        if (category != null) {
            writer.println("CATEGORY: " + category);
        }

        if (operators != null) {
            writer.println("OPERATORS: " + operators);
        }

        writer.println("CLAIMED-SCORE: 0"); // Calculate actual score based on contest rules
        writer.println("QSO: " + qsoCount);
        writer.println();
    }

    /**
     * Write a single QSO in Cabrillo format
     */
    private void writeQSO(PrintWriter writer, QSO qso, Contest contest) {
        try {
            // Cabrillo QSO format varies by contest
            // General format: QSO: freq mo date time call rst-s exch-s call-r rst-r exch-r

            StringBuilder line = new StringBuilder("QSO: ");

            // Frequency (in kHz, right-aligned 5 chars)
            if (qso.getFrequencyKhz() != null) {
                line.append(String.format("%5d", qso.getFrequencyKhz()));
            } else {
                line.append("     ");
            }
            line.append(" ");

            // Mode (CW, PH, RY, DG)
            String mode = mapMode(qso.getMode());
            line.append(String.format("%-2s", mode));
            line.append(" ");

            // Date (yyyy-mm-dd)
            if (qso.getQsoDate() != null) {
                line.append(qso.getQsoDate().format(DATE_FORMAT));
            } else {
                line.append("          ");
            }
            line.append(" ");

            // Time (HHMM)
            if (qso.getTimeOn() != null) {
                line.append(qso.getTimeOn().format(TIME_FORMAT));
            } else {
                line.append("    ");
            }
            line.append(" ");

            // Sent callsign
            if (qso.getStation() != null) {
                line.append(String.format("%-13s", qso.getStation().getCallsign()));
            } else {
                line.append("             ");
            }
            line.append(" ");

            // Sent RST
            if (qso.getRstSent() != null) {
                line.append(String.format("%-3s", qso.getRstSent()));
            } else {
                line.append("   ");
            }
            line.append(" ");

            // Sent exchange (contest-specific)
            String sentExchange = extractExchange(qso, contest, true);
            line.append(String.format("%-6s", sentExchange));
            line.append(" ");

            // Received callsign
            line.append(String.format("%-13s", qso.getCallsign()));
            line.append(" ");

            // Received RST
            if (qso.getRstRcvd() != null) {
                line.append(String.format("%-3s", qso.getRstRcvd()));
            } else {
                line.append("   ");
            }
            line.append(" ");

            // Received exchange
            String rcvdExchange = extractExchange(qso, contest, false);
            line.append(String.format("%-6s", rcvdExchange));

            writer.println(line.toString());

        } catch (Exception e) {
            log.warn("Error formatting Cabrillo line for QSO {}: {}", qso.getId(), e.getMessage());
        }
    }

    /**
     * Map mode to Cabrillo format
     */
    private String mapMode(String mode) {
        if (mode == null) return "PH";

        mode = mode.toUpperCase();
        if (mode.equals("CW")) return "CW";
        if (mode.equals("SSB") || mode.equals("FM") || mode.equals("AM")) return "PH";
        if (mode.contains("RTTY")) return "RY";
        return "DG"; // Digital
    }

    /**
     * Extract exchange information from contest data
     */
    private String extractExchange(QSO qso, Contest contest, boolean sent) {
        try {
            if (qso.getContestData() == null) return "";

            JsonNode contestData = objectMapper.readTree(qso.getContestData());

            // Field Day / Winter Field Day: class and section
            if (contest.getContestCode().contains("FD")) {
                String classValue = contestData.has("class") ? contestData.get("class").asText() : "";
                String section = contestData.has("section") ? contestData.get("section").asText() : "";
                return classValue + " " + section;
            }

            // Default: return section or state
            if (contestData.has("section")) {
                return contestData.get("section").asText();
            }

            if (qso.getState() != null) {
                return qso.getState();
            }

            return "";

        } catch (Exception e) {
            log.warn("Error extracting exchange for QSO {}: {}", qso.getId(), e.getMessage());
            return "";
        }
    }
}
