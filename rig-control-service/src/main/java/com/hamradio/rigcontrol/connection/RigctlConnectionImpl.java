package com.hamradio.rigcontrol.connection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Production implementation of RigctlConnection.
 * Maintains a single persistent connection to rigctld.
 * Thread-safe for concurrent access.
 */
@Component
@Slf4j
public class RigctlConnectionImpl implements RigctlConnection {

    @Value("${rigctld.host:localhost}")
    private String rigctldHost;

    @Value("${rigctld.port:4532}")
    private int rigctldPort;

    @Value("${rigctld.timeout:1000}")
    private int socketTimeout;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ReentrantLock connectionLock = new ReentrantLock();

    /**
     * Send command to rigctld with automatic connection management.
     * Creates connection on first use, reconnects if connection lost.
     */
    @Override
    public String sendCommand(String command) throws IOException {
        connectionLock.lock();
        try {
            ensureConnected();

            // Send command
            out.println(command);
            out.flush();

            // Read response
            StringBuilder response = new StringBuilder();
            String line;

            // rigctld protocol: read until RPRT line or timeout
            socket.setSoTimeout(socketTimeout);

            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");

                // End of response indicators
                if (line.startsWith("RPRT")) {
                    break;
                }

                // For query commands (lowercase), first line is the answer
                if (command.length() > 0 && Character.isLowerCase(command.charAt(0))) {
                    // Read one more line to get RPRT status
                    String rprt = in.readLine();
                    if (rprt != null && rprt.startsWith("RPRT")) {
                        response.append(rprt).append("\n");
                    }
                    break;
                }
            }

            return response.toString().trim();

        } catch (IOException e) {
            log.warn("rigctld communication error: {}", e.getMessage());
            closeConnection();
            throw e;
        } finally {
            connectionLock.unlock();
        }
    }

    /**
     * Test connection by attempting to get frequency
     */
    @Override
    public boolean isConnected() {
        try {
            String response = sendCommand("f");
            return response != null && !response.contains("RPRT -");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Ensure connection is established, reconnect if needed
     */
    private void ensureConnected() throws IOException {
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            connect();
        }
    }

    /**
     * Establish connection to rigctld
     */
    private void connect() throws IOException {
        closeConnection();

        log.info("Connecting to rigctld at {}:{}", rigctldHost, rigctldPort);

        socket = new Socket(rigctldHost, rigctldPort);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true); // Disable Nagle for low latency
        socket.setSoTimeout(socketTimeout);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        log.info("Connected to rigctld successfully");
    }

    /**
     * Close connection and release resources
     */
    private void closeConnection() {
        if (in != null) {
            try { in.close(); } catch (IOException e) { /* ignore */ }
            in = null;
        }
        if (out != null) {
            out.close();
            out = null;
        }
        if (socket != null) {
            try { socket.close(); } catch (IOException e) { /* ignore */ }
            socket = null;
        }
    }

    @Override
    @PreDestroy
    public void close() {
        connectionLock.lock();
        try {
            log.info("Closing rigctld connection");
            closeConnection();
        } finally {
            connectionLock.unlock();
        }
    }
}
