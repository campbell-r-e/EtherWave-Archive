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
@DisplayName("Cabrillo Export Service Tests")
class CabrilloExportServiceTest {

    @Mock
    private QSORepository qsoRepository;

    @InjectMocks
    private CabrilloExportService cabrilloExportService;

    private User testUser;
    private Station testStation;
    private Log testLog;
    private Contest fieldDayContest;
    private QSO testQSO1;
    private QSO testQSO2;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser()
                .id(1L)
                .callsign("W1ABC")
                .build();

        testStation = TestDataBuilder.aValidStation()
                .id(1L)
                .callsign("W1ABC")
                .gridSquare("FN42")
                .build();

        fieldDayContest = Contest.builder()
                .id(1L)
                .contestCode("ARRL-FD")
                .contestName("ARRL Field Day")
                .build();

        testLog = TestDataBuilder.aValidLog(testUser)
                .id(1L)
                .logName("Field Day 2025")
                .build();

        testQSO1 = TestDataBuilder.aValidQSO(testStation, testLog)
                .id(1L)
                .callsign("W1AW")
                .frequencyKhz(14250L)
                .mode("SSB")
                .band("20m")
                .qsoDate(LocalDate.of(2025, 6, 28))
                .timeOn(LocalTime.of(18, 0, 0))
                .rstSent("59")
                .rstRcvd("59")
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        testQSO2 = TestDataBuilder.aValidQSO(testStation, testLog)
                .id(2L)
                .callsign("K2ABC")
                .frequencyKhz(7030L)
                .mode("CW")
                .band("40m")
                .qsoDate(LocalDate.of(2025, 6, 28))
                .timeOn(LocalTime.of(19, 30, 0))
                .rstSent("599")
                .rstRcvd("599")
                .contest(fieldDayContest)
                .contestData("{\"class\":\"3A\",\"section\":\"SCV\"}")
                .build();
    }

    // ==================== BASIC EXPORT TESTS ====================

    @Test
    @DisplayName("exportCabrillo - Valid Field Day Log - Exports Successfully")
    void exportCabrillo_validFieldDayLog_exportsSuccessfully() {
        // Arrange
        when(qsoRepository.findAllByLogId(1L)).thenReturn(Arrays.asList(testQSO1, testQSO2));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        assertThat(result).isNotNull();
        String cabrilloContent = new String(result);

        // Check Cabrillo header
        assertThat(cabrilloContent).contains("START-OF-LOG: 3.0");
        assertThat(cabrilloContent).contains("CONTEST: ARRL-FIELD-DAY");
        assertThat(cabrilloContent).contains("CALLSIGN: W1ABC");
        assertThat(cabrilloContent).contains("CATEGORY-STATION: 2A");
        assertThat(cabrilloContent).contains("ARRL-SECTION: ORG");

        // Check QSO lines
        assertThat(cabrilloContent).contains("QSO:");
        assertThat(cabrilloContent).contains("W1AW");
        assertThat(cabrilloContent).contains("K2ABC");

        // Check footer
        assertThat(cabrilloContent).contains("END-OF-LOG");
    }

    @Test
    @DisplayName("exportCabrillo - Empty Log - Returns Valid Cabrillo with No QSOs")
    void exportCabrillo_emptyLog_returnsValidCabrillo() {
        // Arrange
        when(qsoRepository.findAllByLogId(1L)).thenReturn(Collections.emptyList());

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        assertThat(result).isNotNull();
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("START-OF-LOG: 3.0");
        assertThat(cabrilloContent).contains("END-OF-LOG");
        assertThat(cabrilloContent).doesNotContain("QSO:");
    }

    // ==================== HEADER FIELD TESTS ====================

    @Test
    @DisplayName("exportCabrillo - Header Fields - Formats Correctly")
    void exportCabrillo_headerFields_formatsCorrectly() {
        // Arrange
        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(testQSO1));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "5A", "SCV", "W6XYZ");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("CALLSIGN: W6XYZ");
        assertThat(cabrilloContent).contains("CATEGORY-STATION: 5A");
        assertThat(cabrilloContent).contains("ARRL-SECTION: SCV");
        assertThat(cabrilloContent).contains("CREATED-BY: Ham Radio Contest Logbook");
    }

    @Test
    @DisplayName("exportCabrillo - Grid Square in Header - Includes Grid Locator")
    void exportCabrillo_gridSquareInHeader_includesGridLocator() {
        // Arrange
        Station stationWithGrid = TestDataBuilder.aValidStation()
                .callsign("W1ABC")
                .gridSquare("FN42ab")
                .build();

        QSO qso = TestDataBuilder.aValidQSO(stationWithGrid, testLog)
                .contest(fieldDayContest)
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("GRID-LOCATOR: FN42ab");
    }

    // ==================== QSO LINE FORMAT TESTS ====================

    @Test
    @DisplayName("exportCabrillo - QSO Line Format - Phone Mode")
    void exportCabrillo_qsoLineFormat_phoneMode() {
        // Arrange
        QSO ssbQSO = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1AW")
                .frequencyKhz(14250L)
                .mode("SSB")
                .qsoDate(LocalDate.of(2025, 6, 28))
                .timeOn(LocalTime.of(18, 0, 0))
                .rstSent("59")
                .rstRcvd("59")
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(ssbQSO));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        // QSO: freq  mo date       time call          rst exch   call          rst exch
        // QSO: 14250 PH 2025-06-28 1800 W1ABC         59  2A ORG W1AW          59  2A ORG
        assertThat(cabrilloContent).containsPattern("QSO:\\s+14250\\s+PH\\s+2025-06-28\\s+1800\\s+W1ABC");
        assertThat(cabrilloContent).contains("W1AW");
    }

    @Test
    @DisplayName("exportCabrillo - QSO Line Format - CW Mode")
    void exportCabrillo_qsoLineFormat_cwMode() {
        // Arrange
        QSO cwQSO = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("K2ABC")
                .frequencyKhz(7030L)
                .mode("CW")
                .qsoDate(LocalDate.of(2025, 6, 28))
                .timeOn(LocalTime.of(19, 30, 0))
                .rstSent("599")
                .rstRcvd("599")
                .contest(fieldDayContest)
                .contestData("{\"class\":\"3A\",\"section\":\"SCV\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(cwQSO));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).containsPattern("QSO:\\s+7030\\s+CW\\s+2025-06-28\\s+1930\\s+W1ABC");
        assertThat(cabrilloContent).contains("K2ABC");
    }

    @Test
    @DisplayName("exportCabrillo - Digital Mode - Maps to RY")
    void exportCabrillo_digitalMode_mapsToRY() {
        // Arrange
        QSO ft8QSO = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W3XYZ")
                .frequencyKhz(14074L)
                .mode("FT8")
                .qsoDate(LocalDate.of(2025, 6, 28))
                .timeOn(LocalTime.of(20, 0, 0))
                .rstSent("-10")
                .rstRcvd("-05")
                .contest(fieldDayContest)
                .contestData("{\"class\":\"1A\",\"section\":\"WPA\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(ft8QSO));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).containsPattern("QSO:\\s+14074\\s+RY\\s+2025-06-28\\s+2000");
    }

    // ==================== EXCHANGE FORMAT TESTS ====================

    @Test
    @DisplayName("exportCabrillo - Field Day Exchange - Class and Section")
    void exportCabrillo_fieldDayExchange_classAndSection() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W4FD")
                .mode("SSB")
                .frequencyKhz(7250L)
                .contest(fieldDayContest)
                .contestData("{\"class\":\"10F\",\"section\":\"NFL\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("10F NFL"); // Exchange for contacted station
    }

    // ==================== FREQUENCY TESTS ====================

    @Test
    @DisplayName("exportCabrillo - Various Frequencies - Formats as kHz")
    void exportCabrillo_variousFrequencies_formatsAsKhz() {
        // Arrange
        QSO qso1 = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1")
                .frequencyKhz(3550L) // 80m
                .mode("CW")
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        QSO qso2 = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W2")
                .frequencyKhz(28450L) // 10m
                .mode("SSB")
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(Arrays.asList(qso1, qso2));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("3550");
        assertThat(cabrilloContent).contains("28450");
    }

    // ==================== DATE/TIME FORMAT TESTS ====================

    @Test
    @DisplayName("exportCabrillo - Date Format - YYYY-MM-DD")
    void exportCabrillo_dateFormat_yyyyMmDd() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .qsoDate(LocalDate.of(2025, 12, 31))
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("2025-12-31");
    }

    @Test
    @DisplayName("exportCabrillo - Time Format - HHMM")
    void exportCabrillo_timeFormat_hhmm() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .timeOn(LocalTime.of(23, 59, 45))
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("2359"); // Time in HHMM format
    }

    // ==================== CONTEST-SPECIFIC TESTS ====================

    @Test
    @DisplayName("exportCabrillo - Different Contests - Formats Contest Name")
    void exportCabrillo_differentContests_formatsContestName() {
        // Arrange
        Contest wfd = Contest.builder()
                .id(2L)
                .contestCode("WFD")
                .contestName("Winter Field Day")
                .build();

        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .contest(wfd)
                .contestData("{\"class\":\"2O\",\"section\":\"ORG\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "WFD", "2O", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("CONTEST: WINTER-FIELD-DAY");
    }

    // ==================== SORTING TESTS ====================

    @Test
    @DisplayName("exportCabrillo - QSOs Sorted by Date and Time")
    void exportCabrillo_qsosSortedByDateTime() {
        // Arrange
        QSO qso1 = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1LATER")
                .qsoDate(LocalDate.of(2025, 6, 28))
                .timeOn(LocalTime.of(20, 0, 0))
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        QSO qso2 = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1EARLIER")
                .qsoDate(LocalDate.of(2025, 6, 28))
                .timeOn(LocalTime.of(18, 0, 0))
                .contest(fieldDayContest)
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(Arrays.asList(qso1, qso2));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        int earlierIndex = cabrilloContent.indexOf("W1EARLIER");
        int laterIndex = cabrilloContent.indexOf("W1LATER");
        assertThat(earlierIndex).isLessThan(laterIndex);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("exportCabrillo - Missing Contest Data - Handles Gracefully")
    void exportCabrillo_missingContestData_handlesGracefully() {
        // Arrange
        QSO qsoWithoutContestData = TestDataBuilder.aValidQSO(testStation, testLog)
                .contest(null)
                .contestData(null)
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qsoWithoutContestData));

        // Act & Assert - Should handle gracefully
        assertThatCode(() -> cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("exportCabrillo - Large Log - Exports All QSOs")
    void exportCabrillo_largeLog_exportsAllQSOs() {
        // Arrange
        List<QSO> largeQSOList = new java.util.ArrayList<>();
        for (int i = 0; i < 500; i++) {
            largeQSOList.add(TestDataBuilder.aValidQSO(testStation, testLog)
                    .callsign("W" + i)
                    .contest(fieldDayContest)
                    .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                    .build());
        }
        when(qsoRepository.findAllByLogId(1L)).thenReturn(largeQSOList);

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        assertThat(result).isNotNull();
        String cabrilloContent = new String(result);
        int qsoLineCount = cabrilloContent.split("QSO:").length - 1;
        assertThat(qsoLineCount).isEqualTo(500);
    }

    @Test
    @DisplayName("exportCabrillo - Special Characters in Callsigns - Escapes Properly")
    void exportCabrillo_specialCharactersInCallsigns_escapeProperly() {
        // Arrange
        QSO qso = TestDataBuilder.aValidQSO(testStation, testLog)
                .callsign("W1ABC/P") // Portable operation
                .contest(fieldDayContest)
                .contestData("{\"class\":\"1B\",\"section\":\"ORG\"}")
                .build();

        when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(qso));

        // Act
        byte[] result = cabrilloExportService.exportCabrillo(1L, "ARRL-FD", "2A", "ORG", "W1ABC");

        // Assert
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("W1ABC/P");
    }

    @Test
    @DisplayName("exportCabrillo - All Valid Field Day Classes - Accepts All")
    void exportCabrillo_allValidFieldDayClasses_acceptsAll() {
        // Arrange
        String[] validClasses = {"1A", "2A", "3A", "5A", "10A", "1B", "1C", "1D", "1E", "1F", "10F", "40F"};

        for (String fdClass : validClasses) {
            when(qsoRepository.findAllByLogId(1L)).thenReturn(List.of(testQSO1));

            // Act & Assert
            assertThatCode(() -> cabrilloExportService.exportCabrillo(1L, "ARRL-FD", fdClass, "ORG", "W1ABC"))
                    .doesNotThrowAnyException();
        }
    }
}
