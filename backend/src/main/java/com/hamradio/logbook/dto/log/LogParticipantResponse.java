package com.hamradio.logbook.dto.log;

import com.hamradio.logbook.entity.LogParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogParticipantResponse {

    private Long id;
    private Long logId;
    private String logName;
    private Long userId;
    private String username;
    private String userCallsign;
    private LogParticipant.ParticipantRole role;
    private String stationCallsign;
    private Boolean active;
    private LocalDateTime joinedAt;

    public static LogParticipantResponse fromLogParticipant(LogParticipant participant) {
        return LogParticipantResponse.builder()
                .id(participant.getId())
                .logId(participant.getLog().getId())
                .logName(participant.getLog().getName())
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .userCallsign(participant.getUser().getCallsign())
                .role(participant.getRole())
                .stationCallsign(participant.getStationCallsign())
                .active(participant.getActive())
                .joinedAt(participant.getJoinedAt())
                .build();
    }
}
