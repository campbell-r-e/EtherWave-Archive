package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.QSORequest;
import com.hamradio.logbook.dto.QSOResponse;
import com.hamradio.logbook.service.QSOService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for QSO operations
 */
@RestController
@RequestMapping("/api/qsos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class QSOController {

    private final QSOService qsoService;

    /**
     * Create new QSO
     * POST /api/qsos
     */
    @PostMapping
    public ResponseEntity<QSOResponse> createQSO(@Valid @RequestBody QSORequest request) {
        QSOResponse response = qsoService.createQSO(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get QSO by ID
     * GET /api/qsos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<QSOResponse> getQSO(@PathVariable Long id) {
        QSOResponse response = qsoService.getQSO(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update QSO
     * PUT /api/qsos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<QSOResponse> updateQSO(@PathVariable Long id, @Valid @RequestBody QSORequest request) {
        QSOResponse response = qsoService.updateQSO(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete QSO
     * DELETE /api/qsos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQSO(@PathVariable Long id) {
        qsoService.deleteQSO(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all QSOs with pagination
     * GET /api/qsos?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<QSOResponse>> getAllQSOs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<QSOResponse> qsos = qsoService.getAllQSOs(page, size);
        return ResponseEntity.ok(qsos);
    }

    /**
     * Get recent QSOs (for live feed)
     * GET /api/qsos/recent?limit=10
     */
    @GetMapping("/recent")
    public ResponseEntity<List<QSOResponse>> getRecentQSOs(@RequestParam(defaultValue = "10") int limit) {
        List<QSOResponse> qsos = qsoService.getRecentQSOs(limit);
        return ResponseEntity.ok(qsos);
    }

    /**
     * Get QSOs by date range
     * GET /api/qsos/range?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/range")
    public ResponseEntity<List<QSOResponse>> getQSOsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<QSOResponse> qsos = qsoService.getQSOsByDateRange(startDate, endDate);
        return ResponseEntity.ok(qsos);
    }

    /**
     * Get distinct states contacted (for map visualization)
     * GET /api/qsos/states
     */
    @GetMapping("/states")
    public ResponseEntity<List<String>> getContactedStates() {
        List<String> states = qsoService.getContactedStates();
        return ResponseEntity.ok(states);
    }
}
