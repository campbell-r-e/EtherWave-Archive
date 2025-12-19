package com.hamradio.logbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for sending commands to the rig control service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RigCommandRequest {
    private String command;
    private Map<String, Object> params;
}
