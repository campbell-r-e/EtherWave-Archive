package com.hamradio.logbook.security;

import com.hamradio.logbook.util.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JWT Util Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        // Set secret key for testing
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-token-generation-minimum-256-bits");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000); // 1 hour

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();
    }

    // ==================== TOKEN GENERATION TESTS ====================

    @Test
    @DisplayName("generateToken - Valid UserDetails - Returns Token")
    void generateToken_validUserDetails_returnsToken() {
        // Act
        String token = jwtUtil.generateToken(userDetails);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("generateToken - Different Users - Returns Different Tokens")
    void generateToken_differentUsers_returnsDifferentTokens() {
        // Arrange
        UserDetails user1 = User.builder()
                .username("user1")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        UserDetails user2 = User.builder()
                .username("user2")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        // Act
        String token1 = jwtUtil.generateToken(user1);
        String token2 = jwtUtil.generateToken(user2);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    // ==================== USERNAME EXTRACTION TESTS ====================

    @Test
    @DisplayName("getUsernameFromToken - Valid Token - Returns Username")
    void getUsernameFromToken_validToken_returnsUsername() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        String username = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUsernameFromToken - Invalid Token - Throws Exception")
    void getUsernameFromToken_invalidToken_throwsException() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.getUsernameFromToken(invalidToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    // ==================== EXPIRATION TESTS ====================

    @Test
    @DisplayName("getExpirationDateFromToken - Valid Token - Returns Future Date")
    void getExpirationDateFromToken_validToken_returnsFutureDate() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        Date expirationDate = jwtUtil.getExpirationDateFromToken(token);

        // Assert
        assertThat(expirationDate).isAfter(new Date());
    }

    @Test
    @DisplayName("isTokenExpired - Fresh Token - Returns False")
    void isTokenExpired_freshToken_returnsFalse() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Assert
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired - Expired Token - Returns True")
    void isTokenExpired_expiredToken_returnsTrue() {
        // Arrange
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1000); // Expired immediately
        String token = jwtUtil.generateToken(userDetails);

        // Give it a moment to actually expire
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act & Assert - Should throw ExpiredJwtException when checking
        assertThatThrownBy(() -> jwtUtil.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ==================== TOKEN VALIDATION TESTS ====================

    @Test
    @DisplayName("validateToken - Valid Token and Matching User - Returns True")
    void validateToken_validTokenAndMatchingUser_returnsTrue() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateToken - Valid Token but Different User - Returns False")
    void validateToken_validTokenButDifferentUser_returnsFalse() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        // Act
        boolean isValid = jwtUtil.validateToken(token, differentUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken - Malformed Token - Returns False")
    void validateToken_malformedToken_returnsFalse() {
        // Arrange
        String malformedToken = "malformed.token.here";

        // Act
        boolean isValid = jwtUtil.validateToken(malformedToken, userDetails);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken - Empty Token - Returns False")
    void validateToken_emptyToken_returnsFalse() {
        // Act
        boolean isValid = jwtUtil.validateToken("", userDetails);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken - Null Token - Returns False")
    void validateToken_nullToken_returnsFalse() {
        // Act
        boolean isValid = jwtUtil.validateToken(null, userDetails);

        // Assert
        assertThat(isValid).isFalse();
    }

    // ==================== CLAIMS EXTRACTION TESTS ====================

    @Test
    @DisplayName("getAllClaimsFromToken - Valid Token - Returns Claims")
    void getAllClaimsFromToken_validToken_returnsClaims() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        Claims claims = jwtUtil.getAllClaimsFromToken(token);

        // Assert
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("getAllClaimsFromToken - Tampered Token - Throws SignatureException")
    void getAllClaimsFromToken_tamperedToken_throwsSignatureException() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);
        String tamperedToken = token.substring(0, token.length() - 10) + "TAMPERED00";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.getAllClaimsFromToken(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }

    // ==================== TOKEN REFRESH TESTS ====================

    @Test
    @DisplayName("canTokenBeRefreshed - Fresh Token - Returns True")
    void canTokenBeRefreshed_freshToken_returnsTrue() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        boolean canRefresh = jwtUtil.canTokenBeRefreshed(token);

        // Assert
        assertThat(canRefresh).isTrue();
    }

    @Test
    @DisplayName("refreshToken - Valid Token - Returns New Token")
    void refreshToken_validToken_returnsNewToken() {
        // Arrange
        String originalToken = jwtUtil.generateToken(userDetails);

        // Wait a moment to ensure different timestamp
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        String refreshedToken = jwtUtil.refreshToken(originalToken);

        // Assert
        assertThat(refreshedToken).isNotNull();
        assertThat(refreshedToken).isNotEqualTo(originalToken);
        assertThat(jwtUtil.getUsernameFromToken(refreshedToken)).isEqualTo("testuser");
    }

    // ==================== CUSTOM CLAIMS TESTS ====================

    @Test
    @DisplayName("generateTokenWithClaims - Custom Claims - Includes Custom Claims")
    void generateTokenWithClaims_customClaims_includesCustomClaims() {
        // Arrange
        String userId = "123";
        String role = "ROLE_ADMIN";

        // Act
        String token = jwtUtil.generateTokenWithClaims(userDetails, userId, role);

        // Assert
        assertThat(token).isNotNull();
        Claims claims = jwtUtil.getAllClaimsFromToken(token);
        assertThat(claims.get("userId")).isEqualTo(userId);
        assertThat(claims.get("role")).isEqualTo(role);
    }

    // ==================== TOKEN PARSING EDGE CASES ====================

    @Test
    @DisplayName("getUsernameFromToken - Token with Special Characters - Handles Correctly")
    void getUsernameFromToken_tokenWithSpecialCharacters_handlesCorrectly() {
        // Arrange
        UserDetails specialUser = User.builder()
                .username("user@example.com")
                .password("password")
                .authorities(new ArrayList<>())
                .build();

        String token = jwtUtil.generateToken(specialUser);

        // Act
        String username = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertThat(username).isEqualTo("user@example.com");
    }

    // ==================== SECURITY TESTS ====================

    @Test
    @DisplayName("generateToken - Same User Multiple Times - Returns Different Tokens")
    void generateToken_sameUserMultipleTimes_returnsDifferentTokens() {
        // Act
        String token1 = jwtUtil.generateToken(userDetails);

        // Wait to ensure different timestamp
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String token2 = jwtUtil.generateToken(userDetails);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("validateToken - Token from Different Secret - Returns False")
    void validateToken_tokenFromDifferentSecret_returnsFalse() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Create new JwtUtil with different secret
        JwtUtil differentJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(differentJwtUtil, "secret", "different-secret-key-for-jwt-token-generation-minimum-256-bits");
        ReflectionTestUtils.setField(differentJwtUtil, "jwtExpirationMs", 3600000);

        // Act
        boolean isValid = differentJwtUtil.validateToken(token, userDetails);

        // Assert
        assertThat(isValid).isFalse();
    }

    // ==================== ISSUED AT TESTS ====================

    @Test
    @DisplayName("getIssuedAtDateFromToken - Valid Token - Returns Past Date")
    void getIssuedAtDateFromToken_validToken_returnsPastDate() {
        // Arrange
        String token = jwtUtil.generateToken(userDetails);

        // Act
        Date issuedAt = jwtUtil.getIssuedAtDateFromToken(token);

        // Assert
        assertThat(issuedAt).isBeforeOrEqualTo(new Date());
    }

    // ==================== TOKEN STRUCTURE TESTS ====================

    @Test
    @DisplayName("generateToken - Token Structure - Has Correct Format")
    void generateToken_tokenStructure_hasCorrectFormat() {
        // Act
        String token = jwtUtil.generateToken(userDetails);

        // Assert
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3); // Header, Payload, Signature
        assertThat(parts[0]).isNotEmpty(); // Header
        assertThat(parts[1]).isNotEmpty(); // Payload
        assertThat(parts[2]).isNotEmpty(); // Signature
    }
}
