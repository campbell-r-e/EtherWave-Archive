package com.hamradio.rigcontrol.connection;

import java.io.IOException;

/**
 * Abstraction for rigctld communication.
 * Allows mocking in tests without requiring actual hardware.
 */
public interface RigctlConnection {

    /**
     * Send a command to rigctld and receive response
     * @param command The rigctl command (e.g., "f", "F 14250000")
     * @return The response from rigctld
     * @throws IOException if communication fails
     */
    String sendCommand(String command) throws IOException;

    /**
     * Test if connection to rigctld is available
     * @return true if rigctld is reachable
     */
    boolean isConnected();

    /**
     * Close the connection and release resources
     */
    void close();
}
