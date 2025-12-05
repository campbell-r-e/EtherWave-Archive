package com.hamradio.logbook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for QSO responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QSOResponse {

    private Long id;
    private Long stationId;
    private String stationName;
    private Long operatorId;
    private String operatorCallsign;
    private Long contestId;
    private String contestCode;

    private String callsign;
    private Long frequencyKhz;
    private String mode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate qsoDate;

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

    // Contest-specific data
    private String contestData;

    // QSL info
    private String qslSent;
    private String qslRcvd;
    private String lotwSent;
    private String lotwRcvd;

    // Validation
    private Boolean isValid;
    private String validationErrors;

    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
