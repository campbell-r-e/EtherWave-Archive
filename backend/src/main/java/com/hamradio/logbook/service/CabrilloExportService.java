package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final StationRepository stationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    /**
     * Export QSOs for a log in Cabrillo format (backward compatible)
     * Handles both contest and personal logs appropriately
     */
    public byte[] exportLog(Long logId, String callsign, String operators, String category) {
        return exportLog(logId, callsign, operators, category, null, null, null, null, null, null);
    }

    /**
     * Export QSOs for a log in Cabrillo format
     * Handles both contest and personal logs appropriately
     * Supports full Cabrillo 3.0 category specification
     */
    public byte[] exportLog(Long logId, String callsign, String operators, String category,
                            String categoryBand, String categoryMode, String categoryPower,
                            String categoryOperator, String categoryTransmitter, String categoryOverlay) {
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
        writeHeader(writer, contest, callsign, operators, category, qsos.size(),
                categoryBand, categoryMode, categoryPower, categoryOperator, categoryTransmitter, categoryOverlay);

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
     * Export combined Cabrillo (all QSOs including GOTA)
     * This is the same as exportLog() but with clearer naming
     */
    public byte[] exportCombined(Long logId, String callsign, String operators, String category) {
        return exportLog(logId, callsign, operators, category, null, null, null, null, null, null);
    }

    /**
     * Export GOTA QSOs only in Cabrillo format
     */
    public byte[] exportGotaQSOs(Long logId, String callsign, String operators, String category) {
        Log qsoLog = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        // Get all GOTA stations
        List<Station> gotaStations = stationRepository.findAll().stream()
                .filter(s -> s.getIsGota() != null && s.getIsGota())
                .collect(Collectors.toList());

        // Get QSOs for GOTA stations only
        List<QSO> gotaQsos = new ArrayList<>();
        for (Station gotaStation : gotaStations) {
            List<QSO> stationQsos = qsoRepository.findByLogIdAndStationId(logId, gotaStation.getId());
            gotaQsos.addAll(stationQsos);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8);

        // Write Cabrillo header
        Contest contest = qsoLog.getContest();
        writeHeader(writer, contest, callsign, operators, category, gotaQsos.size(),
                null, null, null, null, null, null);

        // Write QSO records
        for (QSO qso : gotaQsos) {
            writeQSO(writer, qso, contest);
        }

        // Write footer
        writer.println("END-OF-LOG:");
        writer.flush();

        log.info("Exported {} GOTA QSOs to Cabrillo format for log {}", gotaQsos.size(), logId);
        return baos.toByteArray();
    }

    /**
     * Export non-GOTA QSOs only in Cabrillo format
     */
    public byte[] exportNonGotaQSOs(Long logId, String callsign, String operators, String category) {
        Log qsoLog = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));

        // Get all GOTA stations
        List<Station> gotaStations = stationRepository.findAll().stream()
                .filter(s -> s.getIsGota() != null && s.getIsGota())
                .collect(Collectors.toList());

        List<Long> gotaStationIds = gotaStations.stream()
                .map(Station::getId)
                .collect(Collectors.toList());

        // Get all QSOs for this log
        List<QSO> allQsos = qsoRepository.findByLogIdAndDateRange(logId, null, null);

        // Filter out GOTA QSOs
        List<QSO> nonGotaQsos = allQsos.stream()
                .filter(qso -> qso.getStation() != null && !gotaStationIds.contains(qso.getStation().getId()))
                .collect(Collectors.toList());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8);

        // Write Cabrillo header
        Contest contest = qsoLog.getContest();
        writeHeader(writer, contest, callsign, operators, category, nonGotaQsos.size(),
                null, null, null, null, null, null);

        // Write QSO records
        for (QSO qso : nonGotaQsos) {
            writeQSO(writer, qso, contest);
        }

        // Write footer
        writer.println("END-OF-LOG:");
        writer.flush();

        log.info("Exported {} non-GOTA QSOs to Cabrillo format for log {}", nonGotaQsos.size(), logId);
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
        writeHeader(writer, contest, callsign, operators, category, qsos.size(),
                null, null, null, null, null, null);

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
     * Supports full Cabrillo 3.0 category specification
     */
    private void writeHeader(PrintWriter writer, Contest contest, String callsign,
                             String operators, String category, int qsoCount,
                             String categoryBand, String categoryMode, String categoryPower,
                             String categoryOperator, String categoryTransmitter, String categoryOverlay) {
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

        // Legacy single category field (backward compatibility)
        if (category != null) {
            writer.println("CATEGORY: " + category);
        } else if (contest == null) {
            writer.println("CATEGORY: CHECKLOG");
        }

        // Cabrillo 3.0 separate category fields
        if (categoryBand != null) {
            writer.println("CATEGORY-BAND: " + categoryBand);
        }

        if (categoryMode != null) {
            writer.println("CATEGORY-MODE: " + categoryMode);
        }

        if (categoryPower != null) {
            writer.println("CATEGORY-POWER: " + categoryPower);
        }

        if (categoryOperator != null) {
            writer.println("CATEGORY-OPERATOR: " + categoryOperator);
        }

        if (categoryTransmitter != null) {
            writer.println("CATEGORY-TRANSMITTER: " + categoryTransmitter);
        }

        if (categoryOverlay != null) {
            writer.println("CATEGORY-OVERLAY: " + categoryOverlay);
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
