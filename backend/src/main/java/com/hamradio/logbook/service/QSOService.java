package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.QSORequest;
import com.hamradio.logbook.dto.QSOResponse;
import com.hamradio.logbook.entity.*;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QSOService {

    private final QSORepository qsoRepository;
    private final StationRepository stationRepository;
    private final OperatorRepository operatorRepository;
    private final ContestRepository contestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final QSOValidationService validationService;

    /**
     * Create a new QSO
     */
    @Transactional
    public QSOResponse createQSO(QSORequest request) {
        log.info("Creating new QSO for callsign: {}", request.getCallsign());

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

        // Build QSO entity
        QSO qso = QSO.builder()
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

        // Convert to response
        QSOResponse response = toResponse(savedQSO);

        // Broadcast new QSO via WebSocket
        messagingTemplate.convertAndSend("/topic/qsos", response);

        return response;
    }

    /**
     * Get QSO by ID
     */
    public QSOResponse getQSO(Long id) {
        QSO qso = qsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("QSO not found"));
        return toResponse(qso);
    }

    /**
     * Update existing QSO
     */
    @Transactional
    public QSOResponse updateQSO(Long id, QSORequest request) {
        QSO qso = qsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("QSO not found"));

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
        return toResponse(updatedQSO);
    }

    /**
     * Delete QSO
     */
    @Transactional
    public void deleteQSO(Long id) {
        if (!qsoRepository.existsById(id)) {
            throw new IllegalArgumentException("QSO not found");
        }
        qsoRepository.deleteById(id);
    }

    /**
     * Get all QSOs with pagination
     */
    public Page<QSOResponse> getAllQSOs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return qsoRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Get recent QSOs (for live feed)
     */
    public List<QSOResponse> getRecentQSOs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return qsoRepository.findRecent(pageable)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get QSOs by date range
     */
    public List<QSOResponse> getQSOsByDateRange(LocalDate startDate, LocalDate endDate) {
        return qsoRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get distinct states contacted (for map visualization)
     */
    public List<String> getContactedStates() {
        return qsoRepository.findDistinctStates();
    }

    /**
     * Convert QSO entity to response DTO
     */
    private QSOResponse toResponse(QSO qso) {
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
}
