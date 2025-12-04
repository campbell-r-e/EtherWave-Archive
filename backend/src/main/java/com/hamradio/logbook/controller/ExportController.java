package com.hamradio.logbook.controller;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.repository.ContestRepository;
import com.hamradio.logbook.service.AdifExportService;
import com.hamradio.logbook.service.CabrilloExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for exporting logbook data
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ExportController {

    private final AdifExportService adifExportService;
    private final CabrilloExportService cabrilloExportService;
    private final ContestRepository contestRepository;

    /**
     * Export log as ADIF (combined - all stations)
     * GET /api/export/adif/log/{logId}/combined
     */
    @GetMapping("/adif/log/{logId}/combined")
    public ResponseEntity<byte[]> exportLogCombinedAsADIF(@PathVariable Long logId) {
        byte[] adifData = adifExportService.exportQSOsByLog(logId);

        String filename = String.format("log_%d_combined_%s.adi",
                logId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(adifData);
    }

    /**
     * Export GOTA QSOs only as ADIF
     * GET /api/export/adif/log/{logId}/gota
     */
    @GetMapping("/adif/log/{logId}/gota")
    public ResponseEntity<byte[]> exportLogGotaAsADIF(@PathVariable Long logId) {
        byte[] adifData = adifExportService.exportGotaQSOs(logId);

        String filename = String.format("log_%d_gota_%s.adi",
                logId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(adifData);
    }

    /**
     * Export non-GOTA QSOs only as ADIF
     * GET /api/export/adif/log/{logId}/non-gota
     */
    @GetMapping("/adif/log/{logId}/non-gota")
    public ResponseEntity<byte[]> exportLogNonGotaAsADIF(@PathVariable Long logId) {
        byte[] adifData = adifExportService.exportNonGotaQSOs(logId);

        String filename = String.format("log_%d_non_gota_%s.adi",
                logId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(adifData);
    }

    /**
     * Export log as ADIF (legacy - backwards compatible)
     * GET /api/export/adif/log/{logId}
     */
    @GetMapping("/adif/log/{logId}")
    public ResponseEntity<byte[]> exportLogAsADIF(@PathVariable Long logId) {
        // Default to combined export for backwards compatibility
        return exportLogCombinedAsADIF(logId);
    }

    /**
     * Export all QSOs as ADIF (legacy - use /adif/log/{logId} instead)
     * GET /api/export/adif
     * @deprecated Use exportLogAsADIF instead
     */
    @Deprecated
    @GetMapping("/adif")
    public ResponseEntity<byte[]> exportAllADIF() {
        byte[] adifData = adifExportService.exportAllQSOs();

        String filename = "logbook_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".adi";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(adifData);
    }

    /**
     * Export QSOs by date range as ADIF
     * GET /api/export/adif/range?startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/adif/range")
    public ResponseEntity<byte[]> exportADIFByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] adifData = adifExportService.exportQSOsByDateRange(startDate, endDate);

        String filename = String.format("logbook_%s_to_%s.adi",
                startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(adifData);
    }

    /**
     * Export log as Cabrillo (supports both contest and personal logs)
     * GET /api/export/cabrillo/log/{logId}?callsign=W1AW&operators=W1AW&category=SINGLE-OP
     * Supports optional category fields for full Cabrillo 3.0 compliance:
     * - categoryBand, categoryMode, categoryPower, categoryOperator, categoryTransmitter, categoryOverlay
     */
    @GetMapping("/cabrillo/log/{logId}")
    public ResponseEntity<byte[]> exportLogAsCabrillo(
            @PathVariable Long logId,
            @RequestParam String callsign,
            @RequestParam(required = false) String operators,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryBand,
            @RequestParam(required = false) String categoryMode,
            @RequestParam(required = false) String categoryPower,
            @RequestParam(required = false) String categoryOperator,
            @RequestParam(required = false) String categoryTransmitter,
            @RequestParam(required = false) String categoryOverlay) {

        byte[] cabrilloData = cabrilloExportService.exportLog(
                logId, callsign, operators, category,
                categoryBand, categoryMode, categoryPower, categoryOperator, categoryTransmitter, categoryOverlay);

        String filename = String.format("log_%d_%s.log",
                logId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(cabrilloData);
    }

    /**
     * Export combined Cabrillo (all QSOs including GOTA)
     * GET /api/export/cabrillo/log/{logId}/combined?callsign=W1AW&operators=W1AW&category=SINGLE-OP
     */
    @GetMapping("/cabrillo/log/{logId}/combined")
    public ResponseEntity<byte[]> exportLogCombinedAsCabrillo(
            @PathVariable Long logId,
            @RequestParam String callsign,
            @RequestParam(required = false) String operators,
            @RequestParam(required = false) String category) {

        byte[] cabrilloData = cabrilloExportService.exportCombined(logId, callsign, operators, category);

        String filename = String.format("log_%d_combined_%s.log",
                logId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(cabrilloData);
    }

    /**
     * Export GOTA QSOs only as Cabrillo
     * GET /api/export/cabrillo/log/{logId}/gota?callsign=W1AW&operators=W1AW&category=SINGLE-OP
     */
    @GetMapping("/cabrillo/log/{logId}/gota")
    public ResponseEntity<byte[]> exportLogGotaAsCabrillo(
            @PathVariable Long logId,
            @RequestParam String callsign,
            @RequestParam(required = false) String operators,
            @RequestParam(required = false) String category) {

        byte[] cabrilloData = cabrilloExportService.exportGotaQSOs(logId, callsign, operators, category);

        String filename = String.format("log_%d_gota_%s.log",
                logId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(cabrilloData);
    }

    /**
     * Export non-GOTA QSOs only as Cabrillo
     * GET /api/export/cabrillo/log/{logId}/non-gota?callsign=W1AW&operators=W1AW&category=SINGLE-OP
     */
    @GetMapping("/cabrillo/log/{logId}/non-gota")
    public ResponseEntity<byte[]> exportLogNonGotaAsCabrillo(
            @PathVariable Long logId,
            @RequestParam String callsign,
            @RequestParam(required = false) String operators,
            @RequestParam(required = false) String category) {

        byte[] cabrilloData = cabrilloExportService.exportNonGotaQSOs(logId, callsign, operators, category);

        String filename = String.format("log_%d_non_gota_%s.log",
                logId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(cabrilloData);
    }

    /**
     * Export contest log as Cabrillo (legacy - use /cabrillo/log/{logId} instead)
     * GET /api/export/cabrillo/{contestId}?callsign=W1AW&operators=W1AW&category=SINGLE-OP
     * @deprecated Use exportLogAsCabrillo instead
     */
    @Deprecated
    @GetMapping("/cabrillo/{contestId}")
    public ResponseEntity<byte[]> exportCabrillo(
            @PathVariable Long contestId,
            @RequestParam String callsign,
            @RequestParam(required = false) String operators,
            @RequestParam(required = false) String category) {

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new IllegalArgumentException("Contest not found"));

        byte[] cabrilloData = cabrilloExportService.exportContestLog(contest, callsign, operators, category);

        String filename = String.format("%s_%s.cbr",
                contest.getContestCode(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(cabrilloData);
    }
}
