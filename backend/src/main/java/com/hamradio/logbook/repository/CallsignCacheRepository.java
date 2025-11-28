package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.CallsignCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for CallsignCache entity
 */
@Repository
public interface CallsignCacheRepository extends JpaRepository<CallsignCache, Long> {

    /**
     * Find cached callsign data
     */
    Optional<CallsignCache> findByCallsign(String callsign);

    /**
     * Find non-expired cached entry
     */
    @Query("SELECT c FROM CallsignCache c WHERE c.callsign = :callsign AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    Optional<CallsignCache> findValidByCallsign(@Param("callsign") String callsign, @Param("now") LocalDateTime now);

    /**
     * Delete expired cache entries
     */
    @Query("DELETE FROM CallsignCache c WHERE c.expiresAt IS NOT NULL AND c.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
