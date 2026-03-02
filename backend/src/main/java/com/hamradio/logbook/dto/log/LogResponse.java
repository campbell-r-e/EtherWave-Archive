package com.hamradio.logbook.dto.log;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogResponse {

    private Long id;
    private String name;
    private String description;
    private Log.LogType type;
    private Long creatorId;
    private String creatorUsername;
    private Long contestId;
    private String contestName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active;
    private Boolean editable;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Contest bonus activities claimed (JSON map)
    private String bonusMetadata;

    private Log.LogPurpose purpose;

    // User's role in this log (if participant)
    private LogParticipant.ParticipantRole userRole;

    // Participant count
    private Integer participantCount;

    // QSO count
    private Integer qsoCount;

    public static LogResponse fromLog(Log log) {
        return LogResponse.builder()
                .id(log.getId())
                .name(log.getName())
                .description(log.getDescription())
                .type(log.getType())
                .creatorId(log.getCreator().getId())
                .creatorUsername(log.getCreator().getUsername())
                .contestId(log.getContest() != null ? log.getContest().getId() : null)
                .contestName(log.getContest() != null ? log.getContest().getContestName() : null)
                .startDate(log.getStartDate())
                .endDate(log.getEndDate())
                .active(log.getActive())
                .editable(log.isEditable())
                .isPublic(log.getIsPublic())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .bonusMetadata(log.getBonusMetadata())
                .purpose(log.getPurpose() != null ? log.getPurpose() : Log.LogPurpose.GENERAL)
                .build();
    }
}
