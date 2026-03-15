package com.hamradio.logbook.controller;

import com.hamradio.logbook.entity.Operator;
import com.hamradio.logbook.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Operator management
 */
@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorRepository operatorRepository;

    @GetMapping
    public ResponseEntity<List<Operator>> getAllOperators() {
        return ResponseEntity.ok(operatorRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operator> getOperator(@PathVariable Long id) {
        return operatorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/callsign/{callsign}")
    public ResponseEntity<Operator> getOperatorByCallsign(@PathVariable String callsign) {
        return operatorRepository.findByCallsign(callsign)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Operator> createOperator(@RequestBody Operator operator) {
        Operator saved = operatorRepository.save(operator);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Operator> updateOperator(@PathVariable Long id, @RequestBody Operator operator) {
        if (!operatorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        operator.setId(id);
        Operator updated = operatorRepository.save(operator);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperator(@PathVariable Long id) {
        if (!operatorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        operatorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
