package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.LogMultiplierRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiplierTrackingService Unit Tests")
class MultiplierTrackingServiceTest {

    @Mock
    private LogMultiplierRepository logMultiplierRepository;
    @Mock
    private QSORepository qsoRepository;
    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private MultiplierTrackingService multiplierTrackingService;

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

        qso = TestDataBuilder.basicQSO(log, station).build();
        qso.setId(10L);
        qso.setIsDuplicate(false);
    }

    // ===== isNewMultiplier() - no contest (state) =====

    @Test
    @DisplayName("No contest: null state → not a new multiplier")
    void noContestNullStateIsNotNewMultiplier() {
        qso.setState(null);
        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
        verifyNoInteractions(logMultiplierRepository);
    }

    @Test
    @DisplayName("No contest: empty state → not a new multiplier")
    void noContestEmptyStateIsNotNewMultiplier() {
        qso.setState("");
        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
        verifyNoInteractions(logMultiplierRepository);
    }

    @Test
    @DisplayName("No contest: new state not in repo → is a new multiplier")
    void noContestNewStateIsNewMultiplier() {
        qso.setState("NY");
        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "STATE", "NY", null))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("No contest: state already in repo → not a new multiplier")
    void noContestExistingStateIsNotNewMultiplier() {
        qso.setState("MA");
        LogMultiplier existing = LogMultiplier.builder()
                .multiplierType("STATE")
                .multiplierValue("MA")
                .build();
        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "STATE", "MA", null))
                .thenReturn(Optional.of(existing));

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    // ===== isNewMultiplier() - contest ARRL_SECTION =====

    @Test
    @DisplayName("Contest ARRL_SECTION: null contestData → not a new multiplier")
    void arrlSectionNullContestDataIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"arrl_section\",\"per_band\":false}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setContestData(null);

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest ARRL_SECTION: no section in contestData → not a new multiplier")
    void arrlSectionMissingSectionIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"arrl_section\",\"per_band\":false}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setContestData("{\"class\":\"2A\"}"); // no section key

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest ARRL_SECTION: new section not in repo → is a new multiplier")
    void arrlSectionNewSectionIsNewMultiplier() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"arrl_section\",\"per_band\":false}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setContestData("{\"class\":\"2A\",\"section\":\"ORG\"}");

        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "ARRL_SECT", "ORG", null))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest ARRL_SECTION: section already worked → not a new multiplier")
    void arrlSectionExistingSectionIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"arrl_section\",\"per_band\":false}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setContestData("{\"class\":\"2A\",\"section\":\"NE\"}");

        LogMultiplier existing = LogMultiplier.builder()
                .multiplierType("ARRL_SECT")
                .multiplierValue("NE")
                .build();
        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "ARRL_SECT", "NE", null))
                .thenReturn(Optional.of(existing));

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest ARRL_SECTION per-band: uses band in lookup")
    void arrlSectionPerBandUsesCorrectBand() {
        Contest contest = TestDataBuilder.fieldDayContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"arrl_section\",\"per_band\":true}}")
                .build();
        contest.setId(10L);
        qso.setContest(contest);
        qso.setBand("20m");
        qso.setContestData("{\"class\":\"2A\",\"section\":\"WMA\"}");

        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "ARRL_SECT", "WMA", "20m"))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
        verify(logMultiplierRepository).findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "ARRL_SECT", "WMA", "20m");
    }

    // ===== isNewMultiplier() - contest STATE =====

    @Test
    @DisplayName("Contest STATE: null state → not a new multiplier")
    void contestStateNullStateIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"state\",\"per_band\":false}}")
                .build();
        contest.setId(11L);
        qso.setContest(contest);
        qso.setState(null);

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest STATE: new state → is a new multiplier")
    void contestStateNewStateIsNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"state\",\"per_band\":false}}")
                .build();
        contest.setId(11L);
        qso.setContest(contest);
        qso.setState("TX");

        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "STATE", "TX", null))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
    }

    // ===== isNewMultiplier() - contest DXCC =====

    @Test
    @DisplayName("Contest DXCC: null dxcc → not a new multiplier")
    void contestDxccNullIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"dxcc\",\"per_band\":false}}")
                .build();
        contest.setId(12L);
        qso.setContest(contest);
        qso.setDxcc(null);

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest DXCC: new entity → is a new multiplier")
    void contestDxccNewEntityIsNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"dxcc\",\"per_band\":false}}")
                .build();
        contest.setId(12L);
        qso.setContest(contest);
        qso.setDxcc(291); // USA

        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "DXCC", "291", null))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest DXCC: existing entity → not a new multiplier")
    void contestDxccExistingEntityIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"dxcc\",\"per_band\":false}}")
                .build();
        contest.setId(12L);
        qso.setContest(contest);
        qso.setDxcc(281); // England

        LogMultiplier existing = LogMultiplier.builder()
                .multiplierType("DXCC")
                .multiplierValue("281")
                .build();
        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "DXCC", "281", null))
                .thenReturn(Optional.of(existing));

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    // ===== isNewMultiplier() - contest GRID =====

    @Test
    @DisplayName("Contest GRID: null grid → not a new multiplier")
    void contestGridNullIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"grid\",\"per_band\":false}}")
                .build();
        contest.setId(13L);
        qso.setContest(contest);
        qso.setGridSquare(null);

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest GRID: new grid → is a new multiplier")
    void contestGridNewGridIsNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"grid\",\"per_band\":false}}")
                .build();
        contest.setId(13L);
        qso.setContest(contest);
        qso.setGridSquare("FN31pr");

        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "GRID", "FN31", null))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest GRID: existing grid → not a new multiplier")
    void contestGridExistingGridIsNotNewMultiplier() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"grid\",\"per_band\":false}}")
                .build();
        contest.setId(13L);
        qso.setContest(contest);
        qso.setGridSquare("DM04rx");

        LogMultiplier existing = LogMultiplier.builder()
                .multiplierType("GRID")
                .multiplierValue("DM04")
                .build();
        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "GRID", "DM04", null))
                .thenReturn(Optional.of(existing));

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
    }

    @Test
    @DisplayName("Contest GRID: truncates to 4 chars for lookup")
    void contestGridTruncatesTo4Chars() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"grid\",\"per_band\":false}}")
                .build();
        contest.setId(13L);
        qso.setContest(contest);
        qso.setGridSquare("FN31pr12"); // 8-char grid → should use first 4

        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "GRID", "FN31", null))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
        verify(logMultiplierRepository).findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "GRID", "FN31", null);
    }

    // ===== isNewMultiplier() - unknown multiplier type =====

    @Test
    @DisplayName("Unknown multiplier type → returns false safely")
    void unknownMultiplierTypeReturnsFalse() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"multipliers\":{\"type\":\"unknown_type\",\"per_band\":false}}")
                .build();
        contest.setId(14L);
        qso.setContest(contest);

        assertFalse(multiplierTrackingService.isNewMultiplier(qso));
        verifyNoInteractions(logMultiplierRepository);
    }

    // ===== isNewMultiplier() - contest with no multipliers config =====

    @Test
    @DisplayName("Contest with no multipliers config → falls back to state check")
    void contestWithNoMultipliersConfigFallsBack() {
        Contest contest = TestDataBuilder.basicContest()
                .rulesConfig("{\"duplicate_window\":\"24_hours\"}")
                .build();
        contest.setId(15L);
        qso.setContest(contest);
        qso.setState("CA");

        when(logMultiplierRepository.findByLogIdAndMultiplierTypeAndMultiplierValueAndBand(
                1L, "STATE", "CA", null))
                .thenReturn(Optional.empty());

        assertTrue(multiplierTrackingService.isNewMultiplier(qso));
    }
}
