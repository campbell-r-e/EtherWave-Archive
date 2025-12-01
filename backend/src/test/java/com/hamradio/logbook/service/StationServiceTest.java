package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StationService Unit Tests")
class StationServiceTest {

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private StationService stationService;

    private Station testStation;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setCallsign("W1TEST");

        testStation = new Station();
        testStation.setId(1L);
        testStation.setStationName("Test Station");
        testStation.setCallsign("W1TEST");
        testStation.setLocation("Test Location");
        testStation.setGridSquare("FN31pr");
        testStation.setAntenna("Dipole");
        testStation.setPowerWatts(100);
        testStation.setRigModel("IC-7300");
        testStation.setRigControlEnabled(false);
    }

    @Test
    @DisplayName("Should create station successfully")
    void shouldCreateStation() {
        when(stationRepository.findByStationName("Test Station")).thenReturn(Optional.empty());
        when(stationRepository.save(any(Station.class))).thenReturn(testStation);

        Station result = stationService.createStation(testStation, testUser);

        assertNotNull(result);
        assertEquals("Test Station", result.getStationName());
        verify(stationRepository).save(testStation);
    }

    @Test
    @DisplayName("Should fail to create station when name already exists")
    void shouldFailToCreateStationWhenNameExists() {
        when(stationRepository.findByStationName("Test Station")).thenReturn(Optional.of(testStation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> stationService.createStation(testStation, testUser)
        );

        assertEquals("Station name already exists", exception.getMessage());
        verify(stationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get station by ID successfully")
    void shouldGetStationById() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        Optional<Station> result = stationService.getStationById(1L);

        assertTrue(result.isPresent());
        assertEquals("Test Station", result.get().getStationName());
        verify(stationRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when station not found by ID")
    void shouldReturnEmptyWhenStationNotFoundById() {
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Station> result = stationService.getStationById(999L);

        assertFalse(result.isPresent());
        verify(stationRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get station by name successfully")
    void shouldGetStationByName() {
        when(stationRepository.findByStationName("Test Station")).thenReturn(Optional.of(testStation));

        Optional<Station> result = stationService.getStationByName("Test Station");

        assertTrue(result.isPresent());
        assertEquals("Test Station", result.get().getStationName());
        verify(stationRepository).findByStationName("Test Station");
    }

    @Test
    @DisplayName("Should return empty when station not found by name")
    void shouldReturnEmptyWhenStationNotFoundByName() {
        when(stationRepository.findByStationName("Nonexistent")).thenReturn(Optional.empty());

        Optional<Station> result = stationService.getStationByName("Nonexistent");

        assertFalse(result.isPresent());
        verify(stationRepository).findByStationName("Nonexistent");
    }

    @Test
    @DisplayName("Should get all stations")
    void shouldGetAllStations() {
        Station station2 = new Station();
        station2.setId(2L);
        station2.setStationName("Station 2");
        station2.setCallsign("W2TEST");

        List<Station> stations = Arrays.asList(testStation, station2);
        when(stationRepository.findAll()).thenReturn(stations);

        List<Station> result = stationService.getAllStations();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(stationRepository).findAll();
    }

    @Test
    @DisplayName("Should get stations by callsign")
    void shouldGetStationsByCallsign() {
        Station station2 = new Station();
        station2.setId(2L);
        station2.setStationName("Station 2");
        station2.setCallsign("W1TEST");

        List<Station> stations = Arrays.asList(testStation, station2);
        when(stationRepository.findByCallsign("W1TEST")).thenReturn(stations);

        List<Station> result = stationService.getStationsByCallsign("W1TEST");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> "W1TEST".equals(s.getCallsign())));
        verify(stationRepository).findByCallsign("W1TEST");
    }

    @Test
    @DisplayName("Should update station successfully")
    void shouldUpdateStation() {
        Station updates = new Station();
        updates.setCallsign("W2UPD");
        updates.setLocation("New Location");
        updates.setGridSquare("FN42aa");
        updates.setAntenna("Yagi");
        updates.setPowerWatts(200);
        updates.setRigModel("IC-9700");
        updates.setRigControlEnabled(true);
        updates.setRigControlHost("localhost");
        updates.setRigControlPort(4532);

        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(stationRepository.save(any(Station.class))).thenReturn(testStation);

        Station result = stationService.updateStation(1L, updates);

        assertNotNull(result);
        verify(stationRepository).save(any(Station.class));
    }

    @Test
    @DisplayName("Should fail to update station when not found")
    void shouldFailToUpdateStationWhenNotFound() {
        Station updates = new Station();
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> stationService.updateStation(999L, updates)
        );

        assertEquals("Station not found", exception.getMessage());
        verify(stationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update station with partial updates")
    void shouldUpdateStationWithPartialUpdates() {
        Station updates = new Station();
        updates.setLocation("Only Location Updated");

        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(stationRepository.save(any(Station.class))).thenReturn(testStation);

        Station result = stationService.updateStation(1L, updates);

        assertNotNull(result);
        verify(stationRepository).save(any(Station.class));
    }

    @Test
    @DisplayName("Should delete station successfully")
    void shouldDeleteStation() {
        doNothing().when(stationRepository).deleteById(1L);

        stationService.deleteStation(1L);

        verify(stationRepository).deleteById(1L);
    }
}
