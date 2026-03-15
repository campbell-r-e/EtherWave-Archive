package com.hamradio.rigcontrol.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates API key before a WebSocket handshake is accepted.
 *
 * The key can be supplied in either of two ways:
 *   - HTTP header:        X-Api-Key: <key>
 *   - Query parameter:    /ws/rig/command?apiKey=<key>
 *
 * If rig.api.keys is empty (default), all connections are accepted for
 * backward compatibility (local / trusted-network deployments).
 */
@Component
@Slf4j
public class RigApiKeyHandshakeInterceptor implements HandshakeInterceptor {

    @Value("${rig.api.keys:}")
    private String configuredKeys;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Set<String> allowedKeys = parseKeys(configuredKeys);

        // If no keys configured, allow all connections
        if (allowedKeys.isEmpty()) {
            return true;
        }

        // Check X-Api-Key header
        String headerKey = request.getHeaders().getFirst("X-Api-Key");
        if (headerKey != null && allowedKeys.contains(headerKey)) {
            log.debug("WS handshake accepted via X-Api-Key header");
            return true;
        }

        // Check ?apiKey= query parameter
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String queryKey = servletRequest.getServletRequest().getParameter("apiKey");
            if (queryKey != null && allowedKeys.contains(queryKey)) {
                log.debug("WS handshake accepted via apiKey query param");
                return true;
            }
        }

        log.warn("WS handshake rejected: missing or invalid API key from {}", request.getRemoteAddress());
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private Set<String> parseKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .collect(Collectors.toSet());
    }
}
