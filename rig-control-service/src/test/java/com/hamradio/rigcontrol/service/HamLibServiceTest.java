package com.hamradio.rigcontrol.service;

import com.hamradio.rigcontrol.dto.RigStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HamLib Service Tests")
class HamLibServiceTest {

    private HamLibService hamLibService;

    @Mock
    private Socket mockSocket;

    @Mock
    private BufferedReader mockReader;

    @Mock
    private PrintWriter mockWriter;

    @BeforeEach
    void setUp() {
        hamLibService = new HamLibService();
    }

    // ==================== CONNECTION TESTS ====================

    @Test
    @DisplayName("connect - Valid rigctld - Connects Successfully")
    void connect_validRigctld_connectsSuccessfully() throws Exception {
        // Arrange
        String host = "localhost";
        int port = 4532;

        // Act
        boolean connected = hamLibService.connect(host, port);

        // Assert
        assertThat(connected).isTrue();
        assertThat(hamLibService.isConnected()).isTrue();
    }

    @Test
    @DisplayName("connect - Invalid Host - Returns False")
    void connect_invalidHost_returnsFalse() {
        // Act
        boolean connected = hamLibService.connect("invalid-host", 4532);

        // Assert
        assertThat(connected).isFalse();
        assertThat(hamLibService.isConnected()).isFalse();
    }

    @Test
    @DisplayName("connect - Invalid Port - Returns False")
    void connect_invalidPort_returnsFalse() {
        // Act
        boolean connected = hamLibService.connect("localhost", 99999);

        // Assert
        assertThat(connected).isFalse();
    }

    @Test
    @DisplayName("connect - Connection Timeout - Returns False")
    void connect_connectionTimeout_returnsFalse() {
        // Act
        boolean connected = hamLibService.connect("192.168.1.250", 4532, 1000);

        // Assert
        assertThat(connected).isFalse();
    }

    @Test
    @DisplayName("disconnect - Connected Rig - Disconnects Successfully")
    void disconnect_connectedRig_disconnectsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        hamLibService.disconnect();

