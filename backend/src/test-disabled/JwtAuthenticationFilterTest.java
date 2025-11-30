package com.hamradio.logbook.security;

import com.hamradio.logbook.config.JwtAuthenticationFilter;
import com.hamradio.logbook.util.security.JwtUtil;
import com.hamradio.logbook.service.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    // ==================== VALID TOKEN TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Valid Token - Sets Authentication")
    void doFilterInternal_validToken_setsAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("doFilterInternal - Valid Token - Calls UserDetailsService Once")
    void doFilterInternal_validToken_callsUserDetailsServiceOnce() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(userDetailsService, times(1)).loadUserByUsername("testuser");
        verify(jwtUtil, times(1)).validateToken(token, userDetails);
    }

    // ==================== NO TOKEN TESTS ====================

    @Test
    @DisplayName("doFilterInternal - No Authorization Header - Does Not Set Authentication")
    void doFilterInternal_noAuthorizationHeader_doesNotSetAuthentication() throws ServletException, IOException {
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - Empty Authorization Header - Does Not Set Authentication")
    void doFilterInternal_emptyAuthorizationHeader_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ==================== INVALID TOKEN TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Invalid Token - Does Not Set Authentication")
    void doFilterInternal_invalidToken_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("doFilterInternal - Malformed Token - Does Not Set Authentication")
    void doFilterInternal_malformedToken_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "malformed.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenThrow(new RuntimeException("Malformed JWT"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ==================== AUTHORIZATION HEADER FORMAT TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Header Without Bearer Prefix - Does Not Set Authentication")
    void doFilterInternal_headerWithoutBearerPrefix_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "InvalidPrefix token");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).getUsernameFromToken(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - Bearer with Lowercase - Does Not Set Authentication")
    void doFilterInternal_bearerWithLowercase_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        request.addHeader("Authorization", "bearer token");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("doFilterInternal - Bearer with Extra Spaces - Extracts Token Correctly")
    void doFilterInternal_bearerWithExtraSpaces_extractsTokenCorrectly() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer  " + token); // Extra space

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // Should handle extra spaces gracefully
    }

    // ==================== USER NOT FOUND TESTS ====================

    @Test
    @DisplayName("doFilterInternal - User Not Found - Does Not Set Authentication")
    void doFilterInternal_userNotFound_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("nonexistentuser");
        when(userDetailsService.loadUserByUsername("nonexistentuser"))
                .thenThrow(new RuntimeException("User not found"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ==================== ALREADY AUTHENTICATED TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Already Authenticated - Does Not Re-authenticate")
    void doFilterInternal_alreadyAuthenticated_doesNotReAuthenticate() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        // Manually set authentication
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // Should not call userDetailsService if already authenticated
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    // ==================== FILTER CHAIN TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Always Calls Filter Chain")
    void doFilterInternal_alwaysCallsFilterChain() throws ServletException, IOException {
        // Act - No token scenario
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Exception During Authentication - Still Calls Filter Chain")
    void doFilterInternal_exceptionDuringAuthentication_stillCallsFilterChain() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    // ==================== MULTIPLE REQUESTS TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Multiple Requests - Handles Independently")
    void doFilterInternal_multipleRequests_handlesIndependently() throws ServletException, IOException {
        // Arrange - First request with valid token
        String token1 = "valid.token.1";
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        request1.addHeader("Authorization", "Bearer " + token1);

        when(jwtUtil.getUsernameFromToken(token1)).thenReturn("user1");
        when(userDetailsService.loadUserByUsername("user1")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token1, userDetails)).thenReturn(true);

        // Act - First request
        jwtAuthenticationFilter.doFilterInternal(request1, response1, filterChain);

        // Clear context
        SecurityContextHolder.clearContext();

        // Arrange - Second request with no token
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        // Act - Second request
        jwtAuthenticationFilter.doFilterInternal(request2, response2, filterChain);

        // Assert
        verify(filterChain, times(2)).doFilter(any(), any());
    }

    // ==================== TOKEN EXTRACTION TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Token with Bearer Prefix - Extracts Correctly")
    void doFilterInternal_tokenWithBearerPrefix_extractsCorrectly() throws ServletException, IOException {
        // Arrange
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(jwtUtil).getUsernameFromToken(token);
    }

    // ==================== SECURITY CONTEXT TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Valid Token - Sets Correct Principal")
    void doFilterInternal_validToken_setsCorrectPrincipal() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("testuser");
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("doFilterInternal - Null Username from Token - Does Not Set Authentication")
    void doFilterInternal_nullUsernameFromToken_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - Empty Username from Token - Does Not Set Authentication")
    void doFilterInternal_emptyUsernameFromToken_doesNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
