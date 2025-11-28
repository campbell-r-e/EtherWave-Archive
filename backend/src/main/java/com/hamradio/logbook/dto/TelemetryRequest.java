package com.hamradio.logbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for rig telemetry data from rig control service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryRequest {

    private Long stationId;
    private Long frequencyKhz;
    private String mode;
    private Boolean pttActive;
    private Integer sMeter;
    private Double swr;
}
