package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import com.hamradio.logbook.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User Repository Integration Tests")
class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid User - Persists Successfully")
    void save_validUser_persistsSuccessfully() {
        // Arrange
        User user = TestDataBuilder.aValidUser().build();

        // Act
        User saved = userRepository.save(user);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getCallsign()).isEqualTo("W1AW");
    }

    @Test
    @DisplayName("findById - Existing User - Returns User")
    void findById_existingUser_returnsUser() {
        // Arrange
        User user = userRepository.save(TestDataBuilder.aValidUser().build());

        // Act
        Optional<User> found = userRepository.findById(user.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    // ==================== FIND BY USERNAME TESTS ====================

    @Test
    @DisplayName("findByUsername - Existing User - Returns User")
    void findByUsername_existingUser_returnsUser() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("operator1").build());

        // Act
        Optional<User> found = userRepository.findByUsername("operator1");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("operator1");
    }

    @Test
    @DisplayName("findByUsername - Non-Existent User - Returns Empty")
    void findByUsername_nonExistentUser_returnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByUsername - Case Sensitive - Returns Correct User")
    void findByUsername_caseSensitive_returnsCorrectUser() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("TestUser").build());

        // Act
        Optional<User> found = userRepository.findByUsername("testuser");

        // Assert
        assertThat(found).isEmpty(); // Username is case-sensitive
    }

    // ==================== FIND BY EMAIL TESTS ====================

    @Test
    @DisplayName("findByEmail - Existing User - Returns User")
    void findByEmail_existingUser_returnsUser() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().email("operator@test.com").build());

        // Act
        Optional<User> found = userRepository.findByEmail("operator@test.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("operator@test.com");
    }

    @Test
    @DisplayName("findByEmail - Non-Existent Email - Returns Empty")
    void findByEmail_nonExistentEmail_returnsEmpty() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@test.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByEmail - Case Insensitive - Returns User")
    void findByEmail_caseInsensitive_returnsUser() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().email("Test@Example.com").build());

        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertThat(found).isPresent();
    }

    // ==================== FIND BY CALLSIGN TESTS ====================

    @Test
    @DisplayName("findByCallsign - Existing Callsign - Returns User")
    void findByCallsign_existingCallsign_returnsUser() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().callsign("K2ABC").build());

        // Act
        Optional<User> found = userRepository.findByCallsign("K2ABC");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCallsign()).isEqualTo("K2ABC");
    }

    @Test
    @DisplayName("findByCallsign - Case Insensitive - Returns User")
    void findByCallsign_caseInsensitive_returnsUser() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().callsign("W1AW").build());

        // Act
        Optional<User> found = userRepository.findByCallsign("w1aw");

        // Assert
        assertThat(found).isPresent();
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("existsByUsername - Existing User - Returns True")
    void existsByUsername_existingUser_returnsTrue() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("operator1").build());

        // Act
        boolean exists = userRepository.existsByUsername("operator1");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername - Non-Existent User - Returns False")
    void existsByUsername_nonExistentUser_returnsFalse() {
        // Act
        boolean exists = userRepository.existsByUsername("nonexistent");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByEmail - Existing Email - Returns True")
    void existsByEmail_existingEmail_returnsTrue() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().email("test@example.com").build());

        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - Non-Existent Email - Returns False")
    void existsByEmail_nonExistentEmail_returnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByCallsign - Existing Callsign - Returns True")
    void existsByCallsign_existingCallsign_returnsTrue() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().callsign("W1AW").build());

        // Act
        boolean exists = userRepository.existsByCallsign("W1AW");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByCallsign - Non-Existent Callsign - Returns False")
    void existsByCallsign_nonExistentCallsign_returnsFalse() {
        // Act
        boolean exists = userRepository.existsByCallsign("ZZ9ZZZ");

        // Assert
        assertThat(exists).isFalse();
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @DisplayName("findByCallsignContaining - Returns Matching Users")
    void findByCallsignContaining_returnsMatchingUsers() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().callsign("W1AW").build());
        userRepository.save(TestDataBuilder.aValidUser().callsign("W1ABC").username("user2").email("user2@test.com").build());
        userRepository.save(TestDataBuilder.aValidUser().callsign("K2ABC").username("user3").email("user3@test.com").build());

        // Act
        List<User> results = userRepository.findByCallsignContaining("W1");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(user -> user.getCallsign().startsWith("W1"));
    }

    @Test
    @DisplayName("findByUsernameContaining - Returns Matching Users")
    void findByUsernameContaining_returnsMatchingUsers() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("operator1").build());
        userRepository.save(TestDataBuilder.aValidUser().username("operator2").email("op2@test.com").callsign("K2ABC").build());
        userRepository.save(TestDataBuilder.aValidUser().username("admin1").email("admin@test.com").callsign("N3XYZ").build());

        // Act
        List<User> results = userRepository.findByUsernameContaining("operator");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(user -> user.getUsername().contains("operator"));
    }

    // ==================== ACTIVE STATUS TESTS ====================

    @Test
    @DisplayName("findByActive - Returns Only Active Users")
    void findByActive_returnsOnlyActiveUsers() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("active1").active(true).build());
        userRepository.save(TestDataBuilder.aValidUser().username("active2").email("a2@test.com").callsign("K2ABC").active(true).build());
        userRepository.save(TestDataBuilder.aValidUser().username("inactive1").email("i1@test.com").callsign("N3XYZ").active(false).build());

        // Act
        List<User> activeUsers = userRepository.findByActive(true);
        List<User> inactiveUsers = userRepository.findByActive(false);

        // Assert
        assertThat(activeUsers).hasSize(2);
        assertThat(inactiveUsers).hasSize(1);
        assertThat(inactiveUsers.get(0).getUsername()).isEqualTo("inactive1");
    }

    @Test
    @DisplayName("countByActive - Returns Count of Active Users")
    void countByActive_returnsCountOfActiveUsers() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().active(true).build());
        userRepository.save(TestDataBuilder.aValidUser().username("user2").email("u2@test.com").callsign("K2ABC").active(true).build());
        userRepository.save(TestDataBuilder.aValidUser().username("user3").email("u3@test.com").callsign("N3XYZ").active(false).build());

        // Act
        long activeCount = userRepository.countByActive(true);
        long inactiveCount = userRepository.countByActive(false);

        // Assert
        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

    // ==================== ROLE TESTS ====================

    @Test
    @DisplayName("findByRole - Returns Users with Role")
    void findByRole_returnsUsersWithRole() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("admin1").role("ROLE_ADMIN").build());
        userRepository.save(TestDataBuilder.aValidUser().username("user1").email("u1@test.com").callsign("K2ABC").role("ROLE_USER").build());
        userRepository.save(TestDataBuilder.aValidUser().username("admin2").email("a2@test.com").callsign("N3XYZ").role("ROLE_ADMIN").build());

        // Act
        List<User> admins = userRepository.findByRole("ROLE_ADMIN");
        List<User> regularUsers = userRepository.findByRole("ROLE_USER");

        // Assert
        assertThat(admins).hasSize(2);
        assertThat(regularUsers).hasSize(1);
    }

    // ==================== DATE TESTS ====================

    @Test
    @DisplayName("findByCreatedAtAfter - Returns Users Created After Date")
    void findByCreatedAtAfter_returnsUsersCreatedAfterDate() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        User oldUser = TestDataBuilder.aValidUser().username("old").build();
        oldUser.setCreatedAt(now.minusDays(10));
        userRepository.save(oldUser);

        User newUser = TestDataBuilder.aValidUser().username("new").email("new@test.com").callsign("K2ABC").build();
        newUser.setCreatedAt(now.minusDays(2));
        userRepository.save(newUser);

        // Act
        List<User> recentUsers = userRepository.findByCreatedAtAfter(now.minusDays(5));

        // Assert
        assertThat(recentUsers).hasSize(1);
        assertThat(recentUsers.get(0).getUsername()).isEqualTo("new");
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("save - Update Existing User - Updates Successfully")
    void save_updateExistingUser_updatesSuccessfully() {
        // Arrange
        User user = userRepository.save(TestDataBuilder.aValidUser().build());
        Long userId = user.getId();

        // Act
        user.setCallsign("N3XYZ");
        user.setEmail("updated@example.com");
        userRepository.save(user);

        // Assert
        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getCallsign()).isEqualTo("N3XYZ");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes User")
    void deleteById_removesUser() {
        // Arrange
        User user = userRepository.save(TestDataBuilder.aValidUser().build());
        Long userId = user.getId();

        // Act
        userRepository.deleteById(userId);

        // Assert
        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByUsername - Removes User by Username")
    void deleteByUsername_removesUserByUsername() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("toDelete").build());

        // Act
        userRepository.deleteByUsername("toDelete");

        // Assert
        assertThat(userRepository.findByUsername("toDelete")).isEmpty();
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("count - Returns Total User Count")
    void count_returnsTotalUserCount() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().build());
        userRepository.save(TestDataBuilder.aValidUser().username("user2").email("u2@test.com").callsign("K2ABC").build());
        userRepository.save(TestDataBuilder.aValidUser().username("user3").email("u3@test.com").callsign("N3XYZ").build());

        // Act
        long count = userRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("save - Duplicate Username - Throws Exception")
    void save_duplicateUsername_throwsException() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().username("duplicate").build());

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.save(TestDataBuilder.aValidUser().username("duplicate").email("different@test.com").callsign("K2ABC").build());
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("save - Duplicate Email - Throws Exception")
    void save_duplicateEmail_throwsException() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().email("duplicate@test.com").build());

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.save(TestDataBuilder.aValidUser().username("different").email("duplicate@test.com").callsign("K2ABC").build());
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("save - Duplicate Callsign - Throws Exception")
    void save_duplicateCallsign_throwsException() {
        // Arrange
        userRepository.save(TestDataBuilder.aValidUser().callsign("W1AW").build());

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.save(TestDataBuilder.aValidUser().username("different").email("different@test.com").callsign("W1AW").build());
        }).isInstanceOf(Exception.class);
    }
}
