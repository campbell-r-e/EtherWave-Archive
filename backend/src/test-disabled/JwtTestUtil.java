package com.hamradio.logbook.testutil;

import com.hamradio.logbook.entity.User;

/**
 * Utility class for generating test JWTs
 * All methods are static for easy use in test classes
 */
public class JwtTestUtil {

    /**
     * Generate a valid JWT for testing
     * Returns a mock token for test purposes
     */
    public static String generateToken(String username) {
        // Return mock token for tests
        return "mock-jwt-token-" + username;
    }

    /**
     * Generate a valid JWT for a test user
     */
    public static String generateToken(User user) {
        return generateToken(user.getUsername());
    }

    /**
     * Generate an expired JWT for testing
     */
    public static String generateExpiredToken(String username) {
        // Return a mock expired token for tests
        return "expired-jwt-token-" + username;
    }

    /**
     * Generate an invalid JWT for testing
     */
    public static String generateInvalidToken() {
        return "invalid.jwt.token";
    }

    /**
     * Generate Authorization header with Bearer token
     */
    public static String bearerToken(String username) {
        return "Bearer " + generateToken(username);
    }
}
