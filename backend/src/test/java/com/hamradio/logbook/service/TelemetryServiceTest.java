package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.RigTelemetry;
import com.hamradio.logbook.repository.RigTelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryService Unit Tests")
class TelemetryServiceTest {

    @Mock
    private RigTelemetryRepository rigTelemetryRepository;

    @InjectMocks
    private TelemetryService telemetryService;

    private RigTelemetry testTelemetry;

    @BeforeEach
    void setUp() {
        testTelemetry = new RigTelemetry();
        testTelemetry.setId(1L);
        testTelemetry.setTimestamp(LocalDateTime.now());
        testTelemetry.setFrequencyKhz(14074L);
        testTelemetry.setMode("FT8");
        testTelemetry.setPttActive(false);
        testTelemetry.setSMeter(5);
        testTelemetry.setAlcLevel(3);
        testTelemetry.setSwr(BigDecimal.valueOf(1.5));
    }

    @Test
    @DisplayName("Should save telemetry successfully")
    void shouldSaveTelemetry() {
        when(rigTelemetryRepository.save(any(RigTelemetry.class))).thenReturn(testTelemetry);

        RigTelemetry result = telemetryService.saveTelemetry(testTelemetry);

        assertNotNull(result);
        assertEquals(14074L, result.getFrequencyKhz());
        assertEquals("FT8", result.getMode());
        verify(rigTelemetryRepository).save(testTelemetry);
    }

    @Test
    @DisplayName("Should get telemetry by ID successfully")
    void shouldGetTelemetryById() {
        when(rigTelemetryRepository.findById(1L)).thenReturn(Optional.of(testTelemetry));

        Optional<RigTelemetry> result = telemetryService.getTelemetryById(1L);

        assertTrue(result.isPresent());
        assertEquals(14074L, result.get().getFrequencyKhz());
        assertEquals("FT8", result.get().getMode());
        verify(rigTelemetryRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when telemetry not found by ID")
    void shouldReturnEmptyWhenTelemetryNotFoundById() {
        when(rigTelemetryRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<RigTelemetry> result = telemetryService.getTelemetryById(999L);

        assertFalse(result.isPresent());
        verify(rigTelemetryRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get all telemetry")
    void shouldGetAllTelemetry() {
        RigTelemetry telemetry2 = new RigTelemetry();
        telemetry2.setId(2L);
        telemetry2.setTimestamp(LocalDateTime.now());
        telemetry2.setFrequencyKhz(7074L);
        telemetry2.setMode("FT4");

        List<RigTelemetry> telemetryList = Arrays.asList(testTelemetry, telemetry2);
        when(rigTelemetryRepository.findAll()).thenReturn(telemetryList);

        List<RigTelemetry> result = telemetryService.getAllTelemetry();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(rigTelemetryRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no telemetry exists")
    void shouldReturnEmptyListWhenNoTelemetry() {
        when(rigTelemetryRepository.findAll()).thenReturn(Arrays.asList());

        List<RigTelemetry> result = telemetryService.getAllTelemetry();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(rigTelemetryRepository).findAll();
    }

    @Test
    @DisplayName("Should delete telemetry successfully")
    void shouldDeleteTelemetry() {
        doNothing().when(rigTelemetryRepository).deleteById(1L);

        telemetryService.deleteTelemetry(1L);

        verify(rigTelemetryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should delete old telemetry before given timestamp")
    void shouldDeleteOldTelemetry() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        doNothing().when(rigTelemetryRepository).deleteByTimestampBefore(cutoffTime);

        telemetryService.deleteOldTelemetry(cutoffTime);

        verify(rigTelemetryRepository).deleteByTimestampBefore(cutoffTime);
    }

    @Test
    @DisplayName("Should save telemetry with all fields populated")
    void shouldSaveTelemetryWithAllFields() {
        RigTelemetry fullTelemetry = new RigTelemetry();
        fullTelemetry.setId(3L);
        fullTelemetry.setTimestamp(LocalDateTime.now());
        fullTelemetry.setFrequencyKhz(21074L);
        fullTelemetry.setMode("SSB");
        fullTelemetry.setPttActive(true);
        fullTelemetry.setSMeter(9);
        fullTelemetry.setAlcLevel(5);
        fullTelemetry.setSwr(BigDecimal.valueOf(1.2));

        when(rigTelemetryRepository.save(any(RigTelemetry.class))).thenReturn(fullTelemetry);

        RigTelemetry result = telemetryService.saveTelemetry(fullTelemetry);

        assertNotNull(result);
        assertEquals(21074L, result.getFrequencyKhz());
        assertEquals("SSB", result.getMode());
        assertTrue(result.getPttActive());
        assertEquals(9, result.getSMeter());
        assertEquals(5, result.getAlcLevel());
        assertEquals(BigDecimal.valueOf(1.2), result.getSwr());
        verify(rigTelemetryRepository).save(fullTelemetry);
    }

    @Test
    @DisplayName("Should handle telemetry with minimal fields")
    void shouldHandleTelemetryWithMinimalFields() {
        RigTelemetry minimalTelemetry = new RigTelemetry();
        minimalTelemetry.setId(4L);
        minimalTelemetry.setTimestamp(LocalDateTime.now());
        minimalTelemetry.setFrequencyKhz(14074L);

        when(rigTelemetryRepository.save(any(RigTelemetry.class))).thenReturn(minimalTelemetry);

        RigTelemetry result = telemetryService.saveTelemetry(minimalTelemetry);

        assertNotNull(result);
        assertEquals(14074L, result.getFrequencyKhz());
        verify(rigTelemetryRepository).save(minimalTelemetry);
    }
}
