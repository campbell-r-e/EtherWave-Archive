package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.repository.LogRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cabrillo Export Service Tests")
class CabrilloExportServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private QSORepository qsoRepository;

    @InjectMocks
    private CabrilloExportService cabrilloExportService;

    private User testUser;
    private Station testStation;
    private Contest fieldDayContest;
    private Log contestLog;
    private QSO testQSO1;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.basicUser().build();
        testUser.setId(1L);
        
        testStation = TestDataBuilder.basicStation().build();
        testStation.setId(1L);
        
        fieldDayContest = TestDataBuilder.fieldDayContest().build();
        fieldDayContest.setId(1L);
        
        contestLog = TestDataBuilder.contestLog(testUser, fieldDayContest).build();
        contestLog.setId(1L);

        testQSO1 = TestDataBuilder.fieldDayQSO(contestLog, testStation, fieldDayContest)
                .callsign("W1AW")
                .qsoDate(LocalDate.of(2024, 6, 22))
                .timeOn(LocalTime.of(14, 30, 0))
                .build();
        testQSO1.setId(1L);
    }

    @Test
    @DisplayName("exportLog - Field Day Log - Exports Cabrillo Format")
    void exportLog_fieldDayLog_exportsCabrilloFormat() {
        when(logRepository.findById(1L)).thenReturn(Optional.of(contestLog));
        when(qsoRepository.findByLogIdAndDateRange(1L, null, null)).thenReturn(Arrays.asList(testQSO1));

        byte[] result = cabrilloExportService.exportLog(1L, "W1FD", "W1FD", "2A");

        assertThat(result).isNotNull();
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("START-OF-LOG:");
        assertThat(cabrilloContent).contains("CONTEST: ARRL Field Day");
        assertThat(cabrilloContent).contains("CALLSIGN: W1FD");
        assertThat(cabrilloContent).contains("END-OF-LOG");
    }

    @Test
    @DisplayName("exportContestLog - Contest with QSOs - Exports Successfully")
    void exportContestLog_contestWithQSOs_exportsSuccessfully() {
        org.springframework.data.domain.Page<QSO> mockPage =
            new org.springframework.data.domain.PageImpl<>(Arrays.asList(testQSO1));
        when(qsoRepository.findByContest(fieldDayContest, null)).thenReturn(mockPage);

        byte[] result = cabrilloExportService.exportContestLog(fieldDayContest, "W1FD", "W1FD", "2A");

        assertThat(result).isNotNull();
        String cabrilloContent = new String(result);
        assertThat(cabrilloContent).contains("START-OF-LOG:");
        assertThat(cabrilloContent).contains("END-OF-LOG");
    }
}
