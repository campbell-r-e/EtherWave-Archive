package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializationService Unit Tests")
class AdminInitializationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializationService adminInitializationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminInitializationService, "adminUsername", "admin");
        ReflectionTestUtils.setField(adminInitializationService, "adminPassword", "admin123");
        ReflectionTestUtils.setField(adminInitializationService, "adminEmail", "admin@hamradio.local");
    }

    @Test
    @DisplayName("Should create admin user successfully when credentials are provided")
    void shouldCreateAdminUserSuccessfully() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("admin", savedUser.getUsername());
        assertEquals("admin@hamradio.local", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals("System Administrator", savedUser.getFullName());
        assertTrue(savedUser.getEnabled());
        assertTrue(savedUser.getAccountNonExpired());
        assertTrue(savedUser.getAccountNonLocked());
        assertTrue(savedUser.getCredentialsNonExpired());
        assertTrue(savedUser.getRoles().contains(User.Role.ROLE_ADMIN));
        assertTrue(savedUser.getRoles().contains(User.Role.ROLE_USER));
    }

    @Test
    @DisplayName("Should skip admin creation when username is empty")
    void shouldSkipWhenUsernameIsEmpty() {
        ReflectionTestUtils.setField(adminInitializationService, "adminUsername", "");

        adminInitializationService.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip admin creation when username is null")
    void shouldSkipWhenUsernameIsNull() {
        ReflectionTestUtils.setField(adminInitializationService, "adminUsername", null);

        adminInitializationService.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip admin creation when password is empty")
    void shouldSkipWhenPasswordIsEmpty() {
        ReflectionTestUtils.setField(adminInitializationService, "adminPassword", "");

        adminInitializationService.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip admin creation when password is null")
    void shouldSkipWhenPasswordIsNull() {
        ReflectionTestUtils.setField(adminInitializationService, "adminPassword", null);

        adminInitializationService.run();

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip admin creation when admin user already exists")
    void shouldSkipWhenAdminAlreadyExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        adminInitializationService.run();

        verify(userRepository).existsByUsername("admin");
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should encode password before saving")
    void shouldEncodePasswordBeforeSaving() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        verify(passwordEncoder).encode("admin123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("$2a$10$encodedPassword", savedUser.getPassword());
    }

    @Test
    @DisplayName("Should assign both ADMIN and USER roles")
    void shouldAssignBothAdminAndUserRoles() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getRoles());
        assertEquals(2, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().contains(User.Role.ROLE_ADMIN));
        assertTrue(savedUser.getRoles().contains(User.Role.ROLE_USER));
    }

    @Test
    @DisplayName("Should set all user account flags to true")
    void shouldSetAllAccountFlagsToTrue() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.getEnabled());
        assertTrue(savedUser.getAccountNonExpired());
        assertTrue(savedUser.getAccountNonLocked());
        assertTrue(savedUser.getCredentialsNonExpired());
    }

    @Test
    @DisplayName("Should use configured email address")
    void shouldUseConfiguredEmailAddress() {
        ReflectionTestUtils.setField(adminInitializationService, "adminEmail", "custom@example.com");

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("custom@example.com", savedUser.getEmail());
    }

    @Test
    @DisplayName("Should set full name to System Administrator")
    void shouldSetFullNameToSystemAdministrator() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("System Administrator", savedUser.getFullName());
    }

    @Test
    @DisplayName("Should accept command line arguments")
    void shouldAcceptCommandLineArguments() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Should accept var args
        adminInitializationService.run("arg1", "arg2");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle custom username")
    void shouldHandleCustomUsername() {
        ReflectionTestUtils.setField(adminInitializationService, "adminUsername", "superadmin");

        when(userRepository.existsByUsername("superadmin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        verify(userRepository).existsByUsername("superadmin");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("superadmin", savedUser.getUsername());
    }

    @Test
    @DisplayName("Should use default email if not configured")
    void shouldUseDefaultEmail() {
        // Default email is set in @BeforeEach
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminInitializationService.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("admin@hamradio.local", savedUser.getEmail());
    }
}
