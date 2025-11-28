package com.hamradio.rigcontrol.service;

import com.hamradio.rigcontrol.dto.RigStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;

/**
 * Service for communicating with Hamlib rigctld daemon
 * Uses the rigctld protocol over TCP sockets
 */
@Service
@Slf4j
public class HamlibService {

    @Value("${rigctld.host}")
    private String rigctldHost;

    @Value("${rigctld.port}")
    private int rigctldPort;

    /**
     * Get current rig status
     */
    public RigStatus getRigStatus() {
        RigStatus.RigStatusBuilder status = RigStatus.builder()
                .timestamp(LocalDateTime.now())
                .connected(false);

        try {
            // Get frequency
            Long frequency = getFrequency();
            if (frequency != null) {
                status.frequencyHz(frequency);
                status.connected(true);
            }

            // Get mode
            String mode = getMode();
            if (mode != null) {
                status.mode(mode);
            }

            // Get PTT status
            Boolean ptt = getPTT();
            if (ptt != null) {
                status.pttActive(ptt);
            }

            // Get S-meter reading (if available)
            Integer sMeter = getSMeter();
            if (sMeter != null) {
                status.sMeter(sMeter);
            }

        } catch (Exception e) {
            log.error("Error getting rig status: {}", e.getMessage());
            status.error(e.getMessage());
        }

        return status.build();
    }

    /**
     * Set rig frequency in Hz
     */
    public boolean setFrequency(long frequencyHz) {
        try {
            String response = sendCommand("F " + frequencyHz);
            return response != null && response.startsWith("RPRT 0");
        } catch (Exception e) {
            log.error("Error setting frequency: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set rig mode
     */
    public boolean setMode(String mode, int bandwidth) {
        try {
            String response = sendCommand("M " + mode + " " + bandwidth);
            return response != null && response.startsWith("RPRT 0");
        } catch (Exception e) {
            log.error("Error setting mode: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get current frequency in Hz
     */
    private Long getFrequency() {
        try {
            String response = sendCommand("f");
            if (response != null && !response.startsWith("RPRT")) {
                return Long.parseLong(response.trim());
            }
        } catch (Exception e) {
            log.debug("Error getting frequency: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get current mode
     */
    private String getMode() {
        try {
            String response = sendCommand("m");
            if (response != null && !response.startsWith("RPRT")) {
                // Response format: "USB\n3000" (mode and bandwidth)
                String[] parts = response.split("\\n");
                if (parts.length > 0) {
                    return parts[0].trim();
                }
            }
        } catch (Exception e) {
            log.debug("Error getting mode: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get PTT status
     */
    private Boolean getPTT() {
        try {
            String response = sendCommand("t");
            if (response != null && !response.startsWith("RPRT")) {
                int ptt = Integer.parseInt(response.trim());
                return ptt == 1;
            }
        } catch (Exception e) {
            log.debug("Error getting PTT: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get S-meter reading
     */
    private Integer getSMeter() {
        try {
            String response = sendCommand("\\get_level STRENGTH");
            if (response != null && !response.startsWith("RPRT")) {
                return Integer.parseInt(response.trim());
            }
        } catch (Exception e) {
            log.debug("Error getting S-meter: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Send a command to rigctld and get response
     */
    private String sendCommand(String command) throws IOException {
        try (Socket socket = new Socket(rigctldHost, rigctldPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            socket.setSoTimeout(1000); // 1 second timeout

            // Send command
            out.println(command);

            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
                // rigctld ends response with RPRT code or after first line for queries
                if (line.startsWith("RPRT") || command.startsWith("\\get")) {
                    break;
                }
            }

            return response.toString().trim();

        } catch (IOException e) {
            log.debug("rigctld communication error: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Test connection to rigctld
     */
    public boolean testConnection() {
        try {
            Long freq = getFrequency();
            return freq != null;
        } catch (Exception e) {
            return false;
        }
    }
}
