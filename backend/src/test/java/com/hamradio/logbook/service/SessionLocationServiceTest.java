package com.hamradio.logbook.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionLocationService Unit Tests")
class SessionLocationServiceTest {

    @Spy
    private MaidenheadConverter maidenheadConverter;

    @InjectMocks
    private SessionLocationService sessionLocationService;

    @BeforeEach
    void setUp() {
        sessionLocationService.clearAllSessionLocations();
    }

    // ===== setSessionLocation() =====

    @Test
    @DisplayName("Should store location when grid is provided")
    void shouldStoreLocationWithProvidedGrid() {
        SessionLocationService.SessionLocationResponse response =
                sessionLocationService.setSessionLocation(1L, 40.7, -74.0, "FN30ab");

        assertTrue(response.isSuccess());
        assertEquals(1L, response.getLogId());
        assertEquals(40.7, response.getLatitude());
        assertEquals(-74.0, response.getLongitude());
        assertEquals("FN30ab", response.getGrid());
    }

    @Test
    @DisplayName("Should auto-calculate grid when not provided (null)")
    void shouldAutoCalculateGridWhenNull() {
        SessionLocationService.SessionLocationResponse response =
                sessionLocationService.setSessionLocation(1L, 40.7, -74.0, null);

        assertTrue(response.isSuccess());
        assertNotNull(response.getGrid());
        assertEquals(6, response.getGrid().length());
        assertTrue(response.getGrid().startsWith("FN30"),
                "Expected FN30xx, got: " + response.getGrid());
    }

    @Test
    @DisplayName("Should auto-calculate grid when provided as blank")
    void shouldAutoCalculateGridWhenBlank() {
        SessionLocationService.SessionLocationResponse response =
                sessionLocationService.setSessionLocation(1L, 51.5, -0.12, "  ");

        assertTrue(response.isSuccess());
        assertNotNull(response.getGrid());
        assertEquals(6, response.getGrid().length());
    }

    @Test
    @DisplayName("Should overwrite existing location for same log")
    void shouldOverwriteExistingLocation() {
        sessionLocationService.setSessionLocation(1L, 40.7, -74.0, "FN30ab");
        sessionLocationService.setSessionLocation(1L, 34.0, -118.2, "DM04rx");

        SessionLocationService.SessionLocation loc = sessionLocationService.getSessionLocation(1L);
        assertNotNull(loc);
        assertEquals(34.0, loc.getLatitude());
        assertEquals(-118.2, loc.getLongitude());
        assertEquals("DM04rx", loc.getGrid());
    }

    @Test
    @DisplayName("Should store locations for different logs independently")
    void shouldStoreLocationsForDifferentLogsIndependently() {
        sessionLocationService.setSessionLocation(1L, 40.7, -74.0, "FN30ab");
        sessionLocationService.setSessionLocation(2L, 34.0, -118.2, "DM04rx");

        assertEquals("FN30ab", sessionLocationService.getSessionLocation(1L).getGrid());
        assertEquals("DM04rx", sessionLocationService.getSessionLocation(2L).getGrid());
    }

    // ===== getSessionLocation() =====

    @Test
    @DisplayName("Should return null when no location set for log")
    void shouldReturnNullWhenNoLocationSet() {
        assertNull(sessionLocationService.getSessionLocation(99L));
    }

    @Test
    @DisplayName("Should return stored location for log")
    void shouldReturnStoredLocation() {
        sessionLocationService.setSessionLocation(5L, 51.5, -0.12, "IO91wm");

        SessionLocationService.SessionLocation loc = sessionLocationService.getSessionLocation(5L);
        assertNotNull(loc);
        assertEquals(5L, loc.getLogId());
        assertEquals(51.5, loc.getLatitude());
        assertEquals(-0.12, loc.getLongitude());
        assertEquals("IO91wm", loc.getGrid());
    }

    @Test
    @DisplayName("Should return null for expired location (over 1 hour)")
    void shouldReturnNullForExpiredLocation() throws Exception {
        sessionLocationService.setSessionLocation(7L, 40.0, -74.0, "FN20aa");

        // Manipulate timestamp to simulate expiry
        setTimestampOld(7L, 3700000); // > 1 hour

        assertNull(sessionLocationService.getSessionLocation(7L));
    }

    // ===== clearSessionLocation() =====

    @Test
    @DisplayName("Should clear session location for specific log")
    void shouldClearSessionLocationForLog() {
        sessionLocationService.setSessionLocation(1L, 40.7, -74.0, "FN30ab");
        sessionLocationService.setSessionLocation(2L, 34.0, -118.2, "DM04rx");

        sessionLocationService.clearSessionLocation(1L);

        assertNull(sessionLocationService.getSessionLocation(1L));
        assertNotNull(sessionLocationService.getSessionLocation(2L));
    }

    @Test
    @DisplayName("Should not throw when clearing non-existent location")
    void shouldNotThrowWhenClearingNonExistentLocation() {
        assertDoesNotThrow(() -> sessionLocationService.clearSessionLocation(999L));
    }

    // ===== clearAllSessionLocations() =====

    @Test
    @DisplayName("Should clear all session locations")
    void shouldClearAllSessionLocations() {
        sessionLocationService.setSessionLocation(1L, 40.7, -74.0, "FN30ab");
        sessionLocationService.setSessionLocation(2L, 34.0, -118.2, "DM04rx");
        sessionLocationService.setSessionLocation(3L, 51.5, -0.12, "IO91wm");

        sessionLocationService.clearAllSessionLocations();

        assertNull(sessionLocationService.getSessionLocation(1L));
        assertNull(sessionLocationService.getSessionLocation(2L));
        assertNull(sessionLocationService.getSessionLocation(3L));
    }

    // ===== getActiveSessionCount() =====

    @Test
    @DisplayName("Should return 0 when no sessions active")
    void shouldReturn0WhenNoSessions() {
        assertEquals(0, sessionLocationService.getActiveSessionCount());
    }

    @Test
    @DisplayName("Should return correct count for active sessions")
    void shouldReturnCorrectCountForActiveSessions() {
        sessionLocationService.setSessionLocation(1L, 40.7, -74.0, "FN30ab");
        sessionLocationService.setSessionLocation(2L, 34.0, -118.2, "DM04rx");

        assertEquals(2, sessionLocationService.getActiveSessionCount());
    }

    @Test
    @DisplayName("Should not count expired sessions in active count")
    void shouldNotCountExpiredSessions() throws Exception {
        sessionLocationService.setSessionLocation(1L, 40.7, -74.0, "FN30ab");
        sessionLocationService.setSessionLocation(2L, 34.0, -118.2, "DM04rx");

        setTimestampOld(1L, 3700000); // expire log 1

        assertEquals(1, sessionLocationService.getActiveSessionCount());
    }

    // ===== Helper =====

    @SuppressWarnings("unchecked")
    private void setTimestampOld(Long logId, long ageMs) throws Exception {
        Field field = SessionLocationService.class.getDeclaredField("sessionLocations");
        field.setAccessible(true);
        Map<Long, SessionLocationService.SessionLocation> map =
                (Map<Long, SessionLocationService.SessionLocation>) field.get(sessionLocationService);

        SessionLocationService.SessionLocation loc = map.get(logId);
        if (loc != null) {
            Field tsField = SessionLocationService.SessionLocation.class.getDeclaredField("timestamp");
            tsField.setAccessible(true);
            tsField.set(loc, System.currentTimeMillis() - ageMs);
        }
    }
}
