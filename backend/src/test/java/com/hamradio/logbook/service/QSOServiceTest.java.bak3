package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.*;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QSOService
 * Tests business logic for QSO CRUD operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QSO Service Tests")
class QSOServiceTest {

    @Mock
    private QSORepository qsoRepository;

    @Mock
    private LogRepository logRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private QSOValidationService validationService;

    @InjectMocks
    private QSOService qsoService;

    private User testUser;
    private Station testStation;
    private Log testLog;
    private QSO testQSO;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser().id(1L).build();
        testStation = TestDataBuilder.aValidStation().id(1L).build();
        testLog = TestDataBuilder.aValidLog(testUser).id(1L).build();
        testQSO = TestDataBuilder.aValidQSO(testStation, testLog).id(1L).build();
    }

    // ==================== CREATE QSO TESTS ====================

    @Test
    @DisplayName("createQSO - Valid QSO - Saves Successfully")
    void createQSO_validQSO_savesSuccessfully() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(qsoRepository.save(any(QSO.class))).thenReturn(testQSO);

        // Act
        QSO result = qsoService.createQSO(testQSO, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCallsign()).isEqualTo("W1AW");
        verify(qsoRepository).save(any(QSO.class));
        verify(validationService).validateQSO(testQSO);
    }

    @Test
    @DisplayName("createQSO - Log Not Found - Throws Exception")
    void createQSO_logNotFound_throwsException() {
        // Arrange
        when(logRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> qsoService.createQSO(testQSO, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Log not found");

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("createQSO - Frozen Log - Throws Exception")
    void createQSO_frozenLog_throwsException() {
        // Arrange
        Log frozenLog = TestDataBuilder.aFrozenLog(testUser).id(1L).build();
        when(logRepository.findById(1L)).thenReturn(Optional.of(frozenLog));

        // Act & Assert
        assertThatThrownBy(() -> qsoService.createQSO(testQSO, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen");

        verify(qsoRepository, never()).save(any());
    }

    @Test
    @DisplayName("createQSO - Invalid Callsign - Throws Exception")
    void createQSO_invalidCallsign_throwsException() {
        // Arrange
        QSO invalidQSO = TestDataBuilder.aValidQSO(testStation, testLog).callsign("INVALID!!!").build();
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        doThrow(new IllegalArgumentException("Invalid callsign format"))
                .when(validationService).validateQSO(invalidQSO);

        // Act & Assert
        assertThatThrownBy(() -> qsoService.createQSO(invalidQSO, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid callsign");

        verify(qsoRepository, never()).save(any());
    }

    // ==================== READ QSO TESTS ====================

    @Test
    @DisplayName("getQSOById - Existing QSO - Returns QSO")
    void getQSOById_existingQSO_returnsQSO() {
        // Arrange
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));

        // Act
        Optional<QSO> result = qsoService.getQSOById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getCallsign()).isEqualTo("W1AW");
    }

    @Test
    @DisplayName("getQSOById - Non-existent QSO - Returns Empty")
    void getQSOById_nonExistentQSO_returnsEmpty() {
        // Arrange
        when(qsoRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<QSO> result = qsoService.getQSOById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getQSOsByLogId - Valid Log - Returns Paginated QSOs")
    void getQSOsByLogId_validLog_returnsPaginatedQSOs() {
        // Arrange
        List<QSO> qsos = List.of(testQSO);
        Page<QSO> page = new PageImpl<>(qsos);
        Pageable pageable = PageRequest.of(0, 20);

        when(qsoRepository.findByLogId(1L, pageable)).thenReturn(page);

        // Act
        Page<QSO> result = qsoService.getQSOsByLogId(1L, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCallsign()).isEqualTo("W1AW");
    }

    @Test
    @DisplayName("getQSOsByDateRange - Valid Range - Returns QSOs")
    void getQSOsByDateRange_validRange_returnsQSOs() {
        // Arrange
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);
        when(qsoRepository.findByDateRange(start, end)).thenReturn(List.of(testQSO));

        // Act
        List<QSO> result = qsoService.getQSOsByDateRange(start, end);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQsoDate()).isBetween(start, end);
    }

    // ==================== UPDATE QSO TESTS ====================

    @Test
    @DisplayName("updateQSO - Valid Update - Updates Successfully")
    void updateQSO_validUpdate_updatesSuccessfully() {
        // Arrange
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));
        when(qsoRepository.save(any(QSO.class))).thenReturn(testQSO);

        QSO updatedQSO = TestDataBuilder.aValidQSO(testStation, testLog)
                .id(1L)
                .callsign("K2ABC")
                .build();

        // Act
        QSO result = qsoService.updateQSO(1L, updatedQSO);

        // Assert
        assertThat(result.getCallsign()).isEqualTo("K2ABC");
        verify(qsoRepository).save(any(QSO.class));
    }

    @Test
    @DisplayName("updateQSO - Frozen Log - Throws Exception")
    void updateQSO_frozenLog_throwsException() {
        // Arrange
        Log frozenLog = TestDataBuilder.aFrozenLog(testUser).id(1L).build();
        QSO qsoInFrozenLog = TestDataBuilder.aValidQSO(testStation, frozenLog).id(1L).build();

        when(qsoRepository.findById(1L)).thenReturn(Optional.of(qsoInFrozenLog));

        // Act & Assert
        assertThatThrownBy(() -> qsoService.updateQSO(1L, qsoInFrozenLog))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen");

        verify(qsoRepository, never()).save(any());
    }

    // ==================== DELETE QSO TESTS ====================

    @Test
    @DisplayName("deleteQSO - Existing QSO - Deletes Successfully")
    void deleteQSO_existingQSO_deletesSuccessfully() {
        // Arrange
        when(qsoRepository.findById(1L)).thenReturn(Optional.of(testQSO));

        // Act
        qsoService.deleteQSO(1L);

        // Assert
        verify(qsoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteQSO - Frozen Log - Throws Exception")
    void deleteQSO_frozenLog_throwsException() {
        // Arrange
        Log frozenLog = TestDataBuilder.aFrozenLog(testUser).id(1L).build();
        QSO qsoInFrozenLog = TestDataBuilder.aValidQSO(testStation, frozenLog).id(1L).build();

        when(qsoRepository.findById(1L)).thenReturn(Optional.of(qsoInFrozenLog));

        // Act & Assert
        assertThatThrownBy(() -> qsoService.deleteQSO(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("frozen");

        verify(qsoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteQSO - Non-existent QSO - Throws Exception")
    void deleteQSO_nonExistentQSO_throwsException() {
        // Arrange
        when(qsoRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> qsoService.deleteQSO(999L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(qsoRepository, never()).deleteById(any());
    }

    // ==================== DUPLICATE DETECTION TESTS ====================

    @Test
    @DisplayName("checkForDuplicates - Duplicate QSO - Returns Duplicates")
    void checkForDuplicates_duplicateQSO_returnsDuplicates() {
        // Arrange
        QSO duplicate1 = TestDataBuilder.aValidQSO(testStation, testLog).id(2L).build();
        QSO duplicate2 = TestDataBuilder.aValidQSO(testStation, testLog).id(3L).build();

        when(qsoRepository.findDuplicates("W1AW", "20m", "SSB", null))
                .thenReturn(List.of(duplicate1, duplicate2));

        // Act
        List<QSO> duplicates = qsoService.checkForDuplicates("W1AW", "20m", "SSB");

        // Assert
        assertThat(duplicates).hasSize(2);
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    @DisplayName("getQSOCount - Valid Log - Returns Count")
    void getQSOCount_validLog_returnsCount() {
        // Arrange
        when(qsoRepository.countByLogId(1L)).thenReturn(42L);

        // Act
        long count = qsoService.getQSOCountForLog(1L);

        // Assert
        assertThat(count).isEqualTo(42L);
    }
}
