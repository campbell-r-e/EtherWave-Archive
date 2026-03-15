package com.hamradio.logbook.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a logging station (radio) in a multi-station setup
 */
@Entity
@Table(name = "stations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_name", nullable = false, unique = true, length = 50)
    private String stationName;

    @Column(nullable = false, length = 20)
    private String callsign;

    @Column(length = 100)
    private String location;

    @Column(name = "grid_square", length = 10)
    private String gridSquare;

    // Operator location (for map visualization)
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "maidenhead_grid", length = 8)
    private String maidenheadGrid;

    @Column(name = "location_name", length = 100)
    private String locationName; // e.g., "Field Day Site 2025"

    @Column(length = 200)
    private String antenna;

    @Column(name = "power_watts")
    private Integer powerWatts;

    @Column(name = "rig_model", length = 100)
    private String rigModel;

    @Column(name = "rig_control_enabled")
    private Boolean rigControlEnabled = false;

    @Column(name = "rig_control_host", length = 50)
    private String rigControlHost;

    @Column(name = "rig_control_port")
    private Integer rigControlPort;

    // GOTA (Get On The Air) station designation for Field Day
    @Column(name = "is_gota")
    private Boolean isGota = false;

    // Station number for multi-station contests (1, 2, 3, etc.)
    // Used for station identification and color coding
    @Column(name = "station_number")
    private Integer stationNumber;

    // Station color (hex code) for visualization
    // Auto-assigned based on station number: 1=Blue, 2=Red, GOTA=Green
    @Column(name = "station_color", length = 7)
    private String stationColor;

    // Cloud relay: station connects to the backend via the station gateway
    // rather than the backend connecting directly to the rig service.
    @Column(name = "remote_station")
    private Boolean remoteStation = false;

    // BCrypt hash of the API key used by the remote rig service to authenticate
    // with the station gateway. Only meaningful when remoteStation = true.
    @Column(name = "api_key_hash", length = 100)
    private String apiKeyHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
