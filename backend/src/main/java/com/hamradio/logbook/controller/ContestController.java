package com.hamradio.logbook.controller;

import com.hamradio.logbook.entity.Contest;
import com.hamradio.logbook.repository.ContestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Contest management
 */
@RestController
@RequestMapping("/api/contests")
@RequiredArgsConstructor
public class ContestController {

    private final ContestRepository contestRepository;

    @GetMapping
    public ResponseEntity<List<Contest>> getAllContests() {
        return ResponseEntity.ok(contestRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Contest>> getActiveContests() {
        return ResponseEntity.ok(contestRepository.findByIsActive(true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contest> getContest(@PathVariable Long id) {
        return contestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Contest> getContestByCode(@PathVariable String code) {
        return contestRepository.findByContestCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Contest> createContest(@RequestBody Contest contest) {
        Contest saved = contestRepository.save(contest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Contest> updateContest(@PathVariable Long id, @RequestBody Contest contest) {
        if (!contestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        contest.setId(id);
        Contest updated = contestRepository.save(contest);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContest(@PathVariable Long id) {
        if (!contestRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        contestRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
