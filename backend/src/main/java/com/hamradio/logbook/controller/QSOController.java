package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.QSORequest;
import com.hamradio.logbook.dto.QSOResponse;
import com.hamradio.logbook.service.LogService;
import com.hamradio.logbook.service.QSOService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for QSO operations
 * Updated to support multi-user log management with permission checks
 */
@RestController
@RequestMapping("/api/qsos")
@RequiredArgsConstructor
public class QSOController {

    private final QSOService qsoService;
    private final LogService logService;

    /**
     * Create new QSO in a specific log
     * POST /api/qsos?logId={logId}
     */
    @PostMapping
    public ResponseEntity<QSOResponse> createQSO(
            @Valid @RequestBody QSORequest request,
            @RequestParam Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        QSOResponse response = qsoService.createQSO(request, logId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get QSO by ID
     * GET /api/qsos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<QSOResponse> getQSO(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        QSOResponse response = qsoService.getQSO(id, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Update QSO
     * PUT /api/qsos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<QSOResponse> updateQSO(
            @PathVariable Long id,
            @Valid @RequestBody QSORequest request,
            Authentication authentication) {
        String username = authentication.getName();
        QSOResponse response = qsoService.updateQSO(id, request, username);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete QSO
     * DELETE /api/qsos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQSO(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        qsoService.deleteQSO(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all QSOs for a specific log with pagination
     * GET /api/qsos?logId={logId}&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<QSOResponse>> getAllQSOs(
            @RequestParam Long logId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String username = authentication.getName();
        Page<QSOResponse> qsos = qsoService.getAllQSOs(logId, page, size, username);
        return ResponseEntity.ok(qsos);
    }

    /**
     * Get recent QSOs for a specific log (for live feed)
     * GET /api/qsos/recent?logId={logId}&limit=10
     */
    @GetMapping("/recent")
    public ResponseEntity<List<QSOResponse>> getRecentQSOs(
            @RequestParam Long logId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        String username = authentication.getName();
        List<QSOResponse> qsos = qsoService.getRecentQSOs(logId, limit, username);
        return ResponseEntity.ok(qsos);
    }

    /**
     * Get QSOs by date range for a specific log
     * GET /api/qsos/range?logId={logId}&startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/range")
    public ResponseEntity<List<QSOResponse>> getQSOsByDateRange(
            @RequestParam Long logId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        String username = authentication.getName();
        List<QSOResponse> qsos = qsoService.getQSOsByDateRange(logId, startDate, endDate, username);
        return ResponseEntity.ok(qsos);
    }

    /**
     * Get distinct states contacted for a specific log (for map visualization)
     * GET /api/qsos/states?logId={logId}
     */
    @GetMapping("/states")
    public ResponseEntity<List<String>> getContactedStates(
            @RequestParam Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        List<String> states = qsoService.getContactedStates(logId, username);
        return ResponseEntity.ok(states);
    }

    /**
     * Global exception handler for this controller
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        ErrorResponse error = new ErrorResponse("FORBIDDEN", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse("CONFLICT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Error response DTO
     */
    private record ErrorResponse(String code, String message) {}
}
