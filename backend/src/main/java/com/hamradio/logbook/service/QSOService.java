package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.QSORequest;
import com.hamradio.logbook.dto.QSOResponse;
import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.exception.ResourceNotFoundException;
import com.hamradio.logbook.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for QSO operations
 * Updated to support multi-user log management with permission checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QSOService {

    private final QSORepository qsoRepository;
    private final StationRepository stationRepository;
    private final OperatorRepository operatorRepository;
    private final ContestRepository contestRepository;
    private final LogRepository logRepository;
    private final UserRepository userRepository;
    private final LogParticipantRepository logParticipantRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final QSOValidationService validationService;
    private final LogService logService;
    private final ScoringService scoringService;

    /**
     * Create a new QSO in a specific log
     */
    @Transactional
    public QSOResponse createQSO(QSORequest request, Long logId, String username) {
        log.info("Creating new QSO for callsign: {} in log ID: {}", request.getCallsign(), logId);

        // Get user and log
        User user = getUserByUsername(username);
        Log qsoLog = getLogByIdOrThrow(logId);

        // Check if user can edit this log
        if (!logService.canEdit(qsoLog, user)) {
            throw new SecurityException("User does not have permission to add QSOs to this log");
        }

        // Check if log is editable
        if (!qsoLog.isEditable()) {
            throw new IllegalStateException("This log is frozen and cannot be edited");
        }

        // Fetch related entities
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new IllegalArgumentException("Station not found"));

        Operator operator = null;
        if (request.getOperatorId() != null) {
            operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found"));
        }

        Contest contest = null;
        if (request.getContestId() != null) {
            contest = contestRepository.findById(request.getContestId())
                    .orElseThrow(() -> new IllegalArgumentException("Contest not found"));
        }

        // Auto-tag QSO with station number and GOTA status from participant assignment
        Integer stationNumber = null;
        Boolean isGota = false;

        // Get participant record for auto-tagging (for shared logs)
        if (qsoLog.isShared()) {
            LogParticipant participant = logParticipantRepository.findByLogAndUser(qsoLog, user)
                    .orElse(null);
            if (participant != null && participant.getActive()) {
                stationNumber = participant.getStationNumber();
                isGota = participant.getIsGota() != null ? participant.getIsGota() : false;
                log.debug("Auto-tagging QSO with stationNumber={}, isGota={} from participant assignment",
                         stationNumber, isGota);
            }
        }

        // Build QSO entity
        QSO qso = QSO.builder()
                .log(qsoLog)
                .station(station)
                .operator(operator)
                .contest(contest)
                .callsign(request.getCallsign().toUpperCase())
                .frequencyKhz(request.getFrequencyKhz())
                .mode(request.getMode())
                .qsoDate(request.getQsoDate())
                .timeOn(request.getTimeOn())
                .timeOff(request.getTimeOff())
                .rstSent(request.getRstSent())
                .rstRcvd(request.getRstRcvd())
                .band(request.getBand())
                .powerWatts(request.getPowerWatts())
                .gridSquare(request.getGridSquare())
                .county(request.getCounty())
                .state(request.getState())
                .country(request.getCountry())
                .dxcc(request.getDxcc())
                .cqZone(request.getCqZone())
                .ituZone(request.getItuZone())
                .name(request.getName())
                .licenseClass(request.getLicenseClass())
                .stationNumber(stationNumber)  // Auto-tagged from participant
                .isGota(isGota)                 // Auto-tagged from participant
                .contestData(request.getContestData())
                .qslSent(request.getQslSent())
                .qslRcvd(request.getQslRcvd())
                .lotwSent(request.getLotwSent())
                .lotwRcvd(request.getLotwRcvd())
                .notes(request.getNotes())
                .isValid(true)
                .build();

        // Validate QSO
        var validationResult = validationService.validateQSO(qso);
        qso.setIsValid(validationResult.isValid());
        if (validationResult.hasErrors() || validationResult.hasWarnings()) {
            String errors = String.join("; ", validationResult.getErrors());
            String warnings = String.join("; ", validationResult.getWarnings());
            String combined = "";
            if (!errors.isEmpty()) combined += "Errors: " + errors;
            if (!errors.isEmpty() && !warnings.isEmpty()) combined += " | ";
            if (!warnings.isEmpty()) combined += "Warnings: " + warnings;
            qso.setValidationErrors(combined);
            log.info("QSO validation result - Valid: {}, Errors: {}, Warnings: {}",
                    validationResult.isValid(), validationResult.getErrors().size(), validationResult.getWarnings().size());
        }

        // Save QSO
        QSO savedQSO = qsoRepository.save(qso);
        log.info("QSO saved with ID: {}", savedQSO.getId());

        // Calculate scoring (points, duplicates, multipliers)
        try {
            scoringService.updateScoreForQso(savedQSO);
            log.debug("Scoring calculated for QSO {}: points={}, isDupe={}, isMult={}",
                    savedQSO.getId(), savedQSO.getPoints(), savedQSO.getIsDuplicate(), savedQSO.getIsMultiplier());
        } catch (Exception e) {
            log.error("Error calculating score for QSO {}: {}", savedQSO.getId(), e.getMessage());
        }

        // Convert to response
        QSOResponse response = toResponse(savedQSO);

        // Broadcast new QSO via WebSocket
        messagingTemplate.convertAndSend("/topic/qsos", response);

        return response;
    }

    /**
     * Get QSO by ID with permission check
     */
    @Transactional(readOnly = true)
    public QSOResponse getQSO(Long id, String username) {
        User user = getUserByUsername(username);
        QSO qso = qsoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QSO not found"));

        // Check if user has access to this log
        if (!logService.hasAccess(qso.getLog(), user)) {
            throw new SecurityException("User does not have access to this QSO");
        }

        return toResponse(qso);
    }

    /**
     * Update existing QSO with permission check
     */
    @Transactional
    public QSOResponse updateQSO(Long id, QSORequest request, String username) {
        User user = getUserByUsername(username);
        QSO qso = qsoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QSO not found"));

        // Check if user can edit this log
        if (!logService.canEdit(qso.getLog(), user)) {
            throw new SecurityException("User does not have permission to edit QSOs in this log");
        }

        // Check if log is editable
        if (!qso.getLog().isEditable()) {
            throw new IllegalStateException("This log is frozen and cannot be edited");
        }

        // Update fields
        qso.setCallsign(request.getCallsign().toUpperCase());
        qso.setFrequencyKhz(request.getFrequencyKhz());
        qso.setMode(request.getMode());
        qso.setQsoDate(request.getQsoDate());
        qso.setTimeOn(request.getTimeOn());
        qso.setTimeOff(request.getTimeOff());
        qso.setRstSent(request.getRstSent());
        qso.setRstRcvd(request.getRstRcvd());
        qso.setBand(request.getBand());
        qso.setPowerWatts(request.getPowerWatts());
        qso.setGridSquare(request.getGridSquare());
        qso.setCounty(request.getCounty());
        qso.setState(request.getState());
        qso.setCountry(request.getCountry());
        qso.setName(request.getName());
        qso.setLicenseClass(request.getLicenseClass());
        qso.setContestData(request.getContestData());
        qso.setNotes(request.getNotes());

        // Re-validate QSO after update
        var validationResult = validationService.validateQSO(qso);
        qso.setIsValid(validationResult.isValid());
        if (validationResult.hasErrors() || validationResult.hasWarnings()) {
            String errors = String.join("; ", validationResult.getErrors());
            String warnings = String.join("; ", validationResult.getWarnings());
            String combined = "";
            if (!errors.isEmpty()) combined += "Errors: " + errors;
            if (!errors.isEmpty() && !warnings.isEmpty()) combined += " | ";
            if (!warnings.isEmpty()) combined += "Warnings: " + warnings;
            qso.setValidationErrors(combined);
        } else {
            qso.setValidationErrors(null);
        }

        QSO updatedQSO = qsoRepository.save(qso);

        // Recalculate scoring after update
        try {
            scoringService.updateScoreForQso(updatedQSO);
            log.debug("Scoring recalculated for QSO {}: points={}, isDupe={}, isMult={}",
                    updatedQSO.getId(), updatedQSO.getPoints(), updatedQSO.getIsDuplicate(), updatedQSO.getIsMultiplier());
        } catch (Exception e) {
            log.error("Error recalculating score for QSO {}: {}", updatedQSO.getId(), e.getMessage());
        }

        return toResponse(updatedQSO);
    }

    /**
     * Delete QSO with permission check
     */
    @Transactional
    public void deleteQSO(Long id, String username) {
        User user = getUserByUsername(username);
        QSO qso = qsoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QSO not found"));

        // Check if user can edit this log
        if (!logService.canEdit(qso.getLog(), user)) {
            throw new SecurityException("User does not have permission to delete QSOs in this log");
        }

        // Check if log is editable
        if (!qso.getLog().isEditable()) {
            throw new IllegalStateException("This log is frozen and cannot be edited");
        }

        qsoRepository.deleteById(id);
    }

    /**
     * Get all QSOs for a specific log with pagination
     */
    @Transactional(readOnly = true)
    public Page<QSOResponse> getAllQSOs(Long logId, int page, int size, String username) {
        User user = getUserByUsername(username);
        Log qsoLog = getLogByIdOrThrow(logId);

        // Check if user has access to this log
        if (!logService.hasAccess(qsoLog, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<QSO> qsoPage = qsoRepository.findByLogId(logId, pageable);

        // Log any null QSOs found
        qsoPage.getContent().stream()
                .filter(qso -> qso == null)
                .forEach(qso -> log.error("Found null QSO in page for log ID: {}", logId));

        // Map to response (null check handled in toResponse method)
        return qsoPage.map(this::toResponse);
    }

    /**
     * Get recent QSOs for a specific log (for live feed)
     */
    @Transactional(readOnly = true)
    public List<QSOResponse> getRecentQSOs(Long logId, int limit, String username) {
        User user = getUserByUsername(username);
        Log qsoLog = getLogByIdOrThrow(logId);

        // Check if user has access to this log
        if (!logService.hasAccess(qsoLog, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        Pageable pageable = PageRequest.of(0, limit);
        return qsoRepository.findRecentByLogId(logId, pageable)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get QSOs by date range for a specific log
     */
    @Transactional(readOnly = true)
    public List<QSOResponse> getQSOsByDateRange(Long logId, LocalDate startDate, LocalDate endDate, String username) {
        User user = getUserByUsername(username);
        Log qsoLog = getLogByIdOrThrow(logId);

        // Check if user has access to this log
        if (!logService.hasAccess(qsoLog, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        return qsoRepository.findByLogIdAndDateRange(logId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get distinct states contacted for a specific log (for map visualization)
     */
    public List<String> getContactedStates(Long logId, String username) {
        User user = getUserByUsername(username);
        Log qsoLog = getLogByIdOrThrow(logId);

        // Check if user has access to this log
        if (!logService.hasAccess(qsoLog, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        return qsoRepository.findDistinctStatesByLogId(logId);
    }

    /**
     * Convert QSO entity to response DTO
     */
    private QSOResponse toResponse(QSO qso) {
        if (qso == null) {
            log.warn("Attempted to convert null QSO to response");
            return null;
        }
        return QSOResponse.builder()
                .id(qso.getId())
                .stationId(qso.getStation().getId())
                .stationName(qso.getStation().getStationName())
                .operatorId(qso.getOperator() != null ? qso.getOperator().getId() : null)
                .operatorCallsign(qso.getOperator() != null ? qso.getOperator().getCallsign() : null)
                .contestId(qso.getContest() != null ? qso.getContest().getId() : null)
                .contestCode(qso.getContest() != null ? qso.getContest().getContestCode() : null)
                .callsign(qso.getCallsign())
                .frequencyKhz(qso.getFrequencyKhz())
                .mode(qso.getMode())
                .qsoDate(qso.getQsoDate())
                .timeOn(qso.getTimeOn())
                .timeOff(qso.getTimeOff())
                .rstSent(qso.getRstSent())
                .rstRcvd(qso.getRstRcvd())
                .band(qso.getBand())
                .powerWatts(qso.getPowerWatts())
                .gridSquare(qso.getGridSquare())
                .county(qso.getCounty())
                .state(qso.getState())
                .country(qso.getCountry())
                .dxcc(qso.getDxcc())
                .cqZone(qso.getCqZone())
                .ituZone(qso.getItuZone())
                .name(qso.getName())
                .licenseClass(qso.getLicenseClass())
                .stationNumber(qso.getStationNumber())
                .isGota(qso.getIsGota())
                .contestData(qso.getContestData())
                .qslSent(qso.getQslSent())
                .qslRcvd(qso.getQslRcvd())
                .lotwSent(qso.getLotwSent())
                .lotwRcvd(qso.getLotwRcvd())
                .isValid(qso.getIsValid())
                .validationErrors(qso.getValidationErrors())
                .notes(qso.getNotes())
                .createdAt(qso.getCreatedAt())
                .updatedAt(qso.getUpdatedAt())
                .build();
    }

    /**
     * Helper: Get user by username or throw exception
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Helper: Get log by ID or throw exception
     */
    private Log getLogByIdOrThrow(Long logId) {
        return logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
    }
}
