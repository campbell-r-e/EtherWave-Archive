package com.hamradio.logbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application for Ham Radio Logbook System
 *
 * Provides multi-station logging with rig control integration,
 * contest validation, and real-time updates via WebSocket.
 */
@SpringBootApplication
@EnableAsync
public class LogbookApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogbookApplication.class, args);
    }
}
