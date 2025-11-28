package com.hamradio.rigcontrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Rig Control Service - Hamlib Integration Microservice
 *
 * This service runs locally on each client machine and:
 * - Communicates with Hamlib rigctld for rig control
 * - Provides REST API for frequency, mode, PTT status
 * - Optionally sends telemetry to the main backend
 */
@SpringBootApplication
@EnableScheduling
public class RigControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(RigControlApplication.class, args);
    }
}
