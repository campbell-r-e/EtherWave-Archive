package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.RigTelemetry;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Rig Telemetry Repository Integration Tests")
class RigTelemetryRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private RigTelemetryRepository rigTelemetryRepository;

    @Autowired
    private StationRepository stationRepository;

    private Station testStation1;
    private Station testStation2;

    @BeforeEach
    void setUp() {
        rigTelemetryRepository.deleteAll();
        stationRepository.deleteAll();

        testStation1 = stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());
        testStation2 = stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").build());
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid Telemetry - Persists Successfully")
    void save_validTelemetry_persistsSuccessfully() {
        // Arrange
        RigTelemetry telemetry = RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .power(100.0)
                .vfo("VFOA")
                .recordedAt(LocalDateTime.now())
                .build();

        // Act
        RigTelemetry saved = rigTelemetryRepository.save(telemetry);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStation()).isEqualTo(testStation1);
        assertThat(saved.getFrequencyHz()).isEqualTo(14250000L);
        assertThat(saved.getMode()).isEqualTo("USB");
    }

    @Test
    @DisplayName("findById - Existing Telemetry - Returns Telemetry")
    void findById_existingTelemetry_returnsTelemetry() {
        // Arrange
        RigTelemetry telemetry = rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        Optional<RigTelemetry> found = rigTelemetryRepository.findById(telemetry.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFrequencyHz()).isEqualTo(14250000L);
    }

    // ==================== FIND BY STATION TESTS ====================

    @Test
    @DisplayName("findByStationId - Returns Telemetry for Station")
    void findByStationId_returnsTelemetryForStation() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(now.minusMinutes(10))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(now.minusMinutes(5))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation2)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(now)
                .build());

        // Act
        List<RigTelemetry> telemetry = rigTelemetryRepository.findByStationId(testStation1.getId());

        // Assert
        assertThat(telemetry).hasSize(2);
        assertThat(telemetry).allMatch(t -> t.getStation().equals(testStation1));
    }

    @Test
    @DisplayName("findByStationIdOrderByRecordedAtDesc - Returns Latest First")
    void findByStationIdOrderByRecordedAtDesc_returnsLatestFirst() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(now.minusMinutes(10))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(now)
                .build());

        // Act
        List<RigTelemetry> telemetry = rigTelemetryRepository.findByStationIdOrderByRecordedAtDesc(testStation1.getId());

        // Assert
        assertThat(telemetry).hasSize(2);
        assertThat(telemetry.get(0).getMode()).isEqualTo("LSB"); // Most recent
        assertThat(telemetry.get(1).getMode()).isEqualTo("USB"); // Older
    }

    // ==================== FIND LATEST TESTS ====================

    @Test
    @DisplayName("findFirstByStationIdOrderByRecordedAtDesc - Returns Latest Telemetry")
    void findFirstByStationIdOrderByRecordedAtDesc_returnsLatestTelemetry() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(now.minusMinutes(10))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(now)
                .build());

        // Act
        Optional<RigTelemetry> latest = rigTelemetryRepository.findFirstByStationIdOrderByRecordedAtDesc(testStation1.getId());

        // Assert
        assertThat(latest).isPresent();
        assertThat(latest.get().getMode()).isEqualTo("LSB");
        assertThat(latest.get().getFrequencyHz()).isEqualTo(7125000L);
    }

    @Test
    @DisplayName("findFirstByStationIdOrderByRecordedAtDesc - No Telemetry - Returns Empty")
    void findFirstByStationIdOrderByRecordedAtDesc_noTelemetry_returnsEmpty() {
        // Act
        Optional<RigTelemetry> latest = rigTelemetryRepository.findFirstByStationIdOrderByRecordedAtDesc(testStation1.getId());

        // Assert
        assertThat(latest).isEmpty();
    }

    // ==================== DATE RANGE TESTS ====================

    @Test
    @DisplayName("findByStationIdAndRecordedAtBetween - Returns Telemetry in Range")
    void findByStationIdAndRecordedAtBetween_returnsTelemetryInRange() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(now.minusDays(10))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(now.minusDays(5))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(21200000L)
                .mode("USB")
                .recordedAt(now.minusDays(1))
                .build());

        // Act
        List<RigTelemetry> telemetry = rigTelemetryRepository.findByStationIdAndRecordedAtBetween(
                testStation1.getId(),
                now.minusDays(7),
                now
        );

        // Assert
        assertThat(telemetry).hasSize(2);
    }

    @Test
    @DisplayName("findByRecordedAtBefore - Returns Old Telemetry")
    void findByRecordedAtBefore_returnsOldTelemetry() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(now.minusDays(10))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(now.minusHours(1))
                .build());

        // Act
        List<RigTelemetry> oldTelemetry = rigTelemetryRepository.findByRecordedAtBefore(now.minusDays(7));

        // Assert
        assertThat(oldTelemetry).hasSize(1);
        assertThat(oldTelemetry.get(0).getFrequencyHz()).isEqualTo(14250000L);
    }

    // ==================== FREQUENCY TESTS ====================

    @Test
    @DisplayName("findByFrequencyHzBetween - Returns Telemetry in Frequency Range")
    void findByFrequencyHzBetween_returnsTelemetryInFrequencyRange() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)  // 40m
                .mode("LSB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)  // 20m
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(21200000L)  // 15m
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act - Find 20m band telemetry (14.0 - 14.35 MHz)
        List<RigTelemetry> band20m = rigTelemetryRepository.findByFrequencyHzBetween(14000000L, 14350000L);

        // Assert
        assertThat(band20m).hasSize(1);
        assertThat(band20m.get(0).getFrequencyHz()).isEqualTo(14250000L);
    }

    // ==================== MODE TESTS ====================

    @Test
    @DisplayName("findByMode - Returns Telemetry by Mode")
    void findByMode_returnsTelemetryByMode() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14074000L)
                .mode("FT8")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        List<RigTelemetry> usbTelemetry = rigTelemetryRepository.findByMode("USB");
        List<RigTelemetry> ft8Telemetry = rigTelemetryRepository.findByMode("FT8");

        // Assert
        assertThat(usbTelemetry).hasSize(1);
        assertThat(ft8Telemetry).hasSize(1);
    }

    @Test
    @DisplayName("findByStationIdAndMode - Returns Station's Telemetry by Mode")
    void findByStationIdAndMode_returnsStationsTelemetryByMode() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14074000L)
                .mode("FT8")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation2)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        List<RigTelemetry> station1Usb = rigTelemetryRepository.findByStationIdAndMode(testStation1.getId(), "USB");

        // Assert
        assertThat(station1Usb).hasSize(1);
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("countByStationId - Returns Count of Telemetry for Station")
    void countByStationId_returnsCountOfTelemetryForStation() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation2)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        long count = rigTelemetryRepository.countByStationId(testStation1.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByMode - Returns Count by Mode")
    void countByMode_returnsCountByMode() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14074000L)
                .mode("FT8")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        long usbCount = rigTelemetryRepository.countByMode("USB");
        long ft8Count = rigTelemetryRepository.countByMode("FT8");

        // Assert
        assertThat(usbCount).isEqualTo(2);
        assertThat(ft8Count).isEqualTo(1);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes Telemetry")
    void deleteById_removesTelemetry() {
        // Arrange
        RigTelemetry telemetry = rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        Long telemetryId = telemetry.getId();

        // Act
        rigTelemetryRepository.deleteById(telemetryId);

        // Assert
        assertThat(rigTelemetryRepository.findById(telemetryId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByStationId - Removes All Telemetry for Station")
    void deleteByStationId_removesAllTelemetryForStation() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation2)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        rigTelemetryRepository.deleteByStationId(testStation1.getId());

        // Assert
        assertThat(rigTelemetryRepository.countByStationId(testStation1.getId())).isZero();
        assertThat(rigTelemetryRepository.countByStationId(testStation2.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteByRecordedAtBefore - Removes Old Telemetry")
    void deleteByRecordedAtBefore_removesOldTelemetry() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(now.minusDays(10))
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .recordedAt(now.minusHours(1))
                .build());

        // Act
        rigTelemetryRepository.deleteByRecordedAtBefore(now.minusDays(7));

        // Assert
        assertThat(rigTelemetryRepository.count()).isEqualTo(1);
    }

    // ==================== POWER TESTS ====================

    @Test
    @DisplayName("findByPowerGreaterThan - Returns High Power Telemetry")
    void findByPowerGreaterThan_returnsHighPowerTelemetry() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .power(100.0)
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .power(5.0)
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        List<RigTelemetry> highPower = rigTelemetryRepository.findByPowerGreaterThan(50.0);

        // Assert
        assertThat(highPower).hasSize(1);
        assertThat(highPower.get(0).getPower()).isEqualTo(100.0);
    }

    // ==================== VFO TESTS ====================

    @Test
    @DisplayName("findByVfo - Returns Telemetry by VFO")
    void findByVfo_returnsTelemetryByVfo() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .vfo("VFOA")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(7125000L)
                .mode("LSB")
                .vfo("VFOB")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        List<RigTelemetry> vfoA = rigTelemetryRepository.findByVfo("VFOA");

        // Assert
        assertThat(vfoA).hasSize(1);
        assertThat(vfoA.get(0).getVfo()).isEqualTo("VFOA");
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    @DisplayName("findDistinctModesByStationId - Returns Unique Modes for Station")
    void findDistinctModesByStationId_returnsUniqueModesForStation() {
        // Arrange
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14250000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14251000L)
                .mode("USB")
                .recordedAt(LocalDateTime.now())
                .build());
        rigTelemetryRepository.save(RigTelemetry.builder()
                .station(testStation1)
                .frequencyHz(14074000L)
                .mode("FT8")
                .recordedAt(LocalDateTime.now())
                .build());

        // Act
        List<String> modes = rigTelemetryRepository.findDistinctModesByStationId(testStation1.getId());

        // Assert
        assertThat(modes).hasSize(2);
        assertThat(modes).contains("USB", "FT8");
    }
}
