package com.hamradio.rigcontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing current rig status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RigStatus {

    private Long frequencyHz;
    private String mode;
    private String bandwidth;
    private Boolean pttActive;
    private Integer sMeter;
    private Integer powerMeter;
    private Double swr;
    private Boolean connected;
    private LocalDateTime timestamp;
    private String error;
}
