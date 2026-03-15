package com.hamradio.rigcontrol.ptt;

import com.hamradio.rigcontrol.websocket.RigEventsHandler;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PTTLockManager Tests")
class PTTLockManagerTest {

    @Mock
    private RigEventsHandler eventsHandler;

    private PTTLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new PTTLockManager(eventsHandler);
        // Default: safety timeout disabled in unit tests (set per-test when needed)
        ReflectionTestUtils.setField(lockManager, "safetyTimeoutSeconds", 0);
    }

    @AfterEach
    void tearDown() {
        lockManager.shutdown();
    }

    // -------------------------------------------------------------------------
    // Basic PTT acquire/release (existing tests, updated for new constructor)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("acquirePTT - No Active PTT - Success")
    void acquirePTT_noActivePTT_success() {
        boolean acquired = lockManager.acquirePTT("Client1");

        assertThat(acquired).isTrue();
        assertThat(lockManager.isPTTActive()).isTrue();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("acquirePTT - PTT Already Held by Another Client - Denied")
    void acquirePTT_pttHeldByAnother_denied() {
        lockManager.acquirePTT("Client1");

        boolean acquired = lockManager.acquirePTT("Client2");

        assertThat(acquired).isFalse();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("acquirePTT - Client Already Owns PTT - Success")
    void acquirePTT_clientAlreadyOwnsPTT_success() {
        lockManager.acquirePTT("Client1");

        boolean acquired = lockManager.acquirePTT("Client1");

        assertThat(acquired).isTrue();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("releasePTT - Owner Releases - Success")
    void releasePTT_ownerReleases_success() {
        lockManager.acquirePTT("Client1");

        boolean released = lockManager.releasePTT("Client1");

        assertThat(released).isTrue();
        assertThat(lockManager.isPTTActive()).isFalse();
        assertThat(lockManager.getPttOwner()).isNull();
    }

    @Test
    @DisplayName("releasePTT - Non-Owner Tries to Release - Denied")
    void releasePTT_nonOwnerTriesToRelease_denied() {
        lockManager.acquirePTT("Client1");

        boolean released = lockManager.releasePTT("Client2");

        assertThat(released).isFalse();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("forceReleasePTT - Disconnected Client - PTT Released")
    void forceReleasePTT_disconnectedClient_pttReleased() {
        lockManager.acquirePTT("Client1");

        lockManager.forceReleasePTT("Client1");

        assertThat(lockManager.isPTTActive()).isFalse();
        assertThat(lockManager.getPttOwner()).isNull();
    }

    @Test
    @DisplayName("forceReleasePTT - Wrong Client - No Effect")
    void forceReleasePTT_wrongClient_noEffect() {
        lockManager.acquirePTT("Client1");

        lockManager.forceReleasePTT("Client2");

        assertThat(lockManager.isPTTActive()).isTrue();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client1");
    }

    @Test
    @DisplayName("clientOwnsPTT - Client Owns PTT - True")
    void clientOwnsPTT_clientOwnsPTT_true() {
        lockManager.acquirePTT("Client1");

        assertThat(lockManager.clientOwnsPTT("Client1")).isTrue();
        assertThat(lockManager.clientOwnsPTT("Client2")).isFalse();
    }

    @Test
    @DisplayName("getPTTDuration - PTT Held - Returns Duration")
    void getPTTDuration_pttHeld_returnsDuration() throws InterruptedException {
        lockManager.acquirePTT("Client1");
        Thread.sleep(50);

        long duration = lockManager.getPTTDuration();

        assertThat(duration).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Concurrent Access - Multiple Clients - Thread Safe")
    void concurrentAccess_multipleClients_threadSafe() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> results[index] = lockManager.acquirePTT("Client" + index));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long successCount = java.util.stream.IntStream.range(0, threadCount)
                .filter(i -> results[i])
                .count();

        assertThat(successCount).isEqualTo(1);
        assertThat(lockManager.isPTTActive()).isTrue();
    }

    // -------------------------------------------------------------------------
    // PTT safety timeout
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("safetyTimeout - Timeout fires - PTT released and event broadcast")
    void safetyTimeout_timeoutFires_pttReleasedAndEventBroadcast() {
        // Use a 1-second timeout for test speed
        ReflectionTestUtils.setField(lockManager, "safetyTimeoutSeconds", 1);

        lockManager.acquirePTT("Client1");
        assertThat(lockManager.isPTTActive()).isTrue();

        // Wait for timeout to fire (up to 3s)
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .until(() -> !lockManager.isPTTActive());

        assertThat(lockManager.isPTTActive()).isFalse();
        assertThat(lockManager.getPttOwner()).isNull();

        // Verify ptt_timeout event was broadcast
        ArgumentCaptor<RigEventsHandler.RigEvent> eventCaptor =
                ArgumentCaptor.forClass(RigEventsHandler.RigEvent.class);
        verify(eventsHandler, atLeastOnce()).broadcastEvent(eventCaptor.capture());

        boolean hasTimeoutEvent = eventCaptor.getAllValues().stream()
                .anyMatch(e -> "ptt_timeout".equals(e.eventType()));
        assertThat(hasTimeoutEvent).isTrue();
    }

    @Test
    @DisplayName("safetyTimeout - Release before timeout - No timeout event")
    void safetyTimeout_releaseBeforeTimeout_noTimeoutEvent() throws InterruptedException {
        ReflectionTestUtils.setField(lockManager, "safetyTimeoutSeconds", 2);

        lockManager.acquirePTT("Client1");
        Thread.sleep(100);
        lockManager.releasePTT("Client1"); // Release before timeout

        Thread.sleep(200); // Short wait — not enough to trigger a 2-second timeout

        assertThat(lockManager.isPTTActive()).isFalse();
        verify(eventsHandler, never()).broadcastEvent(
                argThat(e -> "ptt_timeout".equals(e.eventType())));
    }

    @Test
    @DisplayName("safetyTimeout - ForceRelease before timeout - No timeout event")
    void safetyTimeout_forceReleaseBeforeTimeout_noTimeoutEvent() throws InterruptedException {
        ReflectionTestUtils.setField(lockManager, "safetyTimeoutSeconds", 2);

        lockManager.acquirePTT("Client1");
        Thread.sleep(100);
        lockManager.forceReleasePTT("Client1");

        Thread.sleep(200);

        assertThat(lockManager.isPTTActive()).isFalse();
        verify(eventsHandler, never()).broadcastEvent(
                argThat(e -> "ptt_timeout".equals(e.eventType())));
    }

    @Test
    @DisplayName("safetyTimeout - Disabled (0 seconds) - No timeout fired")
    void safetyTimeout_disabled_noTimeoutFired() throws InterruptedException {
        // safetyTimeoutSeconds = 0 (set in setUp)
        lockManager.acquirePTT("Client1");

        Thread.sleep(200);

        // PTT should still be held — no timeout
        assertThat(lockManager.isPTTActive()).isTrue();
        verify(eventsHandler, never()).broadcastEvent(any());

        lockManager.releasePTT("Client1");
    }

    @Test
    @DisplayName("safetyTimeout - Re-acquire after timeout - Timeout resets")
    void safetyTimeout_reacquireAfterTimeout_timeoutResets() throws InterruptedException {
        ReflectionTestUtils.setField(lockManager, "safetyTimeoutSeconds", 1);

        lockManager.acquirePTT("Client1");

        // Wait for timeout
        Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> !lockManager.isPTTActive());

        // Acquire again — should succeed and start a fresh timeout
        boolean acquired = lockManager.acquirePTT("Client2");
        assertThat(acquired).isTrue();
        assertThat(lockManager.getPttOwner()).isEqualTo("Client2");

        lockManager.releasePTT("Client2");
    }
}
