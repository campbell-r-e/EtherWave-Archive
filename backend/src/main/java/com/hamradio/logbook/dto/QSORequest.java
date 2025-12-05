package com.hamradio.logbook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating/updating QSO records
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QSORequest {

    @NotNull(message = "Station ID is required")
    private Long stationId;

    private Long operatorId;

    private Long contestId;

    @NotBlank(message = "Callsign is required")
    private String callsign;

    @NotNull(message = "Frequency is required")
    private Long frequencyKhz;

    @NotBlank(message = "Mode is required")
    private String mode;

    @NotNull(message = "QSO date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate qsoDate;

    @NotNull(message = "Time on is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime timeOn;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime timeOff;

    private String rstSent;
    private String rstRcvd;
    private String band;
    private Integer powerWatts;

    // Location data
    private String gridSquare;
    private String county;
    private String state;
    private String country;
    private Integer dxcc;
    private Integer cqZone;
    private Integer ituZone;

    // Operator info
    private String name;
    private String licenseClass;

    // Multi-station contest support
    private Integer stationNumber; // 1-1000, null if unassigned
    private Boolean isGota; // Get On The Air station

    // Contest-specific data (JSON string)
    private String contestData;

    // QSL info
    private String qslSent;
    private String qslRcvd;
    private String lotwSent;
    private String lotwRcvd;

    private String notes;
}
