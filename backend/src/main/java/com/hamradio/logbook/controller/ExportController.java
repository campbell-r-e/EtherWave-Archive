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
     * Export all QSOs as ADIF
     * GET /api/export/adif
     */
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
     */
    @GetMapping("/cabrillo/log/{logId}")
    public ResponseEntity<byte[]> exportLogAsCabrillo(
            @PathVariable Long logId,
            @RequestParam String callsign,
            @RequestParam(required = false) String operators,
            @RequestParam(required = false) String category) {

        byte[] cabrilloData = cabrilloExportService.exportLog(logId, callsign, operators, category);

        String filename = String.format("log_%d_%s.log",
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
