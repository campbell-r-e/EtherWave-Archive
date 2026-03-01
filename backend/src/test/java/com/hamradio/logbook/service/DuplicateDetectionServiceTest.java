package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DuplicateDetectionService Unit Tests")
class DuplicateDetectionServiceTest {

    @Mock
    private QSORepository qsoRepository;
    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    private Log log;
    private Station station;
    private QSO qso;

    @BeforeEach
    void setUp() {
        User user = TestDataBuilder.basicUser().build();
        user.setId(1L);

        station = TestDataBuilder.basicStation().build();
        station.setId(1L);

        log = TestDataBuilder.personalLog(user).build();
        log.setId(1L);

        qso = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .mode("SSB")
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(14, 0))
                .build();
        qso.setId(100L);
    }

    // ===== isDuplicate() - no contest (default window) =====

    @Test
    @DisplayName("Should return false when no existing QSOs (no contest)")
    void shouldReturnFalseWhenNoExistingQsos() {
        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertFalse(duplicateDetectionService.isDuplicate(qso));
    }

    @Test
    @DisplayName("Should return true when same callsign/band QSO exists within 24h (no contest)")
    void shouldReturnTrueWhenDuplicateWithin24h() {
        QSO existing = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(15, 0)) // 1 hour later — within 24h window
                .build();
        existing.setId(200L); // different ID

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertTrue(duplicateDetectionService.isDuplicate(qso));
    }

    @Test
    @DisplayName("Should not count self as duplicate")
    void shouldNotCountSelfAsDuplicate() {
        // Same ID as qso — should be filtered out
        QSO self = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(14, 0))
                .build();
        self.setId(100L); // same ID

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(self));

        assertFalse(duplicateDetectionService.isDuplicate(qso));
    }

    @Test
    @DisplayName("Should return false when existing QSO is outside 24h window")
    void shouldReturnFalseWhenOutside24hWindow() {
        QSO existing = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .qsoDate(LocalDate.of(2024, 6, 20)) // 2 days before
                .timeOn(LocalTime.of(14, 0))
                .build();
        existing.setId(200L);

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertFalse(duplicateDetectionService.isDuplicate(qso));
    }

    // ===== isDuplicate() - contest with duplicate_fields band+mode =====

    @Test
    @DisplayName("Contest (band+mode): not a duplicate when no existing QSOs")
    void contestBandModeShouldReturnFalseWhenNoExisting() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"duplicate_fields\":[\"band\",\"mode\"],"
                           + "\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("SSB");

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertFalse(duplicateDetectionService.isDuplicate(qso));
    }

    @Test
    @DisplayName("Contest (band+mode): duplicate when same mode exists within window")
    void contestBandModeIsDuplicateWhenSameModeExists() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"duplicate_fields\":[\"band\",\"mode\"],"
                           + "\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("SSB");

        QSO existing = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .mode("SSB") // same mode
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(15, 0))
                .build();
        existing.setId(200L);

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertTrue(duplicateDetectionService.isDuplicate(qso));
    }

    @Test
    @DisplayName("Contest (band+mode): not a duplicate when different mode")
    void contestBandModeNotDuplicateWhenDifferentMode() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"duplicate_fields\":[\"band\",\"mode\"],"
                           + "\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("CW");

        QSO existing = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .mode("SSB") // different mode
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(15, 0))
                .build();
        existing.setId(200L);

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertFalse(duplicateDetectionService.isDuplicate(qso));
    }

    // ===== isDuplicate() - contest with band only =====

    @Test
    @DisplayName("Contest (band only): duplicate when same band exists within window")
    void contestBandOnlyIsDuplicateWhenSameBandExists() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"duplicate_fields\":[\"band\"],"
                           + "\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(11L);
        qso.setContest(contest);

        QSO existing = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .mode("CW") // different mode, but band check only
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(15, 0))
                .build();
        existing.setId(200L);

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertTrue(duplicateDetectionService.isDuplicate(qso));
    }

    // ===== isDuplicate() - contest with callsign only =====

    @Test
    @DisplayName("Contest (callsign only): duplicate when callsign exists within window")
    void contestCallsignOnlyIsDuplicate() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(12L);
        qso.setContest(contest);

        QSO existing = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("40m") // different band, callsign-only check
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(15, 0))
                .build();
        existing.setId(200L);

        when(qsoRepository.findByLogIdAndCallsignAndDateRange(
                anyLong(), anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertTrue(duplicateDetectionService.isDuplicate(qso));
    }

    @Test
    @DisplayName("Contest (callsign only): not a duplicate when no existing QSOs")
    void contestCallsignOnlyNotDuplicateWhenNoExisting() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(12L);
        qso.setContest(contest);

        when(qsoRepository.findByLogIdAndCallsignAndDateRange(
                anyLong(), anyString(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertFalse(duplicateDetectionService.isDuplicate(qso));
    }

    // ===== isDuplicate() - contest_duration window =====

    @Test
    @DisplayName("Contest duration window: treats entire contest as one window")
    void contestDurationWindowUsedCorrectly() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"duplicate_fields\":[\"band\",\"mode\"],"
                           + "\"duplicate_window\":\"contest_duration\"}")
                .build();
        contest.setId(13L);
        qso.setContest(contest);
        qso.setMode("SSB");

        QSO existing = TestDataBuilder.basicQSO(log, station)
                .callsign("W1AW")
                .band("20m")
                .mode("SSB")
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(15, 0))
                .build();
        existing.setId(200L);

        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(existing));

        assertTrue(duplicateDetectionService.isDuplicate(qso));
    }

    // ===== markDuplicates() =====

    @Test
    @DisplayName("markDuplicates: returns 0 when log is empty")
    void markDuplicatesReturns0ForEmptyLog() {
        when(qsoRepository.findAllByLogId(1L)).thenReturn(new java.util.ArrayList<>());
        when(qsoRepository.saveAll(any())).thenReturn(Collections.emptyList());

        int count = duplicateDetectionService.markDuplicates(1L);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("markDuplicates: single QSO is never a duplicate")
    void markDuplicatesSingleQsoIsNeverDuplicate() {
        when(qsoRepository.findAllByLogId(1L)).thenReturn(new java.util.ArrayList<>(List.of(qso)));
        when(qsoRepository.findByLogIdAndCallsignAndBandAndDateRange(
                anyLong(), anyString(), anyString(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(qsoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = duplicateDetectionService.markDuplicates(1L);
        assertEquals(0, count);
        assertFalse(qso.getIsDuplicate());
    }
}
