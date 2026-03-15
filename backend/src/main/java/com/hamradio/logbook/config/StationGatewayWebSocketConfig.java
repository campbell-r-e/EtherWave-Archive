package com.hamradio.logbook.config;

import com.hamradio.logbook.websocket.StationGatewayHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the raw WebSocket endpoint for the station gateway.
 *
 * Remote rig services connect to /ws/station-gateway to register themselves
 * so that backend users can control them via the cloud relay architecture.
 *
 * This configuration is separate from WebSocketConfig (STOMP/SockJS) to keep
 * the raw WebSocket endpoint independent of the STOMP message broker.
 * Using @EnableWebSocket alongside @EnableWebSocketMessageBroker is supported
 * by Spring — the two infrastructures co-exist cleanly.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class StationGatewayWebSocketConfig implements WebSocketConfigurer {

    private final StationGatewayHandler stationGatewayHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(stationGatewayHandler, "/ws/station-gateway")
                .setAllowedOriginPatterns("*");
    }
}
