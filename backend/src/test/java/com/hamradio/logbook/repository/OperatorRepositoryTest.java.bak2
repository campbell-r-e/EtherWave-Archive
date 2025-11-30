package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Operator;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Operator Repository Integration Tests")
class OperatorRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private OperatorRepository operatorRepository;

    @BeforeEach
    void setUp() {
        operatorRepository.deleteAll();
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid Operator - Persists Successfully")
    void save_validOperator_persistsSuccessfully() {
        // Arrange
        Operator operator = Operator.builder()
                .callsign("W1AW")
                .name("Hiram Percy Maxim")
                .email("hpm@arrl.org")
                .phone("555-1234")
                .build();

        // Act
        Operator saved = operatorRepository.save(operator);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCallsign()).isEqualTo("W1AW");
        assertThat(saved.getName()).isEqualTo("Hiram Percy Maxim");
    }

    @Test
    @DisplayName("findById - Existing Operator - Returns Operator")
    void findById_existingOperator_returnsOperator() {
        // Arrange
        Operator operator = operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Hiram Percy Maxim")
                .build());

        // Act
        Optional<Operator> found = operatorRepository.findById(operator.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCallsign()).isEqualTo("W1AW");
    }

    // ==================== FIND BY CALLSIGN TESTS ====================

    @Test
    @DisplayName("findByCallsign - Existing Operator - Returns Operator")
    void findByCallsign_existingOperator_returnsOperator() {
        // Arrange
        operatorRepository.save(Operator.builder()
                .callsign("K2ABC")
                .name("John Doe")
                .build());

        // Act
        Optional<Operator> found = operatorRepository.findByCallsign("K2ABC");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("findByCallsign - Case Insensitive - Returns Operator")
    void findByCallsign_caseInsensitive_returnsOperator() {
        // Arrange
        operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Hiram Percy Maxim")
                .build());

        // Act
        Optional<Operator> found = operatorRepository.findByCallsign("w1aw");

        // Assert
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByCallsignContaining - Returns Matching Operators")
    void findByCallsignContaining_returnsMatchingOperators() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator 1").build());
        operatorRepository.save(Operator.builder().callsign("W1ABC").name("Operator 2").build());
        operatorRepository.save(Operator.builder().callsign("K2ABC").name("Operator 3").build());

        // Act
        List<Operator> results = operatorRepository.findByCallsignContaining("W1");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(op -> op.getCallsign().startsWith("W1"));
    }

    // ==================== FIND BY NAME TESTS ====================

    @Test
    @DisplayName("findByNameContainingIgnoreCase - Returns Matching Operators")
    void findByNameContainingIgnoreCase_returnsMatchingOperators() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("John Smith").build());
        operatorRepository.save(Operator.builder().callsign("K2ABC").name("John Doe").build());
        operatorRepository.save(Operator.builder().callsign("N3XYZ").name("Jane Smith").build());

        // Act
        List<Operator> results = operatorRepository.findByNameContainingIgnoreCase("john");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(op -> op.getName().toLowerCase().contains("john"));
    }

    // ==================== FIND BY EMAIL TESTS ====================

    @Test
    @DisplayName("findByEmail - Existing Operator - Returns Operator")
    void findByEmail_existingOperator_returnsOperator() {
        // Arrange
        operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Hiram Percy Maxim")
                .email("hpm@arrl.org")
                .build());

        // Act
        Optional<Operator> found = operatorRepository.findByEmail("hpm@arrl.org");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCallsign()).isEqualTo("W1AW");
    }

    @Test
    @DisplayName("findByEmail - Case Insensitive - Returns Operator")
    void findByEmail_caseInsensitive_returnsOperator() {
        // Arrange
        operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Hiram Percy Maxim")
                .email("HPM@ARRL.ORG")
                .build());

        // Act
        Optional<Operator> found = operatorRepository.findByEmail("hpm@arrl.org");

        // Assert
        assertThat(found).isPresent();
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("existsByCallsign - Existing Operator - Returns True")
    void existsByCallsign_existingOperator_returnsTrue() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator").build());

        // Act
        boolean exists = operatorRepository.existsByCallsign("W1AW");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByCallsign - Non-Existent Operator - Returns False")
    void existsByCallsign_nonExistentOperator_returnsFalse() {
        // Act
        boolean exists = operatorRepository.existsByCallsign("ZZ9ZZZ");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByEmail - Existing Email - Returns True")
    void existsByEmail_existingEmail_returnsTrue() {
        // Arrange
        operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Operator")
                .email("test@example.com")
                .build());

        // Act
        boolean exists = operatorRepository.existsByEmail("test@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("count - Returns Total Operator Count")
    void count_returnsTotalOperatorCount() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator 1").build());
        operatorRepository.save(Operator.builder().callsign("K2ABC").name("Operator 2").build());
        operatorRepository.save(Operator.builder().callsign("N3XYZ").name("Operator 3").build());

        // Act
        long count = operatorRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes Operator")
    void deleteById_removesOperator() {
        // Arrange
        Operator operator = operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Operator")
                .build());
        Long operatorId = operator.getId();

        // Act
        operatorRepository.deleteById(operatorId);

        // Assert
        assertThat(operatorRepository.findById(operatorId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByCallsign - Removes Operator by Callsign")
    void deleteByCallsign_removesOperatorByCallsign() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator").build());

        // Act
        operatorRepository.deleteByCallsign("W1AW");

        // Assert
        assertThat(operatorRepository.findByCallsign("W1AW")).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("save - Update Existing Operator - Updates Successfully")
    void save_updateExistingOperator_updatesSuccessfully() {
        // Arrange
        Operator operator = operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Old Name")
                .email("old@example.com")
                .build());
        Long operatorId = operator.getId();

        // Act
        operator.setName("New Name");
        operator.setEmail("new@example.com");
        operator.setPhone("555-9999");
        operatorRepository.save(operator);

        // Assert
        Operator updated = operatorRepository.findById(operatorId).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getPhone()).isEqualTo("555-9999");
    }

    // ==================== FIND ALL TESTS ====================

    @Test
    @DisplayName("findAll - Returns All Operators")
    void findAll_returnsAllOperators() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator 1").build());
        operatorRepository.save(Operator.builder().callsign("K2ABC").name("Operator 2").build());
        operatorRepository.save(Operator.builder().callsign("N3XYZ").name("Operator 3").build());

        // Act
        List<Operator> operators = (List<Operator>) operatorRepository.findAll();

        // Assert
        assertThat(operators).hasSize(3);
    }

    // ==================== SORTING TESTS ====================

    @Test
    @DisplayName("findAllByOrderByCallsignAsc - Returns Operators Sorted by Callsign")
    void findAllByOrderByCallsignAsc_returnsOperatorsSortedByCallsign() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("N3XYZ").name("Operator 3").build());
        operatorRepository.save(Operator.builder().callsign("K2ABC").name("Operator 2").build());
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator 1").build());

        // Act
        List<Operator> operators = operatorRepository.findAllByOrderByCallsignAsc();

        // Assert
        assertThat(operators).hasSize(3);
        assertThat(operators.get(0).getCallsign()).isEqualTo("K2ABC");
        assertThat(operators.get(1).getCallsign()).isEqualTo("N3XYZ");
        assertThat(operators.get(2).getCallsign()).isEqualTo("W1AW");
    }

    @Test
    @DisplayName("findAllByOrderByNameAsc - Returns Operators Sorted by Name")
    void findAllByOrderByNameAsc_returnsOperatorsSortedByName() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Charlie").build());
        operatorRepository.save(Operator.builder().callsign("K2ABC").name("Alice").build());
        operatorRepository.save(Operator.builder().callsign("N3XYZ").name("Bob").build());

        // Act
        List<Operator> operators = operatorRepository.findAllByOrderByNameAsc();

        // Assert
        assertThat(operators).hasSize(3);
        assertThat(operators.get(0).getName()).isEqualTo("Alice");
        assertThat(operators.get(1).getName()).isEqualTo("Bob");
        assertThat(operators.get(2).getName()).isEqualTo("Charlie");
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("save - Duplicate Callsign - Throws Exception")
    void save_duplicateCallsign_throwsException() {
        // Arrange
        operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator 1").build());

        // Act & Assert
        assertThatThrownBy(() -> {
            operatorRepository.save(Operator.builder().callsign("W1AW").name("Operator 2").build());
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("save - Duplicate Email - Throws Exception")
    void save_duplicateEmail_throwsException() {
        // Arrange
        operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Operator 1")
                .email("test@example.com")
                .build());

        // Act & Assert
        assertThatThrownBy(() -> {
            operatorRepository.save(Operator.builder()
                    .callsign("K2ABC")
                    .name("Operator 2")
                    .email("test@example.com")
                    .build());
        }).isInstanceOf(Exception.class);
    }

    // ==================== NOTES TESTS ====================

    @Test
    @DisplayName("findByNotesContaining - Returns Operators with Matching Notes")
    void findByNotesContaining_returnsOperatorsWithMatchingNotes() {
        // Arrange
        operatorRepository.save(Operator.builder()
                .callsign("W1AW")
                .name("Operator 1")
                .notes("Contest specialist")
                .build());
        operatorRepository.save(Operator.builder()
                .callsign("K2ABC")
                .name("Operator 2")
                .notes("DX expert")
                .build());
        operatorRepository.save(Operator.builder()
                .callsign("N3XYZ")
                .name("Operator 3")
                .notes("Contest enthusiast")
                .build());

        // Act
        List<Operator> contestOperators = operatorRepository.findByNotesContaining("Contest");

        // Assert
        assertThat(contestOperators).hasSize(2);
        assertThat(contestOperators).allMatch(op -> op.getNotes().contains("Contest"));
    }
}