        // Assert
        assertThat(hamLibService.isConnected()).isFalse();
    }

    // ==================== FREQUENCY TESTS ====================

    @Test
    @DisplayName("getFrequency - Connected Rig - Returns Frequency")
    void getFrequency_connectedRig_returnsFrequency() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("14250000"); // 14.250 MHz
        hamLibService.connect("localhost", 4532);

        // Act
        long frequency = hamLibService.getFrequency();

        // Assert
        assertThat(frequency).isEqualTo(14250000L);
    }

    @Test
    @DisplayName("getFrequency - Not Connected - Throws Exception")
    void getFrequency_notConnected_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> hamLibService.getFrequency())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not connected");
    }

    @Test
    @DisplayName("setFrequency - Valid Frequency - Sets Successfully")
    void setFrequency_validFrequency_setsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setFrequency(14250000L);

        // Assert
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("setFrequency - Invalid Frequency - Returns False")
    void setFrequency_invalidFrequency_returnsFalse() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setFrequency(100L); // Too low

        // Assert
        assertThat(success).isFalse();
    }

    // ==================== MODE TESTS ====================

    @Test
    @DisplayName("getMode - Connected Rig - Returns Mode")
    void getMode_connectedRig_returnsMode() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("USB");
        hamLibService.connect("localhost", 4532);

        // Act
        String mode = hamLibService.getMode();

        // Assert
        assertThat(mode).isEqualTo("USB");
    }

    @Test
    @DisplayName("setMode - Valid Mode - Sets Successfully")
    void setMode_validMode_setsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setMode("USB");

        // Assert
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("setMode - Invalid Mode - Returns False")
    void setMode_invalidMode_returnsFalse() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setMode("INVALID");

        // Assert
        assertThat(success).isFalse();
    }

    @Test
    @DisplayName("convertMode - Hamlib to Standard - Converts Correctly")
    void convertMode_hamlibToStandard_convertsCorrectly() {
        // Act & Assert
        assertThat(hamLibService.convertMode("USB")).isEqualTo("SSB");
        assertThat(hamLibService.convertMode("LSB")).isEqualTo("SSB");
        assertThat(hamLibService.convertMode("CW")).isEqualTo("CW");
        assertThat(hamLibService.convertMode("RTTY")).isEqualTo("RTTY");
        assertThat(hamLibService.convertMode("PKTUSB")).isEqualTo("FT8");
    }

    // ==================== VFO TESTS ====================

    @Test
    @DisplayName("getVFO - Connected Rig - Returns VFO")
    void getVFO_connectedRig_returnsVFO() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("VFOA");
        hamLibService.connect("localhost", 4532);

        // Act
        String vfo = hamLibService.getVFO();

        // Assert
        assertThat(vfo).isEqualTo("VFOA");
    }

    @Test
    @DisplayName("setVFO - Valid VFO - Sets Successfully")
    void setVFO_validVFO_setsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setVFO("VFOB");

        // Assert
        assertThat(success).isTrue();
    }

    // ==================== POWER TESTS ====================

    @Test
    @DisplayName("getPower - Connected Rig - Returns Power Level")
    void getPower_connectedRig_returnsPowerLevel() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("100");
        hamLibService.connect("localhost", 4532);

        // Act
        int power = hamLibService.getPower();

        // Assert
        assertThat(power).isEqualTo(100);
    }

    @Test
    @DisplayName("setPower - Valid Power - Sets Successfully")
    void setPower_validPower_setsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setPower(50);

        // Assert
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("setPower - Power Out of Range - Returns False")
    void setPower_powerOutOfRange_returnsFalse() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setPower(150); // Too high

        // Assert
        assertThat(success).isFalse();
    }

    // ==================== RIG STATUS TESTS ====================

    @Test
    @DisplayName("getRigStatus - Connected Rig - Returns Complete Status")
    void getRigStatus_connectedRig_returnsCompleteStatus() throws Exception {
        // Arrange
        when(mockReader.readLine())
                .thenReturn("14250000") // Frequency
                .thenReturn("USB")      // Mode
                .thenReturn("VFOA")     // VFO
                .thenReturn("100");     // Power

        hamLibService.connect("localhost", 4532);

        // Act
        RigStatusDTO status = hamLibService.getRigStatus();

        // Assert
        assertThat(status).isNotNull();
        assertThat(status.getFrequencyHz()).isEqualTo(14250000L);
        assertThat(status.getMode()).isEqualTo("SSB");
        assertThat(status.getVfo()).isEqualTo("VFOA");
        assertThat(status.getPower()).isEqualTo(100);
    }

    @Test
    @DisplayName("getRigStatus - Not Connected - Throws Exception")
    void getRigStatus_notConnected_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> hamLibService.getRigStatus())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not connected");
    }

    // ==================== RIG INFO TESTS ====================

    @Test
    @DisplayName("getRigInfo - Connected Rig - Returns Rig Information")
    void getRigInfo_connectedRig_returnsRigInformation() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("Icom IC-7300");
        hamLibService.connect("localhost", 4532);

        // Act
        String rigInfo = hamLibService.getRigInfo();

        // Assert
        assertThat(rigInfo).isEqualTo("Icom IC-7300");
    }

    // ==================== POLLING TESTS ====================

    @Test
    @DisplayName("startPolling - Connected Rig - Starts Polling")
    void startPolling_connectedRig_startsPolling() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        hamLibService.startPolling(1000); // Poll every 1 second

        // Assert
        assertThat(hamLibService.isPolling()).isTrue();
    }

    @Test
    @DisplayName("stopPolling - Polling Active - Stops Polling")
    void stopPolling_pollingActive_stopsPolling() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);
        hamLibService.startPolling(1000);

        // Act
        hamLibService.stopPolling();

        // Assert
        assertThat(hamLibService.isPolling()).isFalse();
    }

    // ==================== PTT TESTS ====================

    @Test
    @DisplayName("setPTT - Transmit On - Sets PTT Successfully")
    void setPTT_transmitOn_setsPTTSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setPTT(true);

        // Assert
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("getPTT - Rig Transmitting - Returns True")
    void getPTT_rigTransmitting_returnsTrue() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("1"); // PTT on
        hamLibService.connect("localhost", 4532);

        // Act
        boolean pttOn = hamLibService.getPTT();

        // Assert
        assertThat(pttOn).isTrue();
    }

    // ==================== SPLIT OPERATION TESTS ====================

    @Test
    @DisplayName("setSplit - Enable Split - Sets Successfully")
    void setSplit_enableSplit_setsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setSplit(true, "VFOB");

        // Assert
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("getSplit - Split Enabled - Returns True")
    void getSplit_splitEnabled_returnsTrue() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("1"); // Split on
        hamLibService.connect("localhost", 4532);

        // Act
        boolean splitOn = hamLibService.getSplit();

        // Assert
        assertThat(splitOn).isTrue();
    }

    // ==================== BAND TESTS ====================

    @Test
    @DisplayName("determineBand - 20m Frequency - Returns 20m")
    void determineBand_20mFrequency_returns20m() {
        // Act
        String band = hamLibService.determineBand(14250000L);

        // Assert
        assertThat(band).isEqualTo("20m");
    }

    @Test
    @DisplayName("determineBand - 40m Frequency - Returns 40m")
    void determineBand_40mFrequency_returns40m() {
        // Act
        String band = hamLibService.determineBand(7125000L);

        // Assert
        assertThat(band).isEqualTo("40m");
    }

    @Test
    @DisplayName("determineBand - Out of Band - Returns Unknown")
    void determineBand_outOfBand_returnsUnknown() {
        // Act
        String band = hamLibService.determineBand(100000L);

        // Assert
        assertThat(band).isEqualTo("Unknown");
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("getFrequency - Communication Error - Throws Exception")
    void getFrequency_communicationError_throwsException() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenThrow(new RuntimeException("Connection lost"));
        hamLibService.connect("localhost", 4532);

        // Act & Assert
        assertThatThrownBy(() -> hamLibService.getFrequency())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("connect - Already Connected - Returns True")
    void connect_alreadyConnected_returnsTrue() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean connected = hamLibService.connect("localhost", 4532);

        // Assert
        assertThat(connected).isTrue();
    }

    // ==================== MEMORY CHANNEL TESTS ====================

    @Test
    @DisplayName("setMemoryChannel - Valid Channel - Sets Successfully")
    void setMemoryChannel_validChannel_setsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setMemoryChannel(1);

        // Assert
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("getMemoryChannel - Returns Current Channel")
    void getMemoryChannel_returnsCurrentChannel() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("1");
        hamLibService.connect("localhost", 4532);

        // Act
        int channel = hamLibService.getMemoryChannel();

        // Assert
        assertThat(channel).isEqualTo(1);
    }

    // ==================== RIT/XIT TESTS ====================

    @Test
    @DisplayName("setRIT - Valid Offset - Sets Successfully")
    void setRIT_validOffset_setsSuccessfully() throws Exception {
        // Arrange
        hamLibService.connect("localhost", 4532);

        // Act
        boolean success = hamLibService.setRIT(100); // +100 Hz offset

        // Assert
        assertThat(success).isTrue();
    }

    @Test
    @DisplayName("getRIT - Returns RIT Offset")
    void getRIT_returnsRITOffset() throws Exception {
        // Arrange
        when(mockReader.readLine()).thenReturn("100");
        hamLibService.connect("localhost", 4532);

        // Act
        int offset = hamLibService.getRIT();

        // Assert
        assertThat(offset).isEqualTo(100);
    }
}
