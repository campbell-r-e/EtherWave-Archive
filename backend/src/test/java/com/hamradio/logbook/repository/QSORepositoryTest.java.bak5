package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("QSO Repository Integration Tests")
class QSORepositoryTest extends BaseIntegrationTest {

    @Autowired
    private QSORepository qsoRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Station testStation;
    private Log testLog;

    @BeforeEach
    void setUp() {
        qsoRepository.deleteAll();
        logRepository.deleteAll();
        stationRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(TestDataBuilder.aValidUser().build());
        testStation = stationRepository.save(TestDataBuilder.aValidStation().build());
        testLog = logRepository.save(TestDataBuilder.aValidLog(testUser).build());
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid QSO - Persists Successfully")
    void save_validQSO_persistsSuccessfully() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog).build();

        // Act
        QSO saved = qsoRepository.save(qso);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCallsign()).isEqualTo("W1AW");
        assertThat(saved.getFrequencyKhz()).isEqualTo(14250L);
    }

    @Test
    @DisplayName("findById - Existing QSO - Returns QSO")
    void findById_existingQSO_returnsQSO() {
        // Arrange
        QSO qso = qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).build());

        // Act
        QSO found = qsoRepository.findById(qso.getId()).orElse(null);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getCallsign()).isEqualTo("W1AW");
    }

    // ==================== FIND BY LOG TESTS ====================

    @Test
    @DisplayName("findByLogId - Returns QSOs in Paginated Form")
    void findByLogId_returnsQSOsInPaginatedForm() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("W1AW").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("K2ABC").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("N3XYZ").build());

        // Act
        Page<QSO> page = qsoRepository.findByLogId(testLog.getId(), PageRequest.of(0, 10));

        // Assert
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findAllByLogId - Returns All QSOs for Log")
    void findAllByLogId_returnsAllQSOsForLog() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("W1").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("W2").build());

        // Act
        List<QSO> qsos = qsoRepository.findAllByLogId(testLog.getId());

        // Assert
        assertThat(qsos).hasSize(2);
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("findByLogIdAndCallsignContaining - Returns Matching QSOs")
    void findByLogIdAndCallsignContaining_returnsMatchingQSOs() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("W1AW").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("W1ABC").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("K2ABC").build());

        // Act
        List<QSO> results = qsoRepository.findByLogIdAndCallsignContaining(testLog.getId(), "W1");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(qso -> qso.getCallsign().startsWith("W1"));
    }

    // ==================== DATE RANGE TESTS ====================

    @Test
    @DisplayName("findByLogIdAndQsoDateBetween - Returns QSOs in Date Range")
    void findByLogIdAndQsoDateBetween_returnsQSOsInDateRange() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 10)).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 15)).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 20)).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 25)).build());

        // Act
        List<QSO> results = qsoRepository.findByLogIdAndQsoDateBetween(
                testLog.getId(),
                LocalDate.of(2025, 1, 12),
                LocalDate.of(2025, 1, 22)
        );

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(qso ->
                !qso.getQsoDate().isBefore(LocalDate.of(2025, 1, 12)) &&
                !qso.getQsoDate().isAfter(LocalDate.of(2025, 1, 22))
        );
    }

    // ==================== DUPLICATE DETECTION TESTS ====================

    @Test
    @DisplayName("findByLogIdAndCallsignAndQsoDateAndTimeOn - Finds Duplicate")
    void findByLogIdAndCallsignAndQsoDateAndTimeOn_findsDuplicate() {
        // Arrange
        LocalDate date = LocalDate.of(2025, 1, 15);
        LocalTime time = LocalTime.of(14, 30, 0);

        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1AW")
                .qsoDate(date)
                .timeOn(time)
                .build());

        // Act
        List<QSO> duplicates = qsoRepository.findByLogIdAndCallsignAndQsoDateAndTimeOn(
                testLog.getId(), "W1AW", date, time
        );

        // Assert
        assertThat(duplicates).hasSize(1);
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    @DisplayName("countByLogId - Returns Correct Count")
    void countByLogId_returnsCorrectCount() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).build());

        // Act
        long count = qsoRepository.countByLogId(testLog.getId());

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countDistinctCallsignsByLogId - Returns Unique Callsign Count")
    void countDistinctCallsignsByLogId_returnsUniqueCallsignCount() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("W1AW").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("W1AW").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).callsign("K2ABC").build());

        // Act
        long uniqueCount = qsoRepository.countDistinctCallsignsByLogId(testLog.getId());

        // Assert
        assertThat(uniqueCount).isEqualTo(2);
    }

    // ==================== BAND AND MODE TESTS ====================

    @Test
    @DisplayName("findByLogIdAndBand - Returns QSOs on Specific Band")
    void findByLogIdAndBand_returnsQSOsOnSpecificBand() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).band("20m").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).band("20m").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).band("40m").build());

        // Act
        List<QSO> results = qsoRepository.findByLogIdAndBand(testLog.getId(), "20m");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(qso -> qso.getBand().equals("20m"));
    }

    @Test
    @DisplayName("findByLogIdAndMode - Returns QSOs with Specific Mode")
    void findByLogIdAndMode_returnsQSOsWithSpecificMode() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).mode("SSB").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).mode("CW").build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).mode("CW").build());

        // Act
        List<QSO> results = qsoRepository.findByLogIdAndMode(testLog.getId(), "CW");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(qso -> qso.getMode().equals("CW"));
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes QSO")
    void deleteById_removesQSO() {
        // Arrange
        QSO qso = qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).build());
        Long qsoId = qso.getId();

        // Act
        qsoRepository.deleteById(qsoId);

        // Assert
        assertThat(qsoRepository.findById(qsoId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByLogId - Removes All QSOs for Log")
    void deleteByLogId_removesAllQSOsForLog() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).build());

        // Act
        qsoRepository.deleteByLogId(testLog.getId());

        // Assert
        assertThat(qsoRepository.countByLogId(testLog.getId())).isZero();
    }

    // ==================== SORTING TESTS ====================

    @Test
    @DisplayName("findByLogId - Returns QSOs Sorted by Date Descending")
    void findByLogId_returnsQSOsSortedByDateDescending() {
        // Arrange
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 10)).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 20)).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 15)).build());

        // Act
        Page<QSO> page = qsoRepository.findByLogId(testLog.getId(), PageRequest.of(0, 10));

        // Assert
        List<QSO> qsos = page.getContent();
        assertThat(qsos.get(0).getQsoDate()).isEqualTo(LocalDate.of(2025, 1, 20));
        assertThat(qsos.get(1).getQsoDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(qsos.get(2).getQsoDate()).isEqualTo(LocalDate.of(2025, 1, 10));
    }

    // ==================== CONTEST TESTS ====================

    @Test
    @DisplayName("findByLogIdAndContestIsNotNull - Returns Contest QSOs Only")
    void findByLogIdAndContestIsNotNull_returnsContestQSOsOnly() {
        // Arrange
        Contest contest = new Contest();
        contest.setContestCode("ARRL-FD");
        contest.setContestName("Field Day");

        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).contest(contest).build());
        qsoRepository.save(TestDataBuilder.aValidQSO(testStation, testLog).contest(null).build());

        // Act
        List<QSO> results = qsoRepository.findByLogIdAndContestIsNotNull(testLog.getId());

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContest()).isNotNull();
    }
}
