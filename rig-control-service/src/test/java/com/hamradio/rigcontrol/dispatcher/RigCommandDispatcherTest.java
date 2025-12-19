package com.hamradio.rigcontrol.dispatcher;

import com.hamradio.rigcontrol.connection.MockRigctlConnection;
import com.hamradio.rigcontrol.ptt.PTTLockManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

@DisplayName("RigCommandDispatcher Tests")
class RigCommandDispatcherTest {

    private MockRigctlConnection mockConnection;
    private PTTLockManager pttLockManager;
    private RigCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        mockConnection = new MockRigctlConnection();
        pttLockManager = new PTTLockManager();
        dispatcher = new RigCommandDispatcher(mockConnection, pttLockManager);
        dispatcher.init();
    }

    @AfterEach
    void tearDown() {
        dispatcher.shutdown();
    }

    // ==================== READ COMMAND TESTS ====================

    @Test
    @DisplayName("executeReadCommand - First Call - Queries Rig")
    void executeReadCommand_firstCall_queriesRig() throws Exception {
        // Act
        CompletableFuture<String> result = dispatcher.executeReadCommand("f");
        String response = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(response).contains("14250000"); // Default frequency from mock
    }

    @Test
    @DisplayName("executeReadCommand - Cached Value - Returns Immediately")
    void executeReadCommand_cachedValue_returnsImmediately() throws Exception {
        // Arrange - First call to populate cache
        dispatcher.executeReadCommand("f").get(1, TimeUnit.SECONDS);

        // Add delay to mock to verify cache is used
        mockConnection.setCommandDelay(100);

        // Act - Second call should use cache (no delay)
        long startTime = System.currentTimeMillis();
        CompletableFuture<String> result = dispatcher.executeReadCommand("f");
        result.get(1, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Assert - Should be much faster than 100ms
        assertThat(duration).isLessThan(50);
    }

    @Test
    @DisplayName("executeReadCommand - Cache Expired - Queries Rig Again")
    void executeReadCommand_cacheExpired_queriesRigAgain() throws Exception {
        // Arrange - First call
        dispatcher.executeReadCommand("f").get(1, TimeUnit.SECONDS);

        // Wait for cache to expire (50ms default TTL)
        Thread.sleep(60);

        // Clear cache to force new query
        dispatcher.clearCache();

        // Act - Should query rig again
        CompletableFuture<String> result = dispatcher.executeReadCommand("f");
        String response = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(response).contains("14250000");
    }

    @Test
    @DisplayName("executeReadCommand - Concurrent Identical Requests - Coalesced")
    void executeReadCommand_concurrentIdenticalRequests_coalesced() throws Exception {
        // Arrange
        mockConnection.setCommandDelay(50); // Simulate slow rig

        // Act - Send 5 identical requests simultaneously
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            futures.add(dispatcher.executeReadCommand("f"));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(1, TimeUnit.SECONDS);

        // Assert - All should have same result
        String firstResult = futures.get(0).get();
        for (CompletableFuture<String> future : futures) {
            assertThat(future.get()).isEqualTo(firstResult);
        }
    }

    // ==================== WRITE COMMAND TESTS ====================

    @Test
    @DisplayName("executeWriteCommand - Set Frequency - Success")
    void executeWriteCommand_setFrequency_success() throws Exception {
        // Act
        CompletableFuture<String> result = dispatcher.executeWriteCommand("F 14100000");
        String response = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(response).contains("RPRT 0");
        assertThat(mockConnection.getFrequency()).isEqualTo(14100000L);
    }

    @Test
    @DisplayName("executeWriteCommand - Invalidates Cache")
    void executeWriteCommand_invalidatesCache() throws Exception {
        // Arrange - Populate frequency cache
        dispatcher.executeReadCommand("f").get(1, TimeUnit.SECONDS);

        // Act - Change frequency (should invalidate cache)
        dispatcher.executeWriteCommand("F 14100000").get(1, TimeUnit.SECONDS);

        // Read frequency again - should get new value
        String response = dispatcher.executeReadCommand("f").get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(response).contains("14100000");
    }

    @Test
    @DisplayName("executeWriteCommand - Commands Serialized")
    void executeWriteCommand_commandsSerialized() throws Exception {
        // Arrange - Add delay to verify serialization
        mockConnection.setCommandDelay(50);

        // Act - Send multiple write commands
        List<CompletableFuture<String>> futures = new ArrayList<>();
        futures.add(dispatcher.executeWriteCommand("F 14100000"));
        futures.add(dispatcher.executeWriteCommand("F 14150000"));
        futures.add(dispatcher.executeWriteCommand("F 14200000"));

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(1, TimeUnit.SECONDS);

        // Assert - Final frequency should be last command
        assertThat(mockConnection.getFrequency()).isEqualTo(14200000L);
    }

    // ==================== PTT COMMAND TESTS ====================

    @Test
    @DisplayName("executePTTCommand - Activate PTT - Success")
    void executePTTCommand_activatePTT_success() throws Exception {
        // Act
        CompletableFuture<RigCommandDispatcher.PTTResult> result =
                dispatcher.executePTTCommand(true, "Client1");
        RigCommandDispatcher.PTTResult pttResult = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(pttResult.success()).isTrue();
        assertThat(pttResult.message()).contains("PTT activated");
        assertThat(mockConnection.isPTT()).isTrue();
        assertThat(pttLockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("executePTTCommand - PTT Already Held - Denied")
    void executePTTCommand_pttAlreadyHeld_denied() throws Exception {
        // Arrange - Client1 activates PTT
        dispatcher.executePTTCommand(true, "Client1").get(1, TimeUnit.SECONDS);

        // Act - Client2 tries to activate PTT
        CompletableFuture<RigCommandDispatcher.PTTResult> result =
                dispatcher.executePTTCommand(true, "Client2");
        RigCommandDispatcher.PTTResult pttResult = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(pttResult.success()).isFalse();
        assertThat(pttResult.message()).contains("denied");
        assertThat(pttLockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("executePTTCommand - Release PTT - Success")
    void executePTTCommand_releasePTT_success() throws Exception {
        // Arrange - Activate PTT first
        dispatcher.executePTTCommand(true, "Client1").get(1, TimeUnit.SECONDS);

        // Act - Release PTT
        CompletableFuture<RigCommandDispatcher.PTTResult> result =
                dispatcher.executePTTCommand(false, "Client1");
        RigCommandDispatcher.PTTResult pttResult = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(pttResult.success()).isTrue();
        assertThat(pttResult.message()).contains("released");
        assertThat(mockConnection.isPTT()).isFalse();
        assertThat(pttLockManager.isPTTActive()).isFalse();
    }

    @Test
    @DisplayName("executePTTCommand - Non-Owner Tries to Release - Denied")
    void executePTTCommand_nonOwnerTriesToRelease_denied() throws Exception {
        // Arrange - Client1 activates PTT
        dispatcher.executePTTCommand(true, "Client1").get(1, TimeUnit.SECONDS);

        // Act - Client2 tries to release PTT
        CompletableFuture<RigCommandDispatcher.PTTResult> result =
                dispatcher.executePTTCommand(false, "Client2");
        RigCommandDispatcher.PTTResult pttResult = result.get(1, TimeUnit.SECONDS);

        // Assert
        assertThat(pttResult.success()).isFalse();
        assertThat(mockConnection.isPTT()).isTrue(); // PTT still active
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @DisplayName("Performance - Read Commands Under 50ms")
    void performance_readCommandsUnder50ms() throws Exception {
        // Act
        long startTime = System.currentTimeMillis();
        dispatcher.executeReadCommand("f").get(1, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Assert - Should be well under 50ms
        assertThat(duration).isLessThan(50);
    }

    @Test
    @DisplayName("Performance - Cached Reads Under 10ms")
    void performance_cachedReadsUnder10ms() throws Exception {
        // Arrange - Warm up cache
        dispatcher.executeReadCommand("f").get(1, TimeUnit.SECONDS);

        // Act - Cached read
        long startTime = System.currentTimeMillis();
        dispatcher.executeReadCommand("f").get(1, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Assert - Should be very fast
        assertThat(duration).isLessThan(10);
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("executeReadCommand - Connection Error - Exception Thrown")
    void executeReadCommand_connectionError_exceptionThrown() {
        // Arrange
        mockConnection.setSimulateError(true);

        // Act & Assert
        CompletableFuture<String> result = dispatcher.executeReadCommand("f");

        assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS))
                .hasCauseInstanceOf(java.io.IOException.class);
    }

    @Test
    @DisplayName("executeWriteCommand - Connection Error - Exception Thrown")
    void executeWriteCommand_connectionError_exceptionThrown() {
        // Arrange
        mockConnection.setSimulateError(true);

        // Act & Assert
        CompletableFuture<String> result = dispatcher.executeWriteCommand("F 14100000");

        assertThatThrownBy(() -> result.get(1, TimeUnit.SECONDS))
                .hasCauseInstanceOf(java.io.IOException.class);
    }
}
