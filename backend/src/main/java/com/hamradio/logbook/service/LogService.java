package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.log.LogRequest;
import com.hamradio.logbook.dto.log.LogResponse;
import com.hamradio.logbook.dto.log.StationAssignmentRequest;
import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.exception.ResourceNotFoundException;
import com.hamradio.logbook.repository.ContestRepository;
import com.hamradio.logbook.repository.LogParticipantRepository;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.UserRepository;
import com.hamradio.logbook.repository.QSORepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;
    private final LogParticipantRepository logParticipantRepository;
    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final QSORepository qsoRepository;

    /**
     * Get all logs accessible to the current user
     */
    @Transactional(readOnly = true)
    public List<LogResponse> getLogsForUser(String username) {
        User user = getUserByUsername(username);
        List<Log> logs = logRepository.findLogsByUser(user);

        return logs.stream()
                .map(log -> enrichLogResponse(log, user))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific log by ID
     */
    @Transactional(readOnly = true)
    public LogResponse getLogById(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check access
        if (!hasAccess(log, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        return enrichLogResponse(log, user);
    }

    /**
     * Create a new log
     */
    @Transactional
    public LogResponse createLog(LogRequest request, String username) {
        User creator = getUserByUsername(username);

        // Create log entity
        Log log = new Log();
        log.setName(request.getName());
        log.setDescription(request.getDescription());
        log.setType(request.getType());
        log.setCreator(creator);
        log.setStartDate(request.getStartDate());
        log.setEndDate(request.getEndDate());
        log.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
        // Personal logs are always private
        if (log.getType() == Log.LogType.PERSONAL) {
            log.setIsPublic(false);
        }
        log.setActive(true);
        log.setEditable(true);

        // Associate contest if provided
        if (request.getContestId() != null) {
            Contest contest = contestRepository.findById(request.getContestId())
                    .orElseThrow(() -> new IllegalArgumentException("Contest not found: " + request.getContestId()));
            log.setContest(contest);
        }

        // Auto-link contest from purpose if no explicit contestId was provided
        log.setPurpose(request.getPurpose() != null ? request.getPurpose() : Log.LogPurpose.GENERAL);
        if (log.getContest() == null && request.getPurpose() != null) {
            String contestCode = purposeToContestCode(request.getPurpose());
            if (contestCode != null) {
                contestRepository.findByContestCode(contestCode)
                        .ifPresent(log::setContest);
            }
        }

        log = logRepository.save(log);

        // Create creator as participant with CREATOR role
        LogParticipant creatorParticipant = new LogParticipant();
        creatorParticipant.setLog(log);
        creatorParticipant.setUser(creator);
        creatorParticipant.setRole(LogParticipant.ParticipantRole.CREATOR);
        creatorParticipant.setActive(true);
        logParticipantRepository.save(creatorParticipant);

        LogService.log.info("Created log '{}' (ID: {}) for user '{}'", log.getName(), log.getId(), username);

        return enrichLogResponse(log, creator);
    }

    /**
     * Update an existing log
     */
    @Transactional
    public LogResponse updateLog(Long logId, LogRequest request, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check if user is creator
        if (!isCreator(log, user)) {
            throw new SecurityException("Only the log creator can update log details");
        }

        // Update fields
        log.setName(request.getName());
        log.setDescription(request.getDescription());
        log.setType(request.getType());
        log.setStartDate(request.getStartDate());
        log.setEndDate(request.getEndDate());
        log.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : log.getIsPublic());
        if (request.getBonusMetadata() != null) {
            log.setBonusMetadata(request.getBonusMetadata());
        }
        if (request.getPurpose() != null) {
            log.setPurpose(request.getPurpose());
        }

        // Update contest if provided
        if (request.getContestId() != null) {
            Contest contest = contestRepository.findById(request.getContestId())
                    .orElseThrow(() -> new IllegalArgumentException("Contest not found: " + request.getContestId()));
            log.setContest(contest);
        } else {
            log.setContest(null);
        }

        log = logRepository.save(log);

        LogService.log.info("Updated log '{}' (ID: {}) by user '{}'", log.getName(), log.getId(), username);

        return enrichLogResponse(log, user);
    }

    /**
     * Delete a log (soft delete by setting active = false)
     */
    @Transactional
    public void deleteLog(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check if user is creator
        if (!isCreator(log, user)) {
            throw new SecurityException("Only the log creator can delete this log");
        }

        // Soft delete
        log.setActive(false);
        logRepository.save(log);

        LogService.log.info("Deleted log '{}' (ID: {}) by user '{}'", log.getName(), log.getId(), username);
    }

    /**
     * Freeze a log (prevent further edits)
     */
    @Transactional
    public LogResponse freezeLog(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check if user is creator
        if (!isCreator(log, user)) {
            throw new SecurityException("Only the log creator can freeze this log");
        }

        log.setEditable(false);
        log = logRepository.save(log);

        LogService.log.info("Froze log '{}' (ID: {}) by user '{}'", log.getName(), log.getId(), username);

        return enrichLogResponse(log, user);
    }

    /**
     * Unfreeze a log (allow edits again)
     */
    @Transactional
    public LogResponse unfreezeLog(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check if user is creator
        if (!isCreator(log, user)) {
            throw new SecurityException("Only the log creator can unfreeze this log");
        }

        log.setEditable(true);
        log = logRepository.save(log);

        LogService.log.info("Unfroze log '{}' (ID: {}) by user '{}'", log.getName(), log.getId(), username);

        return enrichLogResponse(log, user);
    }

    /**
     * Get all participants for a log
     */
    @Transactional(readOnly = true)
    public List<LogParticipant> getLogParticipants(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check access
        if (!hasAccess(log, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        return logParticipantRepository.findActiveParticipantsByLogId(logId);
    }

    /**
     * Get current user's participant record for a log (station assignment)
     * Returns null if user is not a participant or log is personal
     */
    @Transactional(readOnly = true)
    public LogParticipant getMyParticipation(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Only shared logs have participants
        if (!log.isShared()) {
            return null;
        }

        // Check access
        if (!hasAccess(log, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        return logParticipantRepository.findByLogAndUser(log, user)
                .orElse(null);
    }

    /**
     * Remove a participant from a log
     */
    @Transactional
    public void removeParticipant(Long logId, Long participantId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check if user is creator
        if (!isCreator(log, user)) {
            throw new SecurityException("Only the log creator can remove participants");
        }

        LogParticipant participant = logParticipantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + participantId));

        // Can't remove creator
        if (participant.getRole() == LogParticipant.ParticipantRole.CREATOR) {
            throw new IllegalArgumentException("Cannot remove the log creator");
        }

        // Soft delete by setting active = false
        participant.setActive(false);
        logParticipantRepository.save(participant);

        LogService.log.info("Removed participant '{}' from log '{}' (ID: {})",
            participant.getUser().getUsername(), log.getName(), log.getId());
    }

    /**
     * Update participant station assignment
     * Only the log creator can assign stations
     */
    @Transactional
    public LogParticipant updateParticipantStation(Long logId, Long participantId,
                                                   StationAssignmentRequest request, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        // Check if user is creator
        if (!isCreator(log, user)) {
            throw new SecurityException("Only the log creator can assign stations");
        }

        // Get the participant
        LogParticipant participant = logParticipantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + participantId));

        // Verify participant belongs to this log
        if (!participant.getLog().getId().equals(logId)) {
            throw new IllegalArgumentException("Participant does not belong to this log");
        }

        // Update station number
        participant.setStationNumber(request.getStationNumber());

        // Update GOTA status
        participant.setIsGota(request.getIsGota() != null ? request.getIsGota() : false);

        participant = logParticipantRepository.save(participant);

        LogService.log.info("Updated station assignment for participant '{}' in log '{}' (ID: {}) - Station: {}, GOTA: {}",
            participant.getUser().getUsername(), log.getName(), log.getId(),
            participant.getStationNumber(), participant.getIsGota());

        return participant;
    }

    /**
     * Leave a log (user removes themselves)
     */
    @Transactional
    public void leaveLog(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        LogParticipant participant = logParticipantRepository.findByLogAndUser(log, user)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant of this log"));

        // Can't leave if creator
        if (participant.getRole() == LogParticipant.ParticipantRole.CREATOR) {
            throw new IllegalArgumentException("Log creator cannot leave the log. Delete the log instead.");
        }

        // Soft delete
        participant.setActive(false);
        logParticipantRepository.save(participant);

        LogService.log.info("User '{}' left log '{}' (ID: {})", username, log.getName(), log.getId());
    }

    /**
     * Convert a PERSONAL log to SHARED (one-way, creator only)
     */
    @Transactional
    public LogResponse convertToShared(Long logId, String username) {
        User user = getUserByUsername(username);
        Log log = getLogByIdOrThrow(logId);

        if (!isCreator(log, user)) {
            throw new SecurityException("Only the log creator can convert this log");
        }
        if (log.getType() == Log.LogType.SHARED) {
            throw new IllegalStateException("Log is already shared");
        }

        log.setType(Log.LogType.SHARED);
        log = logRepository.save(log);

        LogService.log.info("Converted log '{}' (ID: {}) to SHARED by user '{}'", log.getName(), log.getId(), username);

        return enrichLogResponse(log, user);
    }

    /**
     * Check if user has access to a log
     */
    public boolean hasAccess(Log log, User user) {
        // Public logs are accessible to everyone
        if (log.getIsPublic()) {
            return true;
        }

        // Check if user is creator
        if (log.getCreator().getId().equals(user.getId())) {
            return true;
        }

        // Check if user is an active participant
        return logParticipantRepository.findByLogAndUser(log, user)
                .map(LogParticipant::getActive)
                .orElse(false);
    }

    /**
     * Check if user is the creator of a log
     */
    public boolean isCreator(Log log, User user) {
        return log.getCreator().getId().equals(user.getId());
    }

    /**
     * Check if user can edit the log (creator or station participant)
     */
    public boolean canEdit(Log log, User user) {
        // Check if log is editable
        if (!log.isEditable()) {
            return false;
        }

        // Creator can always edit
        if (isCreator(log, user)) {
            return true;
        }

        // Check participant role
        return logParticipantRepository.findByLogAndUser(log, user)
                .map(participant -> participant.getActive() && participant.canEdit())
                .orElse(false);
    }

    /**
     * Get user's role in a log
     */
    public LogParticipant.ParticipantRole getUserRole(Log log, User user) {
        return logParticipantRepository.findByLogAndUser(log, user)
                .filter(LogParticipant::getActive)
                .map(LogParticipant::getRole)
                .orElse(null);
    }

    /**
     * Enrich LogResponse with user-specific data
     */
    private LogResponse enrichLogResponse(Log log, User user) {
        LogResponse response = LogResponse.fromLog(log);

        // Set user's role
        response.setUserRole(getUserRole(log, user));

        // Set participant count
        long participantCount = logParticipantRepository.findActiveParticipantsByLogId(log.getId())
                .size();
        response.setParticipantCount((int) participantCount);

        // Set QSO count
        long qsoCount = qsoRepository.countByLogId(log.getId());
        response.setQsoCount((int) qsoCount);

        return response;
    }

    /**
     * Maps a LogPurpose to its associated contest code (or null if none)
     */
    private String purposeToContestCode(Log.LogPurpose purpose) {
        return switch (purpose) {
            case FIELD_DAY        -> "ARRL-FD";
            case POTA             -> "POTA";
            case SOTA             -> "SOTA";
            case CQ_WW            -> "CQWW";
            case SWEEPSTAKES      -> "ARRL-SS";
            case WINTER_FIELD_DAY -> "WFD";
            case STATE_QSO_PARTY  -> "STATE-QSO-PARTY";
            default               -> null;
        };
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
                .orElseThrow(() -> new ResourceNotFoundException("Log not found: " + logId));
    }
}
