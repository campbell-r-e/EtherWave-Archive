package com.hamradio.logbook.dto.log;

import com.hamradio.logbook.entity.Log;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {

    @NotBlank(message = "Log name is required")
    @Size(max = 100, message = "Log name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Log type is required")
    private Log.LogType type;

    private Long contestId; // Optional

    private LocalDateTime startDate; // Optional

    private LocalDateTime endDate; // Optional

    private Boolean isPublic = false;

    // Contest bonus activities (JSON map: bonus_key -> count)
    // Example: {"100pct_emergency_power": 1, "youth_participation": 3}
    private String bonusMetadata;

    private Log.LogPurpose purpose = Log.LogPurpose.GENERAL;
}
