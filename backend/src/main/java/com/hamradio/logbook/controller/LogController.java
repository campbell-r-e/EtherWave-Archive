package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.log.LogParticipantResponse;
import com.hamradio.logbook.dto.log.LogRequest;
import com.hamradio.logbook.dto.log.LogResponse;
import com.hamradio.logbook.dto.log.StationAssignmentRequest;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.service.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * Get all logs for the current user
     */
    @GetMapping
    public ResponseEntity<List<LogResponse>> getMyLogs(Authentication authentication) {
        String username = authentication.getName();
        List<LogResponse> logs = logService.getLogsForUser(username);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get a specific log by ID
     */
    @GetMapping("/{logId}")
    public ResponseEntity<LogResponse> getLog(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        LogResponse log = logService.getLogById(logId, username);
        return ResponseEntity.ok(log);
    }

    /**
     * Create a new log
     */
    @PostMapping
    public ResponseEntity<LogResponse> createLog(
            @Valid @RequestBody LogRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        LogResponse log = logService.createLog(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    /**
     * Update an existing log
     */
    @PutMapping("/{logId}")
    public ResponseEntity<LogResponse> updateLog(
            @PathVariable Long logId,
            @Valid @RequestBody LogRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        LogResponse log = logService.updateLog(logId, request, username);
        return ResponseEntity.ok(log);
    }

    /**
     * Delete a log (soft delete)
     */
    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> deleteLog(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        logService.deleteLog(logId, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Freeze a log (prevent further edits)
     */
    @PostMapping("/{logId}/freeze")
    public ResponseEntity<LogResponse> freezeLog(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        LogResponse log = logService.freezeLog(logId, username);
        return ResponseEntity.ok(log);
    }

    /**
     * Unfreeze a log (allow edits again)
     */
    @PostMapping("/{logId}/unfreeze")
    public ResponseEntity<LogResponse> unfreezeLog(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        LogResponse log = logService.unfreezeLog(logId, username);
        return ResponseEntity.ok(log);
    }

    /**
     * Get all participants for a log
     */
    @GetMapping("/{logId}/participants")
    public ResponseEntity<List<LogParticipantResponse>> getLogParticipants(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        List<LogParticipant> participants = logService.getLogParticipants(logId, username);

        List<LogParticipantResponse> responses = participants.stream()
                .map(LogParticipantResponse::fromLogParticipant)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Remove a participant from a log
     */
    @DeleteMapping("/{logId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long logId,
            @PathVariable Long participantId,
            Authentication authentication) {
        String username = authentication.getName();
        logService.removeParticipant(logId, participantId, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update participant station assignment
     * Only the log creator can assign stations
     */
    @PutMapping("/{logId}/participants/{participantId}/station")
    public ResponseEntity<LogParticipantResponse> updateParticipantStation(
            @PathVariable Long logId,
            @PathVariable Long participantId,
            @Valid @RequestBody StationAssignmentRequest request,
            Authentication authentication) {

        // Validate that stationNumber and isGota are mutually exclusive
        if (!request.isValid()) {
            throw new IllegalArgumentException("A participant cannot be both GOTA and assigned to a station number");
        }

        String username = authentication.getName();
        LogParticipant participant = logService.updateParticipantStation(
                logId, participantId, request, username);

        return ResponseEntity.ok(LogParticipantResponse.fromLogParticipant(participant));
    }

    /**
     * Get current user's participant record for a log (station assignment)
     */
    @GetMapping("/{logId}/my-assignment")
    public ResponseEntity<LogParticipantResponse> getMyAssignment(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        LogParticipant participant = logService.getMyParticipation(logId, username);

        if (participant == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(LogParticipantResponse.fromLogParticipant(participant));
    }

    /**
     * Convert a personal log to a shared log (one-way, creator only)
     */
    @PostMapping("/{logId}/convert-to-shared")
    public ResponseEntity<LogResponse> convertToShared(
            @PathVariable Long logId,
            Authentication authentication) {
        LogResponse log = logService.convertToShared(logId, authentication.getName());
        return ResponseEntity.ok(log);
    }

    /**
     * Leave a log (remove yourself as participant)
     */
    @PostMapping("/{logId}/leave")
    public ResponseEntity<Void> leaveLog(
            @PathVariable Long logId,
            Authentication authentication) {
        String username = authentication.getName();
        logService.leaveLog(logId, username);
        return ResponseEntity.noContent().build();
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
