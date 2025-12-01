package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Operator;
import com.hamradio.logbook.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorService Unit Tests")
class OperatorServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @InjectMocks
    private OperatorService operatorService;

    private Operator testOperator;

    @BeforeEach
    void setUp() {
        testOperator = new Operator();
        testOperator.setId(1L);
        testOperator.setCallsign("W1TEST");
        testOperator.setName("Test Operator");
        testOperator.setLicenseClass("Extra");
        testOperator.setEmail("operator@example.com");
    }

    @Test
    @DisplayName("Should create operator successfully")
    void shouldCreateOperator() {
        when(operatorRepository.save(any(Operator.class))).thenReturn(testOperator);

        Operator result = operatorService.createOperator(testOperator);

        assertNotNull(result);
        assertEquals("W1TEST", result.getCallsign());
        verify(operatorRepository).save(testOperator);
    }

    @Test
    @DisplayName("Should get operator by ID successfully")
    void shouldGetOperatorById() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        Optional<Operator> result = operatorService.getOperatorById(1L);

        assertTrue(result.isPresent());
        assertEquals("W1TEST", result.get().getCallsign());
        verify(operatorRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when operator not found by ID")
    void shouldReturnEmptyWhenOperatorNotFoundById() {
        when(operatorRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Operator> result = operatorService.getOperatorById(999L);

        assertFalse(result.isPresent());
        verify(operatorRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get operator by callsign successfully")
    void shouldGetOperatorByCallsign() {
        when(operatorRepository.findByCallsign("W1TEST")).thenReturn(Optional.of(testOperator));

        Optional<Operator> result = operatorService.getOperatorByCallsign("W1TEST");

        assertTrue(result.isPresent());
        assertEquals("W1TEST", result.get().getCallsign());
        verify(operatorRepository).findByCallsign("W1TEST");
    }

    @Test
    @DisplayName("Should return empty when operator not found by callsign")
    void shouldReturnEmptyWhenOperatorNotFoundByCallsign() {
        when(operatorRepository.findByCallsign("NONEXISTENT")).thenReturn(Optional.empty());

        Optional<Operator> result = operatorService.getOperatorByCallsign("NONEXISTENT");

        assertFalse(result.isPresent());
        verify(operatorRepository).findByCallsign("NONEXISTENT");
    }

    @Test
    @DisplayName("Should get all operators")
    void shouldGetAllOperators() {
        Operator operator2 = new Operator();
        operator2.setId(2L);
        operator2.setCallsign("W2TEST");
        operator2.setName("Operator 2");

        List<Operator> operators = Arrays.asList(testOperator, operator2);
        when(operatorRepository.findAll()).thenReturn(operators);

        List<Operator> result = operatorService.getAllOperators();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(operatorRepository).findAll();
    }

    @Test
    @DisplayName("Should update operator successfully")
    void shouldUpdateOperator() {
        Operator updates = new Operator();
        updates.setCallsign("W2UPD");
        updates.setName("Updated Name");
        updates.setLicenseClass("General");
        updates.setEmail("updated@example.com");

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(testOperator);

        Operator result = operatorService.updateOperator(1L, updates);

        assertNotNull(result);
        verify(operatorRepository).save(any(Operator.class));
    }

    @Test
    @DisplayName("Should fail to update operator when not found")
    void shouldFailToUpdateOperatorWhenNotFound() {
        Operator updates = new Operator();
        when(operatorRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> operatorService.updateOperator(999L, updates)
        );

        assertEquals("Operator not found", exception.getMessage());
        verify(operatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update operator with partial updates")
    void shouldUpdateOperatorWithPartialUpdates() {
        Operator updates = new Operator();
        updates.setName("Only Name Updated");

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.save(any(Operator.class))).thenReturn(testOperator);

        Operator result = operatorService.updateOperator(1L, updates);

        assertNotNull(result);
        verify(operatorRepository).save(any(Operator.class));
    }

    @Test
    @DisplayName("Should delete operator successfully")
    void shouldDeleteOperator() {
        doNothing().when(operatorRepository).deleteById(1L);

        operatorService.deleteOperator(1L);

        verify(operatorRepository).deleteById(1L);
    }
}
