package com.hamradio.logbook.controller;

import com.hamradio.logbook.service.AdifImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for importing logbook data
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class ImportController {

    private final AdifImportService adifImportService;

    /**
     * Import ADIF file into a specific log
     * POST /api/import/adif/{logId}?stationId=1
     */
    @PostMapping("/adif/{logId}")
    public ResponseEntity<Map<String, Object>> importAdif(
            @PathVariable Long logId,
            @RequestParam Long stationId,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file type
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".adi") &&
                                    !filename.toLowerCase().endsWith(".adif"))) {
                response.put("success", false);
                response.put("message", "File must be an ADIF file (.adi or .adif)");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Importing ADIF file {} into log {}", filename, logId);

            // Import the file
            AdifImportService.ImportResult result = adifImportService.importAdif(
                file.getBytes(), logId, stationId);

            // Build response
            response.put("success", result.errorCount == 0);
            response.put("totalRecords", result.totalRecords);
            response.put("successCount", result.successCount);
            response.put("errorCount", result.errorCount);
            response.put("errors", result.errors);
            response.put("importedQSOs", result.importedQSOs.stream()
                .map(qso -> {
                    Map<String, Object> qsoMap = new HashMap<>();
                    qsoMap.put("id", qso.getId());
                    qsoMap.put("callsign", qso.getCallsign());
                    qsoMap.put("qsoDate", qso.getQsoDate());
                    qsoMap.put("timeOn", qso.getTimeOn());
                    qsoMap.put("mode", qso.getMode());
                    qsoMap.put("band", qso.getBand());
                    return qsoMap;
                })
                .toList());

            if (result.errorCount > 0) {
                response.put("message", String.format(
                    "Import completed with %d errors. %d of %d records imported successfully.",
                    result.errorCount, result.successCount, result.totalRecords));
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(response);
            } else {
                response.put("message", String.format(
                    "Import successful! %d records imported.",
                    result.successCount));
                return ResponseEntity.ok(response);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid import request: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error importing ADIF file", e);
            response.put("success", false);
            response.put("message", "Failed to import ADIF file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
