package com.hamradio.rigcontrol.config;

import com.hamradio.rigcontrol.websocket.RigCommandHandler;
import com.hamradio.rigcontrol.websocket.RigEventsHandler;
import com.hamradio.rigcontrol.websocket.RigStatusHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for rig control.
 * Registers three WebSocket endpoints:
 *
 * 1. /ws/rig/command - Bidirectional: clients send commands, receive responses
 * 2. /ws/rig/status  - Broadcast: server sends status updates every 100ms
 * 3. /ws/rig/events  - Broadcast: server sends event notifications
 *
 * All endpoints require an API key when rig.api.keys is configured.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final RigCommandHandler commandHandler;
    private final RigStatusHandler statusHandler;
    private final RigEventsHandler eventsHandler;
    private final RigApiKeyHandshakeInterceptor apiKeyInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Command endpoint - bidirectional
        registry.addHandler(commandHandler, "/ws/rig/command")
                .addInterceptors(apiKeyInterceptor)
                .setAllowedOriginPatterns("*");

        // Status broadcast endpoint - one-way (server -> clients)
        registry.addHandler(statusHandler, "/ws/rig/status")
                .addInterceptors(apiKeyInterceptor)
                .setAllowedOriginPatterns("*");

        // Events broadcast endpoint - one-way (server -> clients)
        registry.addHandler(eventsHandler, "/ws/rig/events")
                .addInterceptors(apiKeyInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
