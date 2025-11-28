package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Operator entity
 */
@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    /**
     * Find operator by callsign
     */
    Optional<Operator> findByCallsign(String callsign);

    /**
     * Check if operator exists by callsign
     */
    boolean existsByCallsign(String callsign);
}
