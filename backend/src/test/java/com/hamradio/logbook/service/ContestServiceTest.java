package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.repository.ContestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContestService Unit Tests")
class ContestServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @InjectMocks
    private ContestService contestService;

    private Contest testContest;
    private Contest inactiveContest;

    @BeforeEach
    void setUp() {
        testContest = new Contest();
        testContest.setId(1L);
        testContest.setContestCode("CQWW");
        testContest.setContestName("CQ World Wide DX Contest");
        testContest.setDescription("Premier DX contest");
        testContest.setStartDate(LocalDateTime.now().plusDays(1));
        testContest.setEndDate(LocalDateTime.now().plusDays(3));
        testContest.setIsActive(true);
        testContest.setValidatorClass("com.hamradio.logbook.validation.CQWWValidator");
        testContest.setRulesConfig("{\"multipliers\": [\"DXCC\", \"Zone\"]}");

        inactiveContest = new Contest();
        inactiveContest.setId(2L);
        inactiveContest.setContestCode("ARRL-FD");
        inactiveContest.setContestName("ARRL Field Day");
        inactiveContest.setIsActive(false);
    }

    @Test
    @DisplayName("Should get all contests")
    void shouldGetAllContests() {
        List<Contest> contests = Arrays.asList(testContest, inactiveContest);
        when(contestRepository.findAll()).thenReturn(contests);

        List<Contest> result = contestService.getAllContests();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(contestRepository).findAll();
    }

    @Test
    @DisplayName("Should get only active contests")
    void shouldGetOnlyActiveContests() {
        List<Contest> contests = Arrays.asList(testContest, inactiveContest);
        when(contestRepository.findAll()).thenReturn(contests);

        List<Contest> result = contestService.getActiveContests();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().allMatch(c -> Boolean.TRUE.equals(c.getIsActive())));
        verify(contestRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no active contests")
    void shouldReturnEmptyListWhenNoActiveContests() {
        List<Contest> contests = Arrays.asList(inactiveContest);
        when(contestRepository.findAll()).thenReturn(contests);

        List<Contest> result = contestService.getActiveContests();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should get contest by ID successfully")
    void shouldGetContestById() {
        when(contestRepository.findById(1L)).thenReturn(Optional.of(testContest));

        Optional<Contest> result = contestService.getContestById(1L);

        assertTrue(result.isPresent());
        assertEquals("CQWW", result.get().getContestCode());
        verify(contestRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when contest not found by ID")
    void shouldReturnEmptyWhenContestNotFoundById() {
        when(contestRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Contest> result = contestService.getContestById(999L);

        assertFalse(result.isPresent());
        verify(contestRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get contest by code successfully")
    void shouldGetContestByCode() {
        when(contestRepository.findByContestCode("CQWW")).thenReturn(Optional.of(testContest));

        Optional<Contest> result = contestService.getContestByCode("CQWW");

        assertTrue(result.isPresent());
        assertEquals("CQ World Wide DX Contest", result.get().getContestName());
        verify(contestRepository).findByContestCode("CQWW");
    }

    @Test
    @DisplayName("Should return empty when contest not found by code")
    void shouldReturnEmptyWhenContestNotFoundByCode() {
        when(contestRepository.findByContestCode("NONEXISTENT")).thenReturn(Optional.empty());

        Optional<Contest> result = contestService.getContestByCode("NONEXISTENT");

        assertFalse(result.isPresent());
        verify(contestRepository).findByContestCode("NONEXISTENT");
    }

    @Test
    @DisplayName("Should create contest successfully")
    void shouldCreateContest() {
        when(contestRepository.save(any(Contest.class))).thenReturn(testContest);

        Contest result = contestService.createContest(testContest);

        assertNotNull(result);
        assertEquals("CQWW", result.getContestCode());
        verify(contestRepository).save(testContest);
    }

    @Test
    @DisplayName("Should update contest successfully")
    void shouldUpdateContest() {
        Contest updates = new Contest();
        updates.setContestName("Updated Contest Name");
        updates.setDescription("Updated description");
        updates.setStartDate(LocalDateTime.now().plusDays(5));
        updates.setEndDate(LocalDateTime.now().plusDays(7));
        updates.setIsActive(false);
        updates.setValidatorClass("com.hamradio.logbook.validation.NewValidator");
        updates.setRulesConfig("{\"updated\": true}");

        when(contestRepository.findById(1L)).thenReturn(Optional.of(testContest));
        when(contestRepository.save(any(Contest.class))).thenReturn(testContest);

        Contest result = contestService.updateContest(1L, updates);

        assertNotNull(result);
        verify(contestRepository).save(any(Contest.class));
    }

    @Test
    @DisplayName("Should fail to update contest when not found")
    void shouldFailToUpdateContestWhenNotFound() {
        Contest updates = new Contest();
        when(contestRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contestService.updateContest(999L, updates)
        );

        assertTrue(exception.getMessage().contains("Contest not found"));
        verify(contestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update contest with partial updates")
    void shouldUpdateContestWithPartialUpdates() {
        Contest updates = new Contest();
        updates.setDescription("Only description updated");

        when(contestRepository.findById(1L)).thenReturn(Optional.of(testContest));
        when(contestRepository.save(any(Contest.class))).thenReturn(testContest);

        Contest result = contestService.updateContest(1L, updates);

        assertNotNull(result);
        verify(contestRepository).save(any(Contest.class));
    }

    @Test
    @DisplayName("Should delete contest successfully")
    void shouldDeleteContest() {
        doNothing().when(contestRepository).deleteById(1L);

        contestService.deleteContest(1L);

        verify(contestRepository).deleteById(1L);
    }
}
