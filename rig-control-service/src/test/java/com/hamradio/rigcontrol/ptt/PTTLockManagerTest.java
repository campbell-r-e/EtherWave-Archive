package com.hamradio.rigcontrol.ptt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PTTLockManager Tests")
class PTTLockManagerTest {

    private PTTLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new PTTLockManager();
    }

    @Test
    @DisplayName("acquirePTT - No Active PTT - Success")
    void acquirePTT_noActivePTT_success() {
        // Act
        boolean acquired = lockManager.acquirePTT("Client1");

        // Assert
        assertThat(acquired).isTrue();
        assertThat(lockManager.isPTTActive()).isTrue();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("acquirePTT - PTT Already Held by Another Client - Denied")
    void acquirePTT_pttHeldByAnother_denied() {
        // Arrange
        lockManager.acquirePTT("Client1");

        // Act
        boolean acquired = lockManager.acquirePTT("Client2");

        // Assert
        assertThat(acquired).isFalse();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("acquirePTT - Client Already Owns PTT - Success")
    void acquirePTT_clientAlreadyOwnsPTT_success() {
        // Arrange
        lockManager.acquirePTT("Client1");

        // Act
        boolean acquired = lockManager.acquirePTT("Client1");

        // Assert
        assertThat(acquired).isTrue();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("releasePTT - Owner Releases - Success")
    void releasePTT_ownerReleases_success() {
        // Arrange
        lockManager.acquirePTT("Client1");

        // Act
        boolean released = lockManager.releasePTT("Client1");

        // Assert
        assertThat(released).isTrue();
        assertThat(lockManager.isPTTActive()).isFalse();
        assertThat(lockManager.getPttOwner()).isNull();
    }

    @Test
    @DisplayName("releasePTT - Non-Owner Tries to Release - Denied")
    void releasePTT_nonOwnerTriesToRelease_denied() {
        // Arrange
        lockManager.acquirePTT("Client1");

        // Act
        boolean released = lockManager.releasePTT("Client2");

        // Assert
        assertThat(released).isFalse();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("forceReleasePTT - Disconnected Client - PTT Released")
    void forceReleasePTT_disconnectedClient_pttReleased() {
        // Arrange
        lockManager.acquirePTT("Client1");

        // Act
        lockManager.forceReleasePTT("Client1");

        // Assert
        assertThat(lockManager.isPTTActive()).isFalse();
        assertThat(lockManager.getPttOwner()).isNull();
    }

    @Test
    @DisplayName("forceReleasePTT - Wrong Client - No Effect")
    void forceReleasePTT_wrongClient_noEffect() {
        // Arrange
        lockManager.acquirePTT("Client1");

        // Act
        lockManager.forceReleasePTT("Client2");

        // Assert
        assertThat(lockManager.isPTTActive()).isTrue();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("clientOwnsPTT - Client Owns PTT - True")
    void clientOwnsPTT_clientOwnsPTT_true() {
        // Arrange
        lockManager.acquirePTT("Client1");

        // Act & Assert
        assertThat(lockManager.clientOwnsPTT("Client1")).isTrue();
        assertThat(lockManager.clientOwnsPTT("Client2")).isFalse();
    }

    @Test
    @DisplayName("getPTTDuration - PTT Held - Returns Duration")
    void getPTTDuration_pttHeld_returnsDuration() throws InterruptedException {
        // Arrange
        lockManager.acquirePTT("Client1");
        Thread.sleep(50);

        // Act
        long duration = lockManager.getPTTDuration();

        // Assert
        assertThat(duration).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Concurrent Access - Multiple Clients - Thread Safe")
    void concurrentAccess_multipleClients_threadSafe() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        // Act - Multiple threads try to acquire PTT simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = lockManager.acquirePTT("Client" + index);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - Only one thread should have succeeded
        long successCount = java.util.stream.IntStream.range(0, threadCount)
                .filter(i -> results[i])
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(lockManager.isPTTActive()).isTrue();
    }
}
