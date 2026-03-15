package com.hamradio.logbook.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.Station;
import com.hamradio.logbook.repository.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StationGatewayHandler.
 * Covers registration, command forwarding, status forwarding, and disconnect cleanup.
 * No real WebSocket connections are used.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StationGatewayHandler Tests")
class StationGatewayHandlerTest {

    @Mock private StationGatewayRegistry registry;
    @Mock private StationRepository stationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private WebSocketSession session;

    private ObjectMapper objectMapper;
    private StationGatewayHandler handler;

    private static final Long STATION_ID = 42L;
    private static final String VALID_KEY = "super-secret";
    private static final String HASHED_KEY = "$2a$10$hashedvalue";
    private static final String SESSION_ID = "session-abc";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        handler = new StationGatewayHandler(registry, stationRepository, passwordEncoder,
                messagingTemplate, objectMapper);

        when(session.getId()).thenReturn(SESSION_ID);
        when(session.isOpen()).thenReturn(true);
        when(session.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9999));
    }

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registration - Valid credentials - Registers in registry and sends ack")
    void registration_validCredentials_registersAndSendsAck() throws Exception {
        Station station = buildRemoteStation();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(station));
        when(passwordEncoder.matches(VALID_KEY, HASHED_KEY)).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, registerMessage(STATION_ID, VALID_KEY));

        verify(registry).register(STATION_ID, session);
        verifySentJson(session, "registered");
    }

    @Test
    @DisplayName("registration - Unknown station - Sends error and closes session")
    void registration_unknownStation_sendsErrorAndCloses() throws Exception {
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.empty());

        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, registerMessage(STATION_ID, VALID_KEY));

        verify(registry, never()).register(any(), any());
        verifySentJson(session, "error");
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    @DisplayName("registration - Station not remote - Sends error and closes session")
    void registration_stationNotRemote_sendsErrorAndCloses() throws Exception {
        Station station = Station.builder()
                .id(STATION_ID).stationName("Local1").callsign("W1AW")
                .remoteStation(false).apiKeyHash(HASHED_KEY).build();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(station));

        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, registerMessage(STATION_ID, VALID_KEY));

        verify(registry, never()).register(any(), any());
        verifySentJson(session, "error");
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    @DisplayName("registration - Invalid API key - Sends error and closes session")
    void registration_invalidApiKey_sendsErrorAndCloses() throws Exception {
        Station station = buildRemoteStation();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(station));
        when(passwordEncoder.matches("wrong-key", HASHED_KEY)).thenReturn(false);

        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, registerMessage(STATION_ID, "wrong-key"));

        verify(registry, never()).register(any(), any());
        verifySentJson(session, "error");
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    // -------------------------------------------------------------------------
    // Status update forwarding
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("status update - Registered station - Forwarded to STOMP")
    void statusUpdate_registeredStation_forwardedToStomp() throws Exception {
        registerStation();

        String statusJson = "{\"type\":\"status\",\"stationId\":42,\"status\":{\"frequencyHz\":14250000}}";
        handler.handleMessage(session, new TextMessage(statusJson));

        verify(messagingTemplate).convertAndSend(eq("/topic/rig/status/" + STATION_ID), any(Object.class));
    }

    // -------------------------------------------------------------------------
    // Command response forwarding
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("command response - Has requestId - Completes request in registry")
    void commandResponse_hasRequestId_completesRegistryRequest() throws Exception {
        registerStation();

        String responseJson = "{\"type\":\"response\",\"id\":\"gw-1\",\"success\":true,\"result\":{}}";
        handler.handleMessage(session, new TextMessage(responseJson));

        verify(registry).completeRequest(eq("gw-1"), any());
    }

    // -------------------------------------------------------------------------
    // Disconnect cleanup
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("disconnect - Registered station - Unregisters from registry")
    void disconnect_registeredStation_unregistersFromRegistry() throws Exception {
        registerStation();

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(registry).unregister(STATION_ID);
    }

    @Test
    @DisplayName("disconnect - Unregistered session - No registry call")
    void disconnect_unregisteredSession_noRegistryCall() throws Exception {
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(registry, never()).unregister(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void registerStation() throws Exception {
        Station station = buildRemoteStation();
        when(stationRepository.findById(STATION_ID)).thenReturn(Optional.of(station));
        when(passwordEncoder.matches(VALID_KEY, HASHED_KEY)).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, registerMessage(STATION_ID, VALID_KEY));
    }

    private TextMessage registerMessage(Long stationId, String apiKey) throws Exception {
        return new TextMessage(objectMapper.writeValueAsString(
                java.util.Map.of("type", "register", "stationId", stationId, "apiKey", apiKey)
        ));
    }

    private Station buildRemoteStation() {
        return Station.builder()
                .id(STATION_ID).stationName("RemoteStation1").callsign("W1AW")
                .remoteStation(true).apiKeyHash(HASHED_KEY).build();
    }

    private void verifySentJson(WebSocketSession session, String expectedType) throws IOException {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());

        boolean found = captor.getAllValues().stream()
                .anyMatch(msg -> msg.getPayload().contains("\"" + expectedType + "\""));
        assertThat(found).as("Expected message with type '%s'", expectedType).isTrue();
    }
}
