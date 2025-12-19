package com.hamradio.logbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for rig control commands
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RigCommandResponse {
    private Boolean success;
    private String message;
    private Map<String, Object> result;
}
