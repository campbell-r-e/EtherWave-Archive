package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.*;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService Unit Tests")
class ScoringServiceTest {

    @Mock
    private QSORepository qsoRepository;
    @Mock
    private LogRepository logRepository;
    @Mock
    private LogMultiplierRepository logMultiplierRepository;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private DuplicateDetectionService duplicateDetectionService;
    @Mock
    private MultiplierTrackingService multiplierTrackingService;
    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScoringService scoringService;

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
                .mode("SSB")
                .build();
        qso.setId(1L);
        qso.setIsValid(true);
        qso.setIsDuplicate(false);
    }

    // ===== calculateQsoPoints() - invalid / duplicate =====

    @Test
    @DisplayName("Should return 0 for explicitly invalid QSO")
    void shouldReturnZeroForInvalidQso() {
        qso.setIsValid(false);
        assertEquals(0, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Should return 0 for duplicate QSO")
    void shouldReturnZeroForDuplicateQso() {
        qso.setIsDuplicate(true);
        assertEquals(0, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Should score valid non-duplicate QSO normally")
    void shouldScoreValidNonDuplicateQso() {
        qso.setIsValid(true);
        qso.setIsDuplicate(false);
        qso.setMode("SSB");
        assertEquals(1, scoringService.calculateQsoPoints(qso));
    }

    // ===== calculateQsoPoints() - default (no contest) mode scoring =====

    @Test
    @DisplayName("Default scoring: SSB should be 1 point")
    void defaultScoringSsbIs1() {
        qso.setMode("SSB");
        assertEquals(1, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: FM should be 1 point")
    void defaultScoringFmIs1() {
        qso.setMode("FM");
        assertEquals(1, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: AM should be 1 point")
    void defaultScoringAmIs1() {
        qso.setMode("AM");
        assertEquals(1, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: CW should be 2 points")
    void defaultScoringCwIs2() {
        qso.setMode("CW");
        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: FT8 should be 2 points")
    void defaultScoringFt8Is2() {
        qso.setMode("FT8");
        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: FT4 should be 2 points")
    void defaultScoringFt4Is2() {
        qso.setMode("FT4");
        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: RTTY should be 2 points")
    void defaultScoringRttyIs2() {
        qso.setMode("RTTY");
        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: PSK31 should be 2 points")
    void defaultScoringPsk31Is2() {
        qso.setMode("PSK31");
        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Default scoring: JT65 should be 2 points")
    void defaultScoringJt65Is2() {
        qso.setMode("JT65");
        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    // ===== calculateQsoPoints() - contest scoring =====

    @Test
    @DisplayName("Contest scoring: CW uses contest cw value")
    void contestScoringCwUsesContestValue() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"scoring\":{\"cw\":3,\"digital\":2,\"phone\":1}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("CW");

        assertEquals(3, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Contest scoring: FT8 uses contest digital value")
    void contestScoringFt8UsesDigitalValue() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"scoring\":{\"cw\":3,\"digital\":2,\"phone\":1}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("FT8");

        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Contest scoring: SSB uses contest phone value")
    void contestScoringPhoneUsesPhoneValue() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"scoring\":{\"cw\":3,\"digital\":2,\"phone\":1}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("SSB");

        assertEquals(1, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Contest scoring: falls back to default when no scoring config")
    void contestScoringFallsBackToDefaultWhenNoConfig() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("CW");

        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Contest scoring: returns 1 as fallback when mode not in scoring config")
    void contestScoringReturnsFallbackForUnknownMode() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"scoring\":{}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setMode("SSB");

        assertEquals(1, scoringService.calculateQsoPoints(qso));
    }

    @Test
    @DisplayName("Contest scoring: Field Day CW is 2 points per rules")
    void fieldDayCwIs2Points() {
        Contest fieldDay = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"scoring\":{\"cw\":2,\"digital\":2,\"phone\":1},"
                           + "\"duplicate_fields\":[\"band\",\"mode\"],"
                           + "\"duplicate_window\":\"24_hours\"}")
                .build();
        fieldDay.setId(11L);
        qso.setContest(fieldDay);
        qso.setMode("CW");

        assertEquals(2, scoringService.calculateQsoPoints(qso));
    }

    // ===== calculateGotaBonus() =====

    @Test
    @DisplayName("GOTA bonus: empty log returns 0")
    void gotaBonusEmptyLogReturns0() {
        when(qsoRepository.findAllByLogId(1L)).thenReturn(Collections.emptyList());
        assertEquals(0, scoringService.calculateGotaBonus(1L));
    }

    @Test
    @DisplayName("GOTA bonus: no GOTA QSOs returns 0")
    void gotaBonusNoGotaQsosReturns0() {
        qso.setIsGota(false);
        qso.setIsDuplicate(false);
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));
        assertEquals(0, scoringService.calculateGotaBonus(1L));
    }

    @Test
    @DisplayName("GOTA bonus: 1 GOTA QSO = 105 (100 + 5)")
    void gotaBonus1GotaQsoIs105() {
        qso.setIsGota(true);
        qso.setIsDuplicate(false);
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));
        assertEquals(105, scoringService.calculateGotaBonus(1L));
    }

    @Test
    @DisplayName("GOTA bonus: 3 non-dup GOTA QSOs = 115 (100 + 15)")
    void gotaBonus3GotaQsosIs115() {
        QSO g1 = buildGotaQso(1L, false);
        QSO g2 = buildGotaQso(2L, false);
        QSO g3 = buildGotaQso(3L, false);
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(g1, g2, g3));
        assertEquals(115, scoringService.calculateGotaBonus(1L));
    }

    @Test
    @DisplayName("GOTA bonus: duplicate GOTA QSOs not counted")
    void gotaBonusDuplicateGotaQsosNotCounted() {
        QSO g1 = buildGotaQso(1L, false);
        QSO g2 = buildGotaQso(2L, true); // duplicate
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(g1, g2));
        assertEquals(105, scoringService.calculateGotaBonus(1L)); // only 1 non-dup
    }

    @Test
    @DisplayName("GOTA bonus: mixed GOTA and non-GOTA QSOs")
    void gotaBonusMixedQsos() {
        QSO normal = TestDataBuilder.basicQSO(log, station).build();
        normal.setId(1L);
        normal.setIsGota(false);
        normal.setIsDuplicate(false);

        QSO gota = buildGotaQso(2L, false);
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(normal, gota));
        assertEquals(105, scoringService.calculateGotaBonus(1L));
    }

    private QSO buildGotaQso(Long id, boolean isDuplicate) {
        QSO q = TestDataBuilder.basicQSO(log, station).build();
        q.setId(id);
        q.setIsGota(true);
        q.setIsDuplicate(isDuplicate);
        return q;
    }
}
