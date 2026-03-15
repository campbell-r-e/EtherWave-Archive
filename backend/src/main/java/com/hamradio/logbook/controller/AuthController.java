package com.hamradio.logbook.controller;

import com.hamradio.logbook.dto.auth.AuthResponse;
import com.hamradio.logbook.dto.auth.LoginRequest;
import com.hamradio.logbook.dto.auth.RegisterRequest;
import com.hamradio.logbook.dto.auth.UserResponse;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.service.security.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = authService.login(loginRequest);
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
    }

    /**
     * Registration endpoint
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            AuthResponse authResponse = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = authService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            return ResponseEntity.ok(UserResponse.fromUser(currentUser));
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Logout endpoint (client-side token removal)
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout() {
        // JWT is stateless, so logout is handled client-side by removing the token
        // This endpoint exists for API consistency
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    // Helper classes for responses
    record ErrorResponse(String message) {}
    record MessageResponse(String message) {}
}
