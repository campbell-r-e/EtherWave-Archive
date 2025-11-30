package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ADIF Export Service Tests")
class AdifExportServiceTest {

    @Mock
    private QSORepository qsoRepository;

    @InjectMocks
    private AdifExportService adifExportService;

    private User testUser;
    private Station testStation;
    private Log testLog;
    private QSO testQSO1;
    private QSO testQSO2;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.basicUser().build();
        testUser.setId(1L);
        
        testStation = TestDataBuilder.basicStation().build();
        testStation.setId(1L);
        
        testLog = TestDataBuilder.personalLog(testUser).build();
        testLog.setId(1L);

        testQSO1 = TestDataBuilder.basicQSO(testLog, testStation)
                .callsign("W1AW")
                .frequencyKhz(14250000L)
                .mode("SSB")
                .band("20m")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .rstSent("59")
                .rstRcvd("59")
                .build();
        testQSO1.setId(1L);

        testQSO2 = TestDataBuilder.basicQSO(testLog, testStation)
                .callsign("K2ABC")
                .frequencyKhz(7030000L)
                .mode("CW")
                .band("40m")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(15, 0, 0))
                .rstSent("599")
                .rstRcvd("599")
                .build();
        testQSO2.setId(2L);
    }

    @Test
    @DisplayName("exportQSOsByLog - Valid Log with QSOs - Exports Successfully")
    void exportQSOsByLog_validLogWithQSOs_exportsSuccessfully() {
        when(qsoRepository.findAllByLogId(1L)).thenReturn(Arrays.asList(testQSO1, testQSO2));

        byte[] result = adifExportService.exportQSOsByLog(1L);

        assertThat(result).isNotNull();
        String adifContent = new String(result);
        assertThat(adifContent).contains("<ADIF_VER:");
        assertThat(adifContent).contains("<CALL:4>W1AW");
        assertThat(adifContent).contains("<CALL:5>K2ABC");
        assertThat(adifContent).contains("<MODE:3>SSB");
        assertThat(adifContent).contains("<MODE:2>CW");
        assertThat(adifContent).contains("<BAND:3>20m");
        assertThat(adifContent).contains("<BAND:3>40m");
        assertThat(adifContent).contains("<EOR>");
    }

    @Test
    @DisplayName("exportQSOsByLog - Empty Log - Returns Empty ADIF")
    void exportQSOsByLog_emptyLog_returnsEmptyAdif() {
        when(qsoRepository.findAllByLogId(1L)).thenReturn(Collections.emptyList());

        byte[] result = adifExportService.exportQSOsByLog(1L);

        assertThat(result).isNotNull();
        String adifContent = new String(result);
        assertThat(adifContent).contains("<ADIF_VER:");
        assertThat(adifContent).contains("<EOR>"); // Trailing EOR is still present
    }

    @Test
    @DisplayName("exportAllQSOs - Multiple QSOs - Exports All")
    void exportAllQSOs_multipleQSOs_exportsAll() {
        when(qsoRepository.findAll()).thenReturn(Arrays.asList(testQSO1, testQSO2));

        byte[] result = adifExportService.exportAllQSOs();

        assertThat(result).isNotNull();
        String adifContent = new String(result);
        assertThat(adifContent).contains("<CALL:4>W1AW");
        assertThat(adifContent).contains("<CALL:5>K2ABC");
    }

    @Test
    @DisplayName("exportQSOsByDateRange - QSOs in Range - Exports Matching QSOs")
    void exportQSOsByDateRange_qsosInRange_exportsMatchingQSOs() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        when(qsoRepository.findByDateRange(startDate, endDate))
            .thenReturn(Arrays.asList(testQSO1, testQSO2));

        byte[] result = adifExportService.exportQSOsByDateRange(startDate, endDate);

        assertThat(result).isNotNull();
        String adifContent = new String(result);
        assertThat(adifContent).contains("<CALL:4>W1AW");
        assertThat(adifContent).contains("<CALL:5>K2ABC");
    }

    @Test
    @DisplayName("exportQSOs - Direct List - Exports QSOs")
    void exportQSOs_directList_exportsQSOs() {
        List<QSO> qsos = Arrays.asList(testQSO1, testQSO2);

        byte[] result = adifExportService.exportQSOs(qsos);

        assertThat(result).isNotNull();
        String adifContent = new String(result);
        assertThat(adifContent).contains("<CALL:4>W1AW");
        assertThat(adifContent).contains("<CALL:5>K2ABC");
    }

    @Test
    @DisplayName("exportQSOs - Empty List - Returns Empty ADIF")
    void exportQSOs_emptyList_returnsEmptyAdif() {
        byte[] result = adifExportService.exportQSOs(Collections.emptyList());

        assertThat(result).isNotNull();
        String adifContent = new String(result);
        assertThat(adifContent).contains("<ADIF_VER:");
        assertThat(adifContent).contains("<EOR>"); // Trailing EOR is still present
    }

    @Test
    @DisplayName("export - QSO with All Fields - Includes All Fields in ADIF")
    void export_qsoWithAllFields_includesAllFields() {
        QSO completeQSO = TestDataBuilder.basicQSO(testLog, testStation)
                .callsign("W1AW")
                .frequencyKhz(14250000L)
                .mode("SSB")
                .band("20m")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .timeOff(LocalTime.of(14, 35, 0))
                .rstSent("59")
                .rstRcvd("59")
                .gridSquare("FN31pr")
                .country("United States")
                .state("Connecticut")
                .county("Hartford")
                .name("John Smith")
                .notes("Great contact!")
                .powerWatts(100)
                .build();
        completeQSO.setId(1L);

        byte[] result = adifExportService.exportQSOs(Arrays.asList(completeQSO));

        String adifContent = new String(result);
        assertThat(adifContent).contains("<GRIDSQUARE:");
        assertThat(adifContent).contains("<COUNTRY:");
        assertThat(adifContent).contains("<STATE:");
        assertThat(adifContent).contains("<CNTY:"); // ADIF uses CNTY not COUNTY
        assertThat(adifContent).contains("<NAME:");
        assertThat(adifContent).contains("<COMMENT:"); // ADIF uses COMMENT not NOTES
        assertThat(adifContent).contains("<TX_PWR:");
    }
}
