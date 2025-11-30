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
        testUser = TestDataBuilder.aValidUser().id(1L).build();
        testStation = TestDataBuilder.aValidStation().id(1L).build();
        testLog = TestDataBuilder.aValidLog(testUser).id(1L).build();

        testQSO1 = TestDataBuilder.aValidQSO(testStation, testLog)
                .id(1L)
                .callsign("W1AW")
                .frequencyKhz(14250L)
                .mode("SSB")
                .band("20m")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .rstSent("59")
                .rstRcvd("59")
                .build();

        testQSO2 = TestDataBuilder.aValidQSO(testStation, testLog)
                .id(2L)
                .callsign("K2ABC")
                .frequencyKhz(7030L)
                .mode("CW")
                .band("40m")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(15, 0, 0))
                .rstSent("599")
                .rstRcvd("599")
                .build();
    }

    // ==================== BASIC EXPORT TESTS ====================

    @Test
    @DisplayName("exportQSOsByLog - Valid Log with QSOs - Exports Successfully")
    void exportQSOsByLog_validLogWithQSOs_exportsSuccessfully() {
        // Arrange
        when(qsoRepository.findAllByLogId(1L)).thenReturn(Arrays.asList(testQSO1, testQSO2));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        assertThat(result).isNotNull();
        String adifContent = new String(result);

        // Check ADIF header
        assertThat(adifContent).contains("<ADIF_VER:");
        assertThat(adifContent).contains("<EOH>");

        // Check first QSO
        assertThat(adifContent).contains("<CALL:4>W1AW");
        assertThat(adifContent).contains("<MODE:3>SSB");
        assertThat(adifContent).contains("<BAND:3>20m");
        assertThat(adifContent).contains("<QSO_DATE:8>20250115");
        assertThat(adifContent).contains("<TIME_ON:6>143000");

        // Check second QSO
        assertThat(adifContent).contains("<CALL:5>K2ABC");
        assertThat(adifContent).contains("<MODE:2>CW");
        assertThat(adifContent).contains("<BAND:3>40m");

        verify(qsoRepository).findAllByLogId(1L);
    }

    @Test
    @DisplayName("exportQSOsByLog - Empty Log - Returns Valid ADIF with No Records")
    void exportQSOsByLog_emptyLog_returnsValidAdif() {
        // Arrange
        when(qsoRepository.findAllByLogId(1L)).thenReturn(Collections.emptyList());

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        assertThat(result).isNotNull();
        String adifContent = new String(result);
        assertThat(adifContent).contains("<ADIF_VER:");
        assertThat(adifContent).contains("<EOH>");
        assertThat(adifContent).doesNotContain("<CALL:");
    }

    // ==================== FREQUENCY EXPORT TESTS ====================

    @Test
    @DisplayName("exportQSOs - Frequency in kHz - Converts to MHz")
    void exportQSOs_frequencyInKhz_convertsToMhz() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .frequencyKhz(14250L) // 14.250 MHz
                .build();
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<FREQ:8>14.25000"); // MHz format
    }

    @Test
    @DisplayName("exportQSOs - Various Frequencies - Formats Correctly")
    void exportQSOs_variousFrequencies_formatsCorrectly() {
        // Arrange
        QSO qso1 = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1")
                .frequencyKhz(7030L) // 7.030 MHz
                .build();
        QSO qso2 = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W2")
                .frequencyKhz(50100L) // 50.100 MHz
                .build();
        QSO qso3 = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W3")
                .frequencyKhz(144200L) // 144.200 MHz
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(Arrays.asList(qso1, qso2, qso3));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<FREQ:7>7.03000");
        assertThat(adifContent).contains("<FREQ:7>50.1000");
        assertThat(adifContent).contains("<FREQ:8>144.2000");
    }

    // ==================== CONTEST DATA EXPORT TESTS ====================

    @Test
    @DisplayName("exportQSOs - Field Day Contest Data - Exports Class and Section")
    void exportQSOs_fieldDayContestData_exportsClassAndSection() throws Exception {
        // Arrange
        Contest fieldDay = Contest.builder().id(1L).contestCode("ARRL-FD").build();
        String contestData = "{\"class\":\"2A\",\"section\":\"ORG\"}";

        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .contest(fieldDay)
                .contestData(contestData)
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<CLASS:2>2A");
        assertThat(adifContent).contains("<ARRL_SECT:3>ORG");
        assertThat(adifContent).contains("<CONTEST_ID:7>ARRL-FD");
    }

    @Test
    @DisplayName("exportQSOs - POTA Contest Data - Exports Park Reference")
    void exportQSOs_potaContestData_exportsParkReference() throws Exception {
        // Arrange
        Contest pota = Contest.builder().id(2L).contestCode("POTA").build();
        String contestData = "{\"park_ref\":\"K-0817\"}";

        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .contest(pota)
                .contestData(contestData)
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<SIG:4>POTA");
        assertThat(adifContent).contains("<SIG_INFO:6>K-0817");
    }

    @Test
    @DisplayName("exportQSOs - SOTA Contest Data - Exports Summit Reference")
    void exportQSOs_sotaContestData_exportsSummitReference() throws Exception {
        // Arrange
        Contest sota = Contest.builder().id(3L).contestCode("SOTA").build();
        String contestData = "{\"summit_ref\":\"W7W/CN-001\"}";

        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .contest(sota)
                .contestData(contestData)
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<SIG:4>SOTA");
        assertThat(adifContent).contains("<SIG_INFO:10>W7W/CN-001");
    }

    // ==================== QSL EXPORT TESTS ====================

    @Test
    @DisplayName("exportQSOs - QSL Info - Exports All QSL Fields")
    void exportQSOs_qslInfo_exportsAllFields() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .qslSent("Y")
                .qslRcvd("Y")
                .lotwSent("20250116")
                .lotwRcvd("20250116")
                .eqslSent("Y")
                .eqslRcvd("Y")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<QSL_SENT:1>Y");
        assertThat(adifContent).contains("<QSL_RCVD:1>Y");
        assertThat(adifContent).contains("<LOTW_QSLSDATE:8>20250116");
        assertThat(adifContent).contains("<LOTW_QSLRDATE:8>20250116");
        assertThat(adifContent).contains("<EQSL_QSLSDATE:1>Y");
        assertThat(adifContent).contains("<EQSL_QSLRDATE:1>Y");
    }

    // ==================== STATION/OPERATOR EXPORT TESTS ====================

    @Test
    @DisplayName("exportQSOs - Station Info - Exports Station Callsign and Grid")
    void exportQSOs_stationInfo_exportsStationCallsignAndGrid() {
        // Arrange
        Station station = TestDataBuilder.aValidStation()
                .callsign("W1ABC")
                .gridSquare("FN42")
                .build();

        QSO qso = TestDataBuilder.aValidQSO(station, testLog).build();
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<STATION_CALLSIGN:5>W1ABC");
        assertThat(adifContent).contains("<MY_GRIDSQUARE:4>FN42");
    }

    @Test
    @DisplayName("exportQSOs - Operator Info - Exports Operator Callsign")
    void exportQSOs_operatorInfo_exportsOperatorCallsign() {
        // Arrange
        Operator operator = Operator.builder()
                .id(1L)
                .callsign("W1OP")
                .name("Test Operator")
                .build();

        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .operator(operator)
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<OPERATOR:4>W1OP");
    }

    // ==================== LOCATION EXPORT TESTS ====================

    @Test
    @DisplayName("exportQSOs - Location Data - Exports Grid, State, County")
    void exportQSOs_locationData_exportsAllLocationFields() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .gridSquare("FN42ab")
                .state("CA")
                .county("Orange")
                .country("United States")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<GRIDSQUARE:6>FN42ab");
        assertThat(adifContent).contains("<STATE:2>CA");
        assertThat(adifContent).contains("<CNTY:6>Orange");
        assertThat(adifContent).contains("<COUNTRY:13>United States");
    }

    // ==================== NOTES EXPORT TESTS ====================

    @Test
    @DisplayName("exportQSOs - Notes Field - Exports Comments")
    void exportQSOs_notesField_exportsComments() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .notes("Great signal on 20m")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<COMMENT:21>Great signal on 20m");
    }

    // ==================== DATE/TIME FORMAT TESTS ====================

    @Test
    @DisplayName("exportQSOs - Date Format - Exports as YYYYMMDD")
    void exportQSOs_dateFormat_exportsAsYYYYMMDD() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 1, 15))
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<QSO_DATE:8>20250115");
    }

    @Test
    @DisplayName("exportQSOs - Time Format - Exports as HHMMSS")
    void exportQSOs_timeFormat_exportsAsHHMMSS() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .timeOn(LocalTime.of(14, 30, 45))
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        String adifContent = new String(result);
        assertThat(adifContent).contains("<TIME_ON:6>143045");
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("exportQSOs - Missing Optional Fields - Handles Gracefully")
    void exportQSOs_missingOptionalFields_handlesGracefully() {
        // Arrange
        QSO minimalQSO = QSO.builder()
                .station(testStation)
                .log(testLog)
                .callsign("W1MIN")
                .frequencyKhz(14250L)
                .mode("SSB")
                .band("20m")
                .qsoDate(LocalDate.of(2025, 1, 15))
                .timeOn(LocalTime.of(14, 30, 0))
                .isValid(true)
                // All optional fields null
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(minimalQSO));

        // Act & Assert - Should not throw exception
        assertThatCode(() -> adifExportService.exportQSOsByLog(1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("exportQSOs - Large Log - Exports All Records")
    void exportQSOs_largeLog_exportsAllRecords() {
        // Arrange
        List<QSO> largeQSOList = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeQSOList.add(TestDataBuilder.aValidQSO(testStation, testLog)
                    .callsign("W" + i)
                    .build());
        }
        when(qsoRepository.findAllByLogId(1L)).thenReturn(largeQSOList);

        // Act
        byte[] result = adifExportService.exportQSOsByLog(1L);

        // Assert
        assertThat(result).isNotNull();
        String adifContent = new String(result);
        // Count <EOR> markers (one per record)
        int eorCount = adifContent.split("<EOR>").length - 1;
        assertThat(eorCount).isEqualTo(1000);
    }
}
