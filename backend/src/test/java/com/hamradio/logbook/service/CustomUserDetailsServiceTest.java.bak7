package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.UserRepository;
import com.hamradio.logbook.service.security.CustomUserDetailsService;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private User inactiveUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataBuilder.aValidUser()
                .id(1L)
                .username("testuser")
                .password("$2a$10$encodedPassword")
                .email("testuser@example.com")
                .callsign("W1TEST")
                .role("USER")
                .isActive(true)
                .build();

        inactiveUser = TestDataBuilder.aValidUser()
                .id(2L)
                .username("inactive")
                .password("$2a$10$encodedPassword")
                .isActive(false)
                .build();

        adminUser = TestDataBuilder.aValidUser()
                .id(3L)
                .username("admin")
                .password("$2a$10$encodedPassword")
                .role("ADMIN")
                .isActive(true)
                .build();
    }

    // ==================== LOAD USER BY USERNAME TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Valid Active User - Returns UserDetails")
    void loadUserByUsername_validActiveUser_returnsUserDetails() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("loadUserByUsername - User Not Found - Throws UsernameNotFoundException")
    void loadUserByUsername_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("loadUserByUsername - Inactive User - Returns UserDetails with Disabled Flag")
    void loadUserByUsername_inactiveUser_returnsDisabledUserDetails() {
        // Arrange
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(inactiveUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("inactive");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("inactive");
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername - Case Sensitive - Exact Match Required")
    void loadUserByUsername_caseSensitive_exactMatchRequired() {
        // Arrange
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("TestUser"))
                .isInstanceOf(UsernameNotFoundException.class);

        // But lowercase works
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");
        assertThat(userDetails).isNotNull();
    }

    // ==================== AUTHORITIES/ROLES TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Regular User - Has USER Authority")
    void loadUserByUsername_regularUser_hasUserAuthority() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).isNotEmpty();
        assertThat(authorities).anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"));
    }

    @Test
    @DisplayName("loadUserByUsername - Admin User - Has ADMIN Authority")
    void loadUserByUsername_adminUser_hasAdminAuthority() {
        // Arrange
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("admin");

        // Assert
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).isNotEmpty();
        assertThat(authorities).anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("loadUserByUsername - User with No Role - Has Default Authority")
    void loadUserByUsername_userWithNoRole_hasDefaultAuthority() {
        // Arrange
        User userWithoutRole = TestDataBuilder.aValidUser()
                .username("norole")
                .role(null)
                .build();
        when(userRepository.findByUsername("norole")).thenReturn(Optional.of(userWithoutRole));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("norole");

        // Assert
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).isNotEmpty();
        assertThat(authorities).anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")); // Default role
    }

    // ==================== PASSWORD HANDLING TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Encrypted Password - Returns as Is")
    void loadUserByUsername_encryptedPassword_returnsAsIs() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPassword");
        assertThat(userDetails.getPassword()).startsWith("$2a$10$"); // BCrypt format
    }

    // ==================== NULL/EMPTY INPUT TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Null Username - Throws UsernameNotFoundException")
    void loadUserByUsername_nullUsername_throwsException() {
        // Arrange
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("loadUserByUsername - Empty Username - Throws UsernameNotFoundException")
    void loadUserByUsername_emptyUsername_throwsException() {
        // Arrange
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("loadUserByUsername - Whitespace Username - Throws UsernameNotFoundException")
    void loadUserByUsername_whitespaceUsername_throwsException() {
        // Arrange
        when(userRepository.findByUsername("   ")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ==================== USER ACCOUNT STATUS TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Active User - Account Not Expired")
    void loadUserByUsername_activeUser_accountNotExpired() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername - Active User - Account Not Locked")
    void loadUserByUsername_activeUser_accountNotLocked() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername - Active User - Credentials Not Expired")
    void loadUserByUsername_activeUser_credentialsNotExpired() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    // ==================== SPECIAL CHARACTERS TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Username with Special Characters - Handles Correctly")
    void loadUserByUsername_usernameWithSpecialCharacters_handlesCorrectly() {
        // Arrange
        User specialUser = TestDataBuilder.aValidUser()
                .username("user_with-dots.and_underscores")
                .build();
        when(userRepository.findByUsername("user_with-dots.and_underscores"))
                .thenReturn(Optional.of(specialUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("user_with-dots.and_underscores");

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("user_with-dots.and_underscores");
    }

    // ==================== MULTIPLE USERS TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Different Users - Returns Correct UserDetails")
    void loadUserByUsername_differentUsers_returnsCorrectUserDetails() {
        // Arrange
        User user1 = TestDataBuilder.aValidUser().username("user1").callsign("W1ABC").build();
        User user2 = TestDataBuilder.aValidUser().username("user2").callsign("K2XYZ").build();

        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(user2));

        // Act
        UserDetails userDetails1 = customUserDetailsService.loadUserByUsername("user1");
        UserDetails userDetails2 = customUserDetailsService.loadUserByUsername("user2");

        // Assert
        assertThat(userDetails1.getUsername()).isEqualTo("user1");
        assertThat(userDetails2.getUsername()).isEqualTo("user2");
        assertThat(userDetails1).isNotEqualTo(userDetails2);
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Called Multiple Times - Queries Database Each Time")
    void loadUserByUsername_calledMultipleTimes_queriesDatabaseEachTime() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        customUserDetailsService.loadUserByUsername("testuser");
        customUserDetailsService.loadUserByUsername("testuser");
        customUserDetailsService.loadUserByUsername("testuser");

        // Assert
        verify(userRepository, times(3)).findByUsername("testuser");
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Database Error - Propagates Exception")
    void loadUserByUsername_databaseError_propagatesException() {
        // Arrange
        when(userRepository.findByUsername("testuser"))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("testuser"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");
    }

    // ==================== INTEGRATION WITH SPRING SECURITY TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Returns UserDetails Compatible with Spring Security")
    void loadUserByUsername_returnsUserDetailsCompatibleWithSpringSecurity() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert - Verify all required UserDetails methods return expected values
        assertThat(userDetails.getUsername()).isNotNull();
        assertThat(userDetails.getPassword()).isNotNull();
        assertThat(userDetails.getAuthorities()).isNotNull();
        assertThat(userDetails.isEnabled()).isNotNull();
        assertThat(userDetails.isAccountNonExpired()).isNotNull();
        assertThat(userDetails.isAccountNonLocked()).isNotNull();
        assertThat(userDetails.isCredentialsNonExpired()).isNotNull();
    }

    // ==================== USER ENTITY MAPPING TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Maps User Entity to UserDetails Correctly")
    void loadUserByUsername_mapsUserEntityToUserDetailsCorrectly() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Assert - Verify mapping
        assertThat(userDetails.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(userDetails.getPassword()).isEqualTo(testUser.getPassword());
        assertThat(userDetails.isEnabled()).isEqualTo(testUser.getIsActive());
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - Very Long Username - Handles Correctly")
    void loadUserByUsername_veryLongUsername_handlesCorrectly() {
        // Arrange
        String longUsername = "a".repeat(255);
        User userWithLongName = TestDataBuilder.aValidUser()
                .username(longUsername)
                .build();
        when(userRepository.findByUsername(longUsername)).thenReturn(Optional.of(userWithLongName));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(longUsername);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(longUsername);
    }

    @Test
    @DisplayName("loadUserByUsername - Username with Unicode - Handles Correctly")
    void loadUserByUsername_usernameWithUnicode_handlesCorrectly() {
        // Arrange
        String unicodeUsername = "user_\u4E2D\u6587_test"; // Chinese characters
        User unicodeUser = TestDataBuilder.aValidUser()
                .username(unicodeUsername)
                .build();
        when(userRepository.findByUsername(unicodeUsername)).thenReturn(Optional.of(unicodeUser));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(unicodeUsername);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(unicodeUsername);
    }

    // ==================== CACHING BEHAVIOR TESTS ====================

    @Test
    @DisplayName("loadUserByUsername - No Caching - Fresh Data Each Time")
    void loadUserByUsername_noCaching_freshDataEachTime() {
        // Arrange
        User user1 = TestDataBuilder.aValidUser().username("testuser").email("old@example.com").build();
        User user2 = TestDataBuilder.aValidUser().username("testuser").email("new@example.com").build();

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(user1))
                .thenReturn(Optional.of(user2));

        // Act
        UserDetails userDetails1 = customUserDetailsService.loadUserByUsername("testuser");
        UserDetails userDetails2 = customUserDetailsService.loadUserByUsername("testuser");

        // Assert - Each call gets fresh data from database
        verify(userRepository, times(2)).findByUsername("testuser");
    }
}
