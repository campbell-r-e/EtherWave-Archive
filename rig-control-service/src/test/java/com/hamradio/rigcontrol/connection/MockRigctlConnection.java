package com.hamradio.rigcontrol.connection;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mock implementation of RigctlConnection for testing.
 * Simulates a radio without requiring actual hardware.
 */
public class MockRigctlConnection implements RigctlConnection {

    private final AtomicLong frequency = new AtomicLong(14250000); // 20m band default
    private final AtomicReference<String> mode = new AtomicReference<>("USB");
    private final AtomicReference<Integer> bandwidth = new AtomicReference<>(3000);
    private final AtomicReference<Boolean> ptt = new AtomicReference<>(false);
    private final AtomicReference<Integer> sMeter = new AtomicReference<>(-73);

    private boolean connected = true;
    private boolean simulateError = false;
    private long commandDelay = 0; // Simulate network latency

    @Override
    public String sendCommand(String command) throws IOException {
        if (!connected) {
            throw new IOException("Not connected to rigctld");
        }

        if (simulateError) {
            throw new IOException("Simulated error");
        }

        // Simulate network latency if configured
        if (commandDelay > 0) {
            try {
                Thread.sleep(commandDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return processCommand(command);
    }

    private String processCommand(String command) {
        String cmd = command.trim();

        // Get frequency
        if (cmd.equals("f")) {
            return frequency.get() + "\nRPRT 0";
        }

        // Set frequency
        if (cmd.startsWith("F ")) {
            try {
                long freq = Long.parseLong(cmd.substring(2).trim());
                frequency.set(freq);
                return "RPRT 0";
            } catch (NumberFormatException e) {
                return "RPRT -1"; // Error
            }
        }

        // Get mode
        if (cmd.equals("m")) {
            return mode.get() + "\n" + bandwidth.get() + "\nRPRT 0";
        }

        // Set mode
        if (cmd.startsWith("M ")) {
            String[] parts = cmd.substring(2).trim().split("\\s+");
            if (parts.length >= 1) {
                mode.set(parts[0]);
                if (parts.length >= 2) {
                    try {
                        bandwidth.set(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        // Use default
                    }
                }
                return "RPRT 0";
            }
            return "RPRT -1";
        }

        // Get PTT
        if (cmd.equals("t")) {
            return (ptt.get() ? "1" : "0") + "\nRPRT 0";
        }

        // Set PTT
        if (cmd.startsWith("T ")) {
            String value = cmd.substring(2).trim();
            ptt.set(value.equals("1"));
            return "RPRT 0";
        }

        // Get S-meter
        if (cmd.equals("\\get_level STRENGTH")) {
            return sMeter.get() + "\nRPRT 0";
        }

        // Unknown command
        return "RPRT -1";
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() {
        connected = false;
    }

    // Test control methods

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setSimulateError(boolean simulateError) {
        this.simulateError = simulateError;
    }

    public void setCommandDelay(long delayMs) {
        this.commandDelay = delayMs;
    }

    public long getFrequency() {
        return frequency.get();
    }

    public String getMode() {
        return mode.get();
    }

    public boolean isPTT() {
        return ptt.get();
    }

    public void setSMeter(int value) {
        sMeter.set(value);
    }
}
