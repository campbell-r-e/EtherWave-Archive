package com.hamradio.logbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for connecting to the rig control service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RigConnectionRequest {
    private Long stationId;
    private String host;
    private Integer port;
}
