package com.hamradio.logbook.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.dto.auth.AuthResponse;
import com.hamradio.logbook.dto.auth.LoginRequest;
import com.hamradio.logbook.dto.auth.RegisterRequest;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.service.security.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hamradio.logbook.config.TestConfig;
import org.springframework.context.annotation.Import;

@Import(TestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("Auth Controller Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("POST /api/auth/register - Valid User - Returns 201 Created")
    void register_validUser_returns201() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "newuser",
                "newuser@example.com",
                "password123",
                "New User",
                "W1NEW",
                "FN42",
                null
        );

        Set<User.Role> roles = new HashSet<>();
        roles.add(User.Role.ROLE_USER);

        AuthResponse response = new AuthResponse(
                "mock-jwt-token",
                1L,
                "newuser",
                "newuser@example.com",
                "W1NEW",
                "New User",
                roles
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.callsign").value("W1NEW"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Duplicate Username - Returns 400 Bad Request")
    void register_duplicateUsername_returns400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "existinguser",
                "newuser@example.com",
                "password123",
                "New User",
                "W1NEW",
                null,
                null
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username is already taken"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Invalid Email - Returns 400 Bad Request")
    void register_invalidEmail_returns400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "newuser",
                "invalid-email",
                "password123",
                null,
                null,
                null,
                null
        );

        // Act & Assert - validation should fail before reaching service
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/register - Short Password - Returns 400 Bad Request")
    void register_shortPassword_returns400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "newuser",
                "newuser@example.com",
                "short",
                null,
                null,
                null,
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("POST /api/auth/login - Valid Credentials - Returns 200 OK with Token")
    void login_validCredentials_returns200WithToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "password123");

        Set<User.Role> roles = new HashSet<>();
        roles.add(User.Role.ROLE_USER);

        AuthResponse response = new AuthResponse(
                "mock-jwt-token",
                1L,
                "testuser",
                "testuser@example.com",
                "W1TEST",
                "Test User",
                roles
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid Credentials - Returns 401 Unauthorized")
    void login_invalidCredentials_returns401() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Missing Username - Returns 400 Bad Request")
    void login_missingUsername_returns400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Missing Password - Returns 400 Bad Request")
    void login_missingPassword_returns400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/login - Login with Email - Returns 200 OK")
    void login_withEmail_returns200() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("testuser@example.com", "password123");

        Set<User.Role> roles = new HashSet<>();
        roles.add(User.Role.ROLE_USER);

        AuthResponse response = new AuthResponse(
                "mock-jwt-token",
                1L,
                "testuser",
                "testuser@example.com",
                "W1TEST",
                "Test User",
                roles
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));

        verify(authService).login(any(LoginRequest.class));
    }
}
