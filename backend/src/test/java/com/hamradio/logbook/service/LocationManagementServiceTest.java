package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.StationRepository;
import com.hamradio.logbook.repository.UserRepository;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationManagementService Unit Tests")
class LocationManagementServiceTest {

    @Mock
    private StationRepository stationRepository;
    @Mock
    private UserRepository userRepository;
    @Spy
    private MaidenheadConverter maidenheadConverter;

    @InjectMocks
    private LocationManagementService locationManagementService;

    private Station station;
    private User user;

    @BeforeEach
    void setUp() {
        station = TestDataBuilder.basicStation().build();
        station.setId(1L);
        station.setLatitude(40.7);
        station.setLongitude(-74.0);
        station.setMaidenheadGrid("FN30aa");

        user = TestDataBuilder.basicUser().build();
        user.setId(1L);
        user.setDefaultLatitude(40.7);
        user.setDefaultLongitude(-74.0);
        user.setDefaultGrid("FN30aa");
    }

    // ===== updateStationLocation() =====

    @Test
    @DisplayName("Should update station location with provided grid")
    void shouldUpdateStationLocationWithProvidedGrid() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationManagementService.LocationUpdateResponse response =
                locationManagementService.updateStationLocation(1L, 34.0, -118.2, "DM04rx", "LA Site");

        assertTrue(response.isSuccess());
        assertEquals(34.0, response.getLatitude());
        assertEquals(-118.2, response.getLongitude());
        assertEquals("DM04rx", response.getGrid());
        assertEquals("LA Site", response.getLocationName());
        assertEquals("STATION", response.getSource());
    }

    @Test
    @DisplayName("Should auto-calculate grid when null is provided")
    void shouldAutoCalculateGridWhenNullProvided() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationManagementService.LocationUpdateResponse response =
                locationManagementService.updateStationLocation(1L, 40.7, -74.0, null, null);

        assertTrue(response.isSuccess());
        assertNotNull(response.getGrid());
        assertEquals(6, response.getGrid().length());
        assertTrue(response.getGrid().startsWith("FN30"),
                "Expected FN30xx, got: " + response.getGrid());
    }

    @Test
    @DisplayName("Should auto-calculate grid when blank string provided")
    void shouldAutoCalculateGridWhenBlankProvided() {
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationManagementService.LocationUpdateResponse response =
                locationManagementService.updateStationLocation(1L, 51.5, -0.12, "  ", "London");

        assertTrue(response.isSuccess());
        assertNotNull(response.getGrid());
        assertEquals(6, response.getGrid().length());
    }

    @Test
    @DisplayName("Should throw RuntimeException when station not found")
    void shouldThrowWhenStationNotFound() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                locationManagementService.updateStationLocation(99L, 40.0, -74.0, null, null));
    }

    @Test
    @DisplayName("Should not update locationName when null is passed")
    void shouldNotUpdateLocationNameWhenNull() {
        station.setLocationName("Original Name");
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationManagementService.LocationUpdateResponse response =
                locationManagementService.updateStationLocation(1L, 40.7, -74.0, "FN30aa", null);

        // locationName should remain unchanged (service skips update when null)
        verify(stationRepository).save(argThat(s -> "Original Name".equals(s.getLocationName())));
        assertTrue(response.isSuccess());
    }

    // ===== updateUserLocation() =====

    @Test
    @DisplayName("Should update user default location with provided grid")
    void shouldUpdateUserLocationWithProvidedGrid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationManagementService.LocationUpdateResponse response =
                locationManagementService.updateUserLocation(1L, 34.0, -118.2, "DM04rx");

        assertTrue(response.isSuccess());
        assertEquals(34.0, response.getLatitude());
        assertEquals(-118.2, response.getLongitude());
        assertEquals("DM04rx", response.getGrid());
        assertEquals("USER", response.getSource());
    }

    @Test
    @DisplayName("Should auto-calculate grid for user location when null")
    void shouldAutoCalculateGridForUserWhenNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        LocationManagementService.LocationUpdateResponse response =
                locationManagementService.updateUserLocation(1L, 41.7, -72.7, null);

        assertTrue(response.isSuccess());
        assertNotNull(response.getGrid());
        assertEquals(6, response.getGrid().length());
        assertTrue(response.getGrid().startsWith("FN31"),
                "W1AW area expected FN31xx, got: " + response.getGrid());
    }

    @Test
    @DisplayName("Should throw RuntimeException when user not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                locationManagementService.updateUserLocation(99L, 40.0, -74.0, null));
    }

    // ===== getStationLocation() =====

    @Test
    @DisplayName("Should return LocationInfo for station with coordinates")
    void shouldReturnLocationInfoForStation() {
        station.setLatitude(40.7);
        station.setLongitude(-74.0);
        station.setMaidenheadGrid("FN30aa");
        station.setLocationName("My QTH");
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        LocationManagementService.LocationInfo info =
                locationManagementService.getStationLocation(1L);

        assertNotNull(info);
        assertEquals(40.7, info.getLatitude());
        assertEquals(-74.0, info.getLongitude());
        assertEquals("FN30aa", info.getGrid());
        assertEquals("My QTH", info.getLocationName());
        assertEquals("STATION", info.getSource());
    }

    @Test
    @DisplayName("Should return null when station has no latitude")
    void shouldReturnNullWhenStationHasNoLatitude() {
        station.setLatitude(null);
        station.setLongitude(-74.0);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        assertNull(locationManagementService.getStationLocation(1L));
    }

    @Test
    @DisplayName("Should return null when station has no longitude")
    void shouldReturnNullWhenStationHasNoLongitude() {
        station.setLatitude(40.7);
        station.setLongitude(null);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));

        assertNull(locationManagementService.getStationLocation(1L));
    }

    @Test
    @DisplayName("Should throw when station not found in getStationLocation")
    void shouldThrowWhenStationNotFoundInGet() {
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                locationManagementService.getStationLocation(99L));
    }

    // ===== getUserLocation() =====

    @Test
    @DisplayName("Should return LocationInfo for user with coordinates")
    void shouldReturnLocationInfoForUser() {
        user.setDefaultLatitude(40.7);
        user.setDefaultLongitude(-74.0);
        user.setDefaultGrid("FN30aa");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LocationManagementService.LocationInfo info =
                locationManagementService.getUserLocation(1L);

        assertNotNull(info);
        assertEquals(40.7, info.getLatitude());
        assertEquals(-74.0, info.getLongitude());
        assertEquals("FN30aa", info.getGrid());
        assertEquals("USER", info.getSource());
    }

    @Test
    @DisplayName("Should return null when user has no default latitude")
    void shouldReturnNullWhenUserHasNoLatitude() {
        user.setDefaultLatitude(null);
        user.setDefaultLongitude(-74.0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertNull(locationManagementService.getUserLocation(1L));
    }

    @Test
    @DisplayName("Should return null when user has no default longitude")
    void shouldReturnNullWhenUserHasNoLongitude() {
        user.setDefaultLatitude(40.7);
        user.setDefaultLongitude(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertNull(locationManagementService.getUserLocation(1L));
    }

    @Test
    @DisplayName("Should throw when user not found in getUserLocation")
    void shouldThrowWhenUserNotFoundInGet() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                locationManagementService.getUserLocation(99L));
    }
}
