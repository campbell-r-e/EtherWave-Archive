package com.hamradio.rigcontrol.service;

import com.hamradio.rigcontrol.connection.MockRigctlConnection;
import com.hamradio.rigcontrol.dispatcher.RigCommandDispatcher;
import com.hamradio.rigcontrol.dto.RigStatus;
import com.hamradio.rigcontrol.ptt.PTTLockManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RigService Tests")
class RigServiceTest {

    private MockRigctlConnection mockConnection;
    private RigCommandDispatcher dispatcher;
    private RigService rigService;

    @BeforeEach
    void setUp() {
        mockConnection = new MockRigctlConnection();
        PTTLockManager pttLockManager = new PTTLockManager();
        dispatcher = new RigCommandDispatcher(mockConnection, pttLockManager);
        dispatcher.init();
        rigService = new RigService(dispatcher);
    }

    @AfterEach
    void tearDown() {
        dispatcher.shutdown();
    }

    @Test
    @DisplayName("getRigStatus - Connected - Returns Complete Status")
    void getRigStatus_connected_returnsCompleteStatus() {
        // Act
        RigStatus status = rigService.getRigStatus();

        // Assert
        assertThat(status.getConnected()).isTrue();
        assertThat(status.getFrequencyHz()).isEqualTo(14250000L);
        assertThat(status.getMode()).isEqualTo("USB");
        assertThat(status.getPttActive()).isFalse();
        assertThat(status.getSMeter()).isNotNull();
        assertThat(status.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("getRigStatus - Connection Error - Returns Disconnected")
    void getRigStatus_connectionError_returnsDisconnected() {
        // Arrange
        mockConnection.setConnected(false);

        // Act
        RigStatus status = rigService.getRigStatus();

        // Assert
        assertThat(status.getConnected()).isFalse();
        assertThat(status.getError()).isNotNull();
    }

    @Test
    @DisplayName("getFrequency - Returns Current Frequency")
    void getFrequency_returnsCurrentFrequency() throws Exception {
        // Act
        Long frequency = rigService.getFrequency().get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(frequency).isEqualTo(14250000L);
    }

    @Test
    @DisplayName("setFrequency - Valid Frequency - Success")
    void setFrequency_validFrequency_success() throws Exception {
        // Act
        Boolean success = rigService.setFrequency(14100000L).get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(success).isTrue();
        assertThat(mockConnection.getFrequency()).isEqualTo(14100000L);
    }

    @Test
    @DisplayName("getMode - Returns Current Mode")
    void getMode_returnsCurrentMode() throws Exception {
        // Act
        String mode = rigService.getMode().get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(mode).isEqualTo("USB");
    }

    @Test
    @DisplayName("setMode - Valid Mode - Success")
    void setMode_validMode_success() throws Exception {
        // Act
        Boolean success = rigService.setMode("LSB", 2400).get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(success).isTrue();
        assertThat(mockConnection.getMode()).isEqualTo("LSB");
    }

    @Test
    @DisplayName("setPTT - Activate - Success")
    void setPTT_activate_success() throws Exception {
        // Act
        RigCommandDispatcher.PTTResult result = rigService.setPTT(true, "Client1")
                .get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(mockConnection.isPTT()).isTrue();
    }

    @Test
    @DisplayName("getPTT - PTT Active - Returns True")
    void getPTT_pttActive_returnsTrue() throws Exception {
        // Arrange
        rigService.setPTT(true, "Client1").get(1, TimeUnit.SECONDS);

        // Act
        Boolean pttActive = rigService.getPTT().get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(pttActive).isTrue();
    }

    @Test
    @DisplayName("getSMeter - Returns S-Meter Value")
    void getSMeter_returnsSMeterValue() throws Exception {
        // Arrange
        mockConnection.setSMeter(-80);

        // Act
        Integer sMeter = rigService.getSMeter().get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(sMeter).isEqualTo(-80);
    }

    @Test
    @DisplayName("testConnection - Connected - Returns True")
    void testConnection_connected_returnsTrue() {
        // Act
        boolean connected = rigService.testConnection();

        // Assert
        assertThat(connected).isTrue();
    }

    @Test
    @DisplayName("testConnection - Not Connected - Returns False")
    void testConnection_notConnected_returnsFalse() {
        // Arrange
        mockConnection.setConnected(false);

        // Act
        boolean connected = rigService.testConnection();

        // Assert
        assertThat(connected).isFalse();
    }
}
