package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.QSORepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting logs in Cabrillo format
 * Supports both contest and personal/non-contest log exports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CabrilloExportService {

    private final QSORepository qsoRepository;
    private final LogRepository logRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    /**
     * Export QSOs for a log in Cabrillo format
     * Handles both contest and personal logs appropriately
     */
    public byte[] exportLog(Long logId, String callsign, String operators, String category) {
        Log qsoLog = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        List<QSO> qsos = qsoRepository.findByLogIdAndDateRange(
                logId,
                null,
                null
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8);

        // Write Cabrillo header
        Contest contest = qsoLog.getContest();
        writeHeader(writer, contest, callsign, operators, category, qsos.size());

        // Write QSO records
        for (QSO qso : qsos) {
            writeQSO(writer, qso, contest);
        }

        // Write footer
        writer.println("END-OF-LOG:");
        writer.flush();

        String logType = contest != null ? "contest " + contest.getContestCode() : "personal log";
        log.info("Exported {} QSOs to Cabrillo format for {}", qsos.size(), logType);
        return baos.toByteArray();
    }

    /**
     * Export QSOs for a contest in Cabrillo format
     * @deprecated Use exportLog() instead for better flexibility
     */
    @Deprecated
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
     * Handles both contest and non-contest logs
     */
    private void writeHeader(PrintWriter writer, Contest contest, String callsign,
                             String operators, String category, int qsoCount) {
        writer.println("START-OF-LOG: 3.0");
        writer.println("CREATED-BY: Ham Radio Logbook System v1.0");

        // Contest name or generic label for personal logs
        if (contest != null) {
            writer.println("CONTEST: " + contest.getContestName());
        } else {
            writer.println("CONTEST: PERSONAL-LOG");
        }

        if (callsign != null) {
            writer.println("CALLSIGN: " + callsign);
        }

        if (category != null) {
            writer.println("CATEGORY: " + category);
        } else if (contest == null) {
            writer.println("CATEGORY: CHECKLOG");
        }

        if (operators != null) {
            writer.println("OPERATORS: " + operators);
        }

        writer.println("CLAIMED-SCORE: 0");
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
     * For non-contest logs, uses RST or generic exchange
     */
    private String extractExchange(QSO qso, Contest contest, boolean sent) {
        try {
            // For non-contest logs, use RST or signal report
            if (contest == null) {
                String rst = sent ? qso.getRstSent() : qso.getRstRcvd();
                return rst != null ? rst : "59";
            }

            if (qso.getContestData() == null || qso.getContestData().isEmpty()) {
                // No contest data - use state or RST as fallback
                if (qso.getState() != null && !qso.getState().isEmpty()) {
                    return qso.getState();
                }
                String rst = sent ? qso.getRstSent() : qso.getRstRcvd();
                return rst != null ? rst : "59";
            }

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
