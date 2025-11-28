package com.hamradio.logbook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for callsign lookup information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallsignInfo {

    private String callsign;
    private String name;
    private String address;
    private String state;
    private String country;
    private String licenseClass;
    private String gridSquare;
    private String lookupSource;
    private boolean cached;
}
