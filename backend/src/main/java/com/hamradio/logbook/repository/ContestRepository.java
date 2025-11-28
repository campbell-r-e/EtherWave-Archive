package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Contest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Contest entity
 */
@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {

    /**
     * Find contest by contest code
     */
    Optional<Contest> findByContestCode(String contestCode);

    /**
     * Find all active contests
     */
    List<Contest> findByIsActive(Boolean isActive);

    /**
     * Find contests by name (case-insensitive, partial match)
     */
    List<Contest> findByContestNameContainingIgnoreCase(String name);
}
