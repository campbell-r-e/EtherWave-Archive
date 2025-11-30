package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ADIF Import Service Tests")
class AdifImportServiceTest {

    @Mock
    private QSORepository qsoRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private LogRepository logRepository;

    @InjectMocks
    private AdifImportService adifImportService;

    private Log testLog;
    private Station testStation;

    @BeforeEach
    void setUp() {
        testLog = Log.builder().id(1L).logName("Test Log").isFrozen(false).build();
        testStation = Station.builder().id(1L).callsign("W1ABC").stationName("Home").build();
    }

    // ==================== BASIC IMPORT TESTS ====================

    @Test
    @DisplayName("importAdif - Valid ADIF File - Imports Successfully")
    void importAdif_validFile_importsSuccessfully() {
        // Arrange
        String adifContent = """
                ADIF Export
                <ADIF_VER:5>3.1.4
                <EOH>
                <CALL:4>W1AW <FREQ:8>14.25000 <MODE:3>SSB <QSO_DATE:8>20250115 <TIME_ON:6>143000 <RST_SENT:2>59 <RST_RCVD:2>59 <BAND:3>20m <EOR>
                <CALL:5>K2ABC <FREQ:7>7.03000 <MODE:2>CW <QSO_DATE:8>20250115 <TIME_ON:6>150000 <RST_SENT:3>599 <RST_RCVD:3>599 <BAND:3>40m <EOR>
                """;

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(qsoRepository.save(any(QSO.class))).thenAnswer(invocation -> {
            QSO qso = invocation.getArgument(0);
            qso.setId(System.currentTimeMillis()); // Simulate ID generation
            return qso;
        });

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.totalRecords).isEqualTo(2);
        assertThat(result.successCount).isEqualTo(2);
        assertThat(result.errorCount).isEqualTo(0);
        assertThat(result.importedQSOs).hasSize(2);
        verify(qsoRepository, times(2)).save(any(QSO.class));
    }

    @Test
    @DisplayName("importAdif - Missing Required Field - Records Error")
    void importAdif_missingRequiredField_recordsError() {
        // Arrange
        String adifContent = """
                <ADIF_VER:5>3.1.4
                <EOH>
                <FREQ:8>14.25000 <MODE:3>SSB <QSO_DATE:8>20250115 <TIME_ON:6>143000 <EOR>
                """;

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.totalRecords).isEqualTo(1);
        assertThat(result.errorCount).isEqualTo(1);
        assertThat(result.errors).isNotEmpty();
        assertThat(result.errors.get(0)).contains("CALL");
        verify(qsoRepository, never()).save(any(QSO.class));
    }

    @Test
    @DisplayName("importAdif - Invalid Log ID - Throws Exception")
    void importAdif_invalidLogId_throwsException() {
        // Arrange
        when(logRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adifImportService.importAdif("".getBytes(), 999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Log not found");
    }

    @Test
    @DisplayName("importAdif - Invalid Station ID - Throws Exception")
    void importAdif_invalidStationId_throwsException() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adifImportService.importAdif("".getBytes(), 1L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station not found");
    }

    // ==================== FIELD PARSING TESTS ====================

    @Test
    @DisplayName("importAdif - Contest Data - Parses Correctly")
    void importAdif_contestData_parsesCorrectly() {
        // Arrange
        Contest fieldDay = Contest.builder().id(1L).contestCode("ARRL-FD").build();
        String adifContent = """
                <EOH>
                <CALL:4>W4FD <FREQ:8>14.25000 <MODE:3>SSB <QSO_DATE:8>20250628 <TIME_ON:6>180000 <CLASS:2>2A <ARRL_SECT:3>ORG <CONTEST_ID:7>ARRL-FD <EOR>
                """;

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(contestRepository.findByContestCode("ARRL-FD")).thenReturn(Optional.of(fieldDay));
        when(qsoRepository.save(any(QSO.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.successCount).isEqualTo(1);
        QSO imported = result.importedQSOs.get(0);
        assertThat(imported.getContest()).isEqualTo(fieldDay);
        assertThat(imported.getContestData()).contains("\"class\":\"2A\"");
        assertThat(imported.getContestData()).contains("\"section\":\"ORG\"");
    }

    @Test
    @DisplayName("importAdif - POTA Data - Parses Correctly")
    void importAdif_potaData_parsesCorrectly() {
        // Arrange
        String adifContent = """
                <EOH>
                <CALL:5>K5POT <FREQ:8>14.25000 <MODE:3>SSB <QSO_DATE:8>20250115 <TIME_ON:6>143000 <SIG:4>POTA <SIG_INFO:6>K-0817 <EOR>
                """;

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(qsoRepository.save(any(QSO.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.successCount).isEqualTo(1);
        QSO imported = result.importedQSOs.get(0);
        assertThat(imported.getContestData()).contains("\"park_ref\":\"K-0817\"");
    }

    @Test
    @DisplayName("importAdif - QSL Info - Imports Correctly")
    void importAdif_qslInfo_importsCorrectly() {
        // Arrange
        String adifContent = """
                <EOH>
                <CALL:4>W1AW <FREQ:8>14.25000 <MODE:3>SSB <QSO_DATE:8>20250115 <TIME_ON:6>143000 <QSL_SENT:1>Y <QSL_RCVD:1>Y <LOTW_QSLSDATE:8>20250116 <EOR>
                """;

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(qsoRepository.save(any(QSO.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.successCount).isEqualTo(1);
        QSO imported = result.importedQSOs.get(0);
        assertThat(imported.getQslSent()).isEqualTo("Y");
        assertThat(imported.getQslRcvd()).isEqualTo("Y");
        assertThat(imported.getLotwSent()).isEqualTo("20250116");
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("importAdif - Malformed ADIF - Handles Gracefully")
    void importAdif_malformedAdif_handlesGracefully() {
        // Arrange
        String adifContent = "This is not valid ADIF content!!!";

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.totalRecords).isEqualTo(0);
        assertThat(result.successCount).isEqualTo(0);
    }

    @Test
    @DisplayName("importAdif - Empty File - Returns Empty Result")
    void importAdif_emptyFile_returnsEmptyResult() {
        // Arrange
        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                "".getBytes(), 1L, 1L);

        // Assert
        assertThat(result.totalRecords).isEqualTo(0);
        assertThat(result.successCount).isEqualTo(0);
        assertThat(result.errorCount).isEqualTo(0);
    }

    // ==================== FREQUENCY/BAND CONVERSION TESTS ====================

    @Test
    @DisplayName("importAdif - Frequency in MHz - Converts to kHz")
    void importAdif_frequencyInMhz_convertsToKhz() {
        // Arrange
        String adifContent = """
                <EOH>
                <CALL:4>W1AW <FREQ:8>14.25000 <MODE:3>SSB <QSO_DATE:8>20250115 <TIME_ON:6>143000 <EOR>
                """;

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(qsoRepository.save(any(QSO.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.successCount).isEqualTo(1);
        QSO imported = result.importedQSOs.get(0);
        assertThat(imported.getFrequencyKhz()).isEqualTo(14250L);
    }

    @Test
    @DisplayName("importAdif - Time Formats - Handles Both HHMMSS and HHMM")
    void importAdif_timeFormats_handlesBothFormats() {
        // Arrange
        String adifContent = """
                <EOH>
                <CALL:4>W1AW <FREQ:8>14.25000 <MODE:3>SSB <QSO_DATE:8>20250115 <TIME_ON:4>1430 <EOR>
                <CALL:5>K2ABC <FREQ:7>7.03000 <MODE:2>CW <QSO_DATE:8>20250115 <TIME_ON:6>150000 <EOR>
                """;

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(qsoRepository.save(any(QSO.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        AdifImportService.ImportResult result = adifImportService.importAdif(
                adifContent.getBytes(), 1L, 1L);

        // Assert
        assertThat(result.successCount).isEqualTo(2);
        assertThat(result.importedQSOs.get(0).getTimeOn()).isNotNull();
        assertThat(result.importedQSOs.get(1).getTimeOn()).isNotNull();
    }
}
