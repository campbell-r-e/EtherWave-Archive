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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ADIF Import Service Tests")
class AdifImportServiceTest {

    @Mock
    private QSORepository qsoRepository;

    @Mock
    private LogRepository logRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private ContestRepository contestRepository;

    @InjectMocks
    private AdifImportService adifImportService;

    private User testUser;
    private Station testStation;
    private Log testLog;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.basicUser().build();
        testUser.setId(1L);
        
        testStation = TestDataBuilder.basicStation().build();
        testStation.setId(1L);
        
        testLog = TestDataBuilder.personalLog(testUser).build();
        testLog.setId(1L);
    }

    @Test
    @DisplayName("importAdif - Valid ADIF Data - Imports Successfully")
    void importAdif_validAdifData_importsSuccessfully() {
        String adifData = "ADIF Export\n<ADIF_VER:5>3.1.4\n<EOH>\n" +
                "<CALL:5>W1AW <QSO_DATE:8>20250115 <TIME_ON:6>143000 " +
                "<FREQ:6>14.250 <MODE:3>SSB <RST_SENT:2>59 <RST_RCVD:2>59 <EOR>\n";

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));
        when(qsoRepository.save(any(QSO.class))).thenAnswer(inv -> inv.getArgument(0));

        AdifImportService.ImportResult result = adifImportService.importAdif(
            adifData.getBytes(), 1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.successCount).isGreaterThan(0);
        assertThat(result.errorCount).isEqualTo(0);
        verify(qsoRepository, atLeastOnce()).save(any(QSO.class));
    }

    @Test
    @DisplayName("importAdif - Invalid Log ID - Returns Error")
    void importAdif_invalidLogId_returnsError() {
        when(logRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adifImportService.importAdif("".getBytes(), 999L, 1L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("importAdif - Empty Data - Returns Zero Imports")
    void importAdif_emptyData_returnsZeroImports() {
        String adifData = "ADIF Export\n<ADIF_VER:5>3.1.4\n<EOH>\n";

        when(logRepository.findById(1L)).thenReturn(Optional.of(testLog));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(testStation));

        AdifImportService.ImportResult result = adifImportService.importAdif(
            adifData.getBytes(), 1L, 1L);

        assertThat(result.successCount).isEqualTo(0);
    }
}
