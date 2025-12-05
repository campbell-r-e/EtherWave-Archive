package com.hamradio.logbook.dto.log;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationAssignmentRequest {

    @Min(value = 1, message = "Station number must be between 1 and 1000")
    @Max(value = 1000, message = "Station number must be between 1 and 1000")
    private Integer stationNumber; // null to unassign

    private Boolean isGota; // defaults to false if null

    // Validation: stationNumber and isGota are mutually exclusive
    public boolean isValid() {
        if (isGota != null && isGota && stationNumber != null) {
            return false; // Cannot be GOTA and have a station number
        }
        return true;
    }
}
