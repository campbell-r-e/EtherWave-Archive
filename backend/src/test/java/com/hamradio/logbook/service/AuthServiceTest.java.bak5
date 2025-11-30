package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.UserRepository;
import com.hamradio.logbook.service.security.AuthService;
import com.hamradio.logbook.testutil.TestDataBuilder;
import com.hamradio.logbook.util.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser().id(1L).build();
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("register - Valid User - Creates Successfully")
    void register_validUser_createsSuccessfully() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.register(testUser, "password123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - Duplicate Username - Throws Exception")
    void register_duplicateUsername_throwsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(testUser, "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - Duplicate Email - Throws Exception")
    void register_duplicateEmail_throwsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(testUser, "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - Weak Password - Throws Exception")
    void register_weakPassword_throwsException() {
        // Act & Assert
        assertThatThrownBy(() -> authService.register(testUser, "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - Invalid Callsign Format - Throws Exception")
    void register_invalidCallsign_throwsException() {
        // Arrange
        User invalidUser = TestDataBuilder.aValidUser().callsign("INVALID!!!").build();

        // Act & Assert
        assertThatThrownBy(() -> authService.register(invalidUser, "password123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid callsign");

        verify(userRepository, never()).save(any());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("login - Valid Credentials - Returns JWT Token")
    void login_validCredentials_returnsToken() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtUtil.generateToken("testuser")).thenReturn("jwt-token-123");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        String token = authService.login("testuser", "password123");

        // Assert
        assertThat(token).isEqualTo("jwt-token-123");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken("testuser");
    }

    @Test
    @DisplayName("login - Invalid Credentials - Throws Exception")
    void login_invalidCredentials_throwsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login("testuser", "wrongpassword"))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    @DisplayName("login - Inactive User - Throws Exception")
    void login_inactiveUser_throwsException() {
        // Arrange
        User inactiveUser = TestDataBuilder.aValidUser().isActive(false).build();
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(inactiveUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.login("testuser", "password123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive");

        verify(jwtUtil, never()).generateToken(any());
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    @Test
    @DisplayName("validateToken - Valid Token - Returns True")
    void validateToken_validToken_returnsTrue() {
        // Arrange
        when(jwtUtil.validateToken("valid-token", "testuser")).thenReturn(true);

        // Act
        boolean isValid = authService.validateToken("valid-token", "testuser");

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateToken - Expired Token - Returns False")
    void validateToken_expiredToken_returnsFalse() {
        // Arrange
        when(jwtUtil.validateToken("expired-token", "testuser")).thenReturn(false);

        // Act
        boolean isValid = authService.validateToken("expired-token", "testuser");

        // Assert
        assertThat(isValid).isFalse();
    }

    // ==================== PASSWORD CHANGE TESTS ====================

    @Test
    @DisplayName("changePassword - Valid Old Password - Changes Successfully")
    void changePassword_validOldPassword_changesSuccessfully() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$new");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.changePassword(1L, "oldPassword", "newPassword123");

        // Assert
        verify(passwordEncoder).matches("oldPassword", testUser.getPassword());
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword - Wrong Old Password - Throws Exception")
    void changePassword_wrongOldPassword_throwsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.changePassword(1L, "wrongPassword", "newPassword123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository, never()).save(any());
    }

    // ==================== USER RETRIEVAL TESTS ====================

    @Test
    @DisplayName("getUserByUsername - Existing User - Returns User")
    void getUserByUsername_existingUser_returnsUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = authService.getUserByUsername("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserByUsername - Non-existent User - Returns Empty")
    void getUserByUsername_nonExistentUser_returnsEmpty() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = authService.getUserByUsername("unknown");

        // Assert
        assertThat(result).isEmpty();
    }
}
