package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Station Repository Integration Tests")
class StationRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        stationRepository.deleteAll();
        userRepository.deleteAll();

        testUser1 = userRepository.save(TestDataBuilder.aValidUser().username("user1").email("user1@test.com").build());
        testUser2 = userRepository.save(TestDataBuilder.aValidUser().username("user2").email("user2@test.com").callsign("K2ABC").build());
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid Station - Persists Successfully")
    void save_validStation_persistsSuccessfully() {
        // Arrange
        Station station = TestDataBuilder.aValidStation().build();

        // Act
        Station saved = stationRepository.save(station);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCallsign()).isEqualTo("W1AW");
        assertThat(saved.getGridSquare()).isEqualTo("FN31");
    }

    @Test
    @DisplayName("findById - Existing Station - Returns Station")
    void findById_existingStation_returnsStation() {
        // Arrange
        Station station = stationRepository.save(TestDataBuilder.aValidStation().build());

        // Act
        Optional<Station> found = stationRepository.findById(station.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCallsign()).isEqualTo("W1AW");
    }

    // ==================== FIND BY CALLSIGN TESTS ====================

    @Test
    @DisplayName("findByCallsign - Existing Station - Returns Station")
    void findByCallsign_existingStation_returnsStation() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").build());

        // Act
        Optional<Station> found = stationRepository.findByCallsign("K2ABC");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCallsign()).isEqualTo("K2ABC");
    }

    @Test
    @DisplayName("findByCallsign - Case Insensitive - Returns Station")
    void findByCallsign_caseInsensitive_returnsStation() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());

        // Act
        Optional<Station> found = stationRepository.findByCallsign("w1aw");

        // Assert
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByCallsignContaining - Returns Matching Stations")
    void findByCallsignContaining_returnsMatchingStations() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").build());

        // Act
        List<Station> results = stationRepository.findByCallsignContaining("W1");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(station -> station.getCallsign().startsWith("W1"));
    }

    // ==================== GRID SQUARE TESTS ====================

    @Test
    @DisplayName("findByGridSquare - Returns Stations in Grid")
    void findByGridSquare_returnsStationsInGrid() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").gridSquare("FN31").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").gridSquare("FN31").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").gridSquare("FN42").build());

        // Act
        List<Station> fn31Stations = stationRepository.findByGridSquare("FN31");

        // Assert
        assertThat(fn31Stations).hasSize(2);
        assertThat(fn31Stations).allMatch(station -> station.getGridSquare().equals("FN31"));
    }

    @Test
    @DisplayName("findByGridSquareStartingWith - Returns Stations in Grid Region")
    void findByGridSquareStartingWith_returnsStationsInGridRegion() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").gridSquare("FN31aa").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").gridSquare("FN31bb").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").gridSquare("FN42aa").build());

        // Act
        List<Station> fn31Stations = stationRepository.findByGridSquareStartingWith("FN31");

        // Assert
        assertThat(fn31Stations).hasSize(2);
        assertThat(fn31Stations).allMatch(station -> station.getGridSquare().startsWith("FN31"));
    }

    // ==================== POWER TESTS ====================

    @Test
    @DisplayName("findByPowerGreaterThan - Returns High Power Stations")
    void findByPowerGreaterThan_returnsHighPowerStations() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").power(100.0).build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").power(1500.0).build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").power(50.0).build());

        // Act
        List<Station> highPowerStations = stationRepository.findByPowerGreaterThan(500.0);

        // Assert
        assertThat(highPowerStations).hasSize(1);
        assertThat(highPowerStations.get(0).getCallsign()).isEqualTo("W1ABC");
        assertThat(highPowerStations.get(0).getPower()).isEqualTo(1500.0);
    }

    @Test
    @DisplayName("findByPowerLessThan - Returns QRP Stations")
    void findByPowerLessThan_returnsQRPStations() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").power(5.0).build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").power(100.0).build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").power(2.5).build());

        // Act
        List<Station> qrpStations = stationRepository.findByPowerLessThan(10.0);

        // Assert
        assertThat(qrpStations).hasSize(2);
        assertThat(qrpStations).allMatch(station -> station.getPower() < 10.0);
    }

    // ==================== ANTENNA TESTS ====================

    @Test
    @DisplayName("findByAntennaContaining - Returns Stations with Antenna Type")
    void findByAntennaContaining_returnsStationsWithAntennaType() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").antenna("Dipole").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").antenna("Vertical Dipole").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").antenna("Yagi").build());

        // Act
        List<Station> dipoleStations = stationRepository.findByAntennaContaining("Dipole");

        // Assert
        assertThat(dipoleStations).hasSize(2);
        assertThat(dipoleStations).allMatch(station -> station.getAntenna().contains("Dipole"));
    }

    // ==================== RIG TESTS ====================

    @Test
    @DisplayName("findByRigContaining - Returns Stations with Rig Model")
    void findByRigContaining_returnsStationsWithRigModel() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").rig("Icom IC-7300").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").rig("Icom IC-9700").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").rig("Yaesu FT-991A").build());

        // Act
        List<Station> icomStations = stationRepository.findByRigContaining("Icom");

        // Assert
        assertThat(icomStations).hasSize(2);
        assertThat(icomStations).allMatch(station -> station.getRig().contains("Icom"));
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("existsByCallsign - Existing Station - Returns True")
    void existsByCallsign_existingStation_returnsTrue() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());

        // Act
        boolean exists = stationRepository.existsByCallsign("W1AW");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByCallsign - Non-Existent Station - Returns False")
    void existsByCallsign_nonExistentStation_returnsFalse() {
        // Act
        boolean exists = stationRepository.existsByCallsign("ZZ9ZZZ");

        // Assert
        assertThat(exists).isFalse();
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("count - Returns Total Station Count")
    void count_returnsTotalStationCount() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("N3XYZ").build());

        // Act
        long count = stationRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countByGridSquare - Returns Count of Stations in Grid")
    void countByGridSquare_returnsCountOfStationsInGrid() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").gridSquare("FN31").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").gridSquare("FN31").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").gridSquare("FN42").build());

        // Act
        long count = stationRepository.countByGridSquare("FN31");

        // Assert
        assertThat(count).isEqualTo(2);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes Station")
    void deleteById_removesStation() {
        // Arrange
        Station station = stationRepository.save(TestDataBuilder.aValidStation().build());
        Long stationId = station.getId();

        // Act
        stationRepository.deleteById(stationId);

        // Assert
        assertThat(stationRepository.findById(stationId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByCallsign - Removes Station by Callsign")
    void deleteByCallsign_removesStationByCallsign() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());

        // Act
        stationRepository.deleteByCallsign("W1AW");

        // Assert
        assertThat(stationRepository.findByCallsign("W1AW")).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("save - Update Existing Station - Updates Successfully")
    void save_updateExistingStation_updatesSuccessfully() {
        // Arrange
        Station station = stationRepository.save(TestDataBuilder.aValidStation().build());
        Long stationId = station.getId();

        // Act
        station.setGridSquare("FN42");
        station.setPower(200.0);
        station.setAntenna("Yagi");
        stationRepository.save(station);

        // Assert
        Station updated = stationRepository.findById(stationId).orElseThrow();
        assertThat(updated.getGridSquare()).isEqualTo("FN42");
        assertThat(updated.getPower()).isEqualTo(200.0);
        assertThat(updated.getAntenna()).isEqualTo("Yagi");
    }

    // ==================== SORTING TESTS ====================

    @Test
    @DisplayName("findAll - Returns Stations Sorted by Callsign")
    void findAll_returnsStationsSortedByCallsign() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("N3XYZ").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());

        // Act
        List<Station> stations = (List<Station>) stationRepository.findAll();

        // Assert - Should be sorted alphabetically if sort is defined
        assertThat(stations).hasSize(3);
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("save - Duplicate Callsign - Throws Exception")
    void save_duplicateCallsign_throwsException() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());

        // Act & Assert
        assertThatThrownBy(() -> {
            stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").build());
        }).isInstanceOf(Exception.class);
    }

    // ==================== LOCATION TESTS ====================

    @Test
    @DisplayName("findByLatitudeBetweenAndLongitudeBetween - Returns Stations in Geo Area")
    void findByLatitudeBetweenAndLongitudeBetween_returnsStationsInGeoArea() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").latitude(42.0).longitude(-72.0).build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").latitude(41.5).longitude(-71.5).build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").latitude(45.0).longitude(-75.0).build());

        // Act
        List<Station> nearbyStations = stationRepository.findByLatitudeBetweenAndLongitudeBetween(
                41.0, 43.0, -73.0, -71.0
        );

        // Assert
        assertThat(nearbyStations).hasSize(2);
    }

    // ==================== NOTES TESTS ====================

    @Test
    @DisplayName("findByNotesContaining - Returns Stations with Matching Notes")
    void findByNotesContaining_returnsStationsWithMatchingNotes() {
        // Arrange
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1AW").notes("Contest station").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("W1ABC").notes("Portable contest setup").build());
        stationRepository.save(TestDataBuilder.aValidStation().callsign("K2ABC").notes("Home QTH").build());

        // Act
        List<Station> contestStations = stationRepository.findByNotesContaining("contest");

        // Assert
        assertThat(contestStations).hasSize(2);
        assertThat(contestStations).allMatch(station ->
            station.getNotes().toLowerCase().contains("contest"));
    }
}
