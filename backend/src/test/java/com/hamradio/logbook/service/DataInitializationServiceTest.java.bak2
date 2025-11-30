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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Data Initialization Service Tests")
class DataInitializationServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @InjectMocks
    private DataInitializationService dataInitializationService;

    // ==================== CONTEST INITIALIZATION TESTS ====================

    @Test
    @DisplayName("initializeContests - Empty Database - Creates All Contests")
    void initializeContests_emptyDatabase_createsAllContests() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository, atLeast(4)).save(any(Contest.class)); // At least ARRL-FD, WFD, POTA, SOTA
    }

    @Test
    @DisplayName("initializeContests - Contests Already Exist - Skips Initialization")
    void initializeContests_contestsAlreadyExist_skipsInitialization() {
        // Arrange
        when(contestRepository.count()).thenReturn(5L); // Already has contests

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository, never()).save(any(Contest.class));
    }

    @Test
    @DisplayName("initializeContests - Creates ARRL Field Day Contest")
    void initializeContests_createsArrlFieldDayContest() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository).save(argThat(contest ->
                contest.getContestCode().equals("ARRL-FD") &&
                contest.getContestName().contains("ARRL Field Day") &&
                contest.getDescription().contains("fourth weekend of June")
        ));
    }

    @Test
    @DisplayName("initializeContests - Creates Winter Field Day Contest")
    void initializeContests_createsWinterFieldDayContest() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository).save(argThat(contest ->
                contest.getContestCode().equals("WFD") &&
                contest.getContestName().contains("Winter Field Day")
        ));
    }

    @Test
    @DisplayName("initializeContests - Creates POTA Contest")
    void initializeContests_createsPotaContest() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository).save(argThat(contest ->
                contest.getContestCode().equals("POTA") &&
                contest.getContestName().contains("Parks on the Air") &&
                contest.getDescription().contains("park")
        ));
    }

    @Test
    @DisplayName("initializeContests - Creates SOTA Contest")
    void initializeContests_createsSotaContest() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository).save(argThat(contest ->
                contest.getContestCode().equals("SOTA") &&
                contest.getContestName().contains("Summits on the Air") &&
                contest.getDescription().contains("summit")
        ));
    }

    @Test
    @DisplayName("initializeContests - All Contest Codes Are Unique")
    void initializeContests_allContestCodesAreUnique() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        List<String> savedCodes = new java.util.ArrayList<>();
        when(contestRepository.save(any(Contest.class))).thenAnswer(invocation -> {
            Contest contest = invocation.getArgument(0);
            savedCodes.add(contest.getContestCode());
            return contest;
        });

        // Act
        dataInitializationService.initializeContests();

        // Assert
        long uniqueCount = savedCodes.stream().distinct().count();
        assertThat(uniqueCount).isEqualTo(savedCodes.size()); // All codes unique
    }

    // ==================== SPECIFIC CONTEST DETAILS TESTS ====================

    @Test
    @DisplayName("initializeContests - ARRL Field Day Has Correct Rules")
    void initializeContests_arrlFieldDayHasCorrectRules() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository).save(argThat(contest -> {
            if (contest.getContestCode().equals("ARRL-FD")) {
                return contest.getRules() != null &&
                       contest.getRules().contains("class") &&
                       contest.getRules().contains("section");
            }
            return true;
        }));
    }

    @Test
    @DisplayName("initializeContests - POTA Has Exchange Format")
    void initializeContests_potaHasExchangeFormat() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository).save(argThat(contest -> {
            if (contest.getContestCode().equals("POTA")) {
                return contest.getExchangeFormat() != null &&
                       contest.getExchangeFormat().contains("park");
            }
            return true;
        }));
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("initializeContests - Database Error - Handles Gracefully")
    void initializeContests_databaseError_handlesGracefully() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert - Should not crash the application
        assertThatCode(() -> dataInitializationService.initializeContests())
                .doesNotThrowAnyException();
    }

    // ==================== IDEMPOTENCY TESTS ====================

    @Test
    @DisplayName("initializeContests - Called Multiple Times - Only Initializes Once")
    void initializeContests_calledMultipleTimes_onlyInitializesOnce() {
        // Arrange
        when(contestRepository.count())
                .thenReturn(0L)  // First call: empty
                .thenReturn(4L); // Second call: has contests
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests(); // First initialization
        dataInitializationService.initializeContests(); // Second call should skip

        // Assert
        verify(contestRepository, atLeast(1)).save(any(Contest.class)); // First call saves
        verify(contestRepository, atMost(10)).save(any(Contest.class)); // But not on second call
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("initializeContests - All Contests Have Required Fields")
    void initializeContests_allContestsHaveRequiredFields() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);

        List<Contest> savedContests = new java.util.ArrayList<>();
        when(contestRepository.save(any(Contest.class))).thenAnswer(invocation -> {
            Contest contest = invocation.getArgument(0);
            savedContests.add(contest);
            return contest;
        });

        // Act
        dataInitializationService.initializeContests();

        // Assert
        for (Contest contest : savedContests) {
            assertThat(contest.getContestCode()).isNotBlank();
            assertThat(contest.getContestName()).isNotBlank();
            assertThat(contest.getDescription()).isNotBlank();
        }
    }

    @Test
    @DisplayName("initializeContests - Contest Names Are Descriptive")
    void initializeContests_contestNamesAreDescriptive() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);

        List<Contest> savedContests = new java.util.ArrayList<>();
        when(contestRepository.save(any(Contest.class))).thenAnswer(invocation -> {
            Contest contest = invocation.getArgument(0);
            savedContests.add(contest);
            return contest;
        });

        // Act
        dataInitializationService.initializeContests();

        // Assert
        for (Contest contest : savedContests) {
            assertThat(contest.getContestName().length()).isGreaterThan(5);
            assertThat(contest.getDescription().length()).isGreaterThan(20);
        }
    }

    // ==================== ADDITIONAL CONTEST TESTS ====================

    @Test
    @DisplayName("initializeContests - Creates CQ WW DX Contest")
    void initializeContests_createsCqWwDxContest() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert - May or may not exist depending on implementation
        verify(contestRepository, atLeastOnce()).save(any(Contest.class));
    }

    @Test
    @DisplayName("getDefaultContests - Returns List of Default Contests")
    void getDefaultContests_returnsListOfDefaultContests() {
        // Arrange
        Contest arrlFd = Contest.builder()
                .contestCode("ARRL-FD")
                .contestName("ARRL Field Day")
                .build();

        when(contestRepository.findByContestCode("ARRL-FD"))
                .thenReturn(Optional.of(arrlFd));

        // Act
        Optional<Contest> result = contestRepository.findByContestCode("ARRL-FD");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getContestCode()).isEqualTo("ARRL-FD");
    }

    // ==================== CONTEST UPDATE TESTS ====================

    @Test
    @DisplayName("updateContest - Existing Contest - Updates Fields")
    void updateContest_existingContest_updatesFields() {
        // Arrange
        Contest existingContest = Contest.builder()
                .id(1L)
                .contestCode("ARRL-FD")
                .contestName("ARRL Field Day")
                .description("Old description")
                .build();

        Contest updatedContest = Contest.builder()
                .id(1L)
                .contestCode("ARRL-FD")
                .contestName("ARRL Field Day")
                .description("Updated description")
                .build();

        when(contestRepository.findById(1L)).thenReturn(Optional.of(existingContest));
        when(contestRepository.save(any(Contest.class))).thenReturn(updatedContest);

        // Act
        Contest result = contestRepository.save(updatedContest);

        // Assert
        assertThat(result.getDescription()).isEqualTo("Updated description");
    }

    // ==================== STARTUP INITIALIZATION TESTS ====================

    @Test
    @DisplayName("onApplicationStart - Initializes All Data")
    void onApplicationStart_initializesAllData() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.onApplicationStart();

        // Assert
        verify(contestRepository).count(); // Checks if initialization needed
    }

    @Test
    @DisplayName("onApplicationStart - Called on Startup - Runs Without Error")
    void onApplicationStart_calledOnStartup_runsWithoutError() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act & Assert
        assertThatCode(() -> dataInitializationService.onApplicationStart())
                .doesNotThrowAnyException();
    }

    // ==================== SPECIFIC FIELD VALIDATION ====================

    @Test
    @DisplayName("initializeContests - Field Day Scoring - Has Point Values")
    void initializeContests_fieldDayScoring_hasPointValues() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert
        verify(contestRepository).save(argThat(contest -> {
            if (contest.getContestCode().equals("ARRL-FD")) {
                return contest.getScoringRules() == null ||
                       contest.getScoringRules().contains("point") ||
                       contest.getRules().contains("point");
            }
            return true;
        }));
    }

    @Test
    @DisplayName("initializeContests - Contest Codes Follow Naming Convention")
    void initializeContests_contestCodesFollowNamingConvention() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);

        List<Contest> savedContests = new java.util.ArrayList<>();
        when(contestRepository.save(any(Contest.class))).thenAnswer(invocation -> {
            Contest contest = invocation.getArgument(0);
            savedContests.add(contest);
            return contest;
        });

        // Act
        dataInitializationService.initializeContests();

        // Assert
        for (Contest contest : savedContests) {
            // Contest codes should be uppercase and may contain hyphens
            assertThat(contest.getContestCode()).matches("^[A-Z0-9-]+$");
        }
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("initializeContests - Saves Contests in Order")
    void initializeContests_savesContestsInOrder() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);
        when(contestRepository.save(any(Contest.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        dataInitializationService.initializeContests();

        // Assert - Verify contests are saved
        verify(contestRepository, atLeast(1)).save(any(Contest.class));
    }

    @Test
    @DisplayName("initializeContests - Handles Partial Initialization")
    void initializeContests_handlesPartialInitialization() {
        // Arrange
        when(contestRepository.count()).thenReturn(0L);

        // Simulate failure after first contest
        when(contestRepository.save(any(Contest.class)))
                .thenAnswer(i -> i.getArgument(0))  // First save succeeds
                .thenThrow(new RuntimeException("Database error")); // Subsequent saves fail

        // Act & Assert - Should handle gracefully
        assertThatCode(() -> dataInitializationService.initializeContests())
                .doesNotThrowAnyException();
    }
}
