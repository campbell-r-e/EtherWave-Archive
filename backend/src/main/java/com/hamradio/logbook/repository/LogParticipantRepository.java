package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.LogParticipant;
import com.hamradio.logbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogParticipantRepository extends JpaRepository<LogParticipant, Long> {

    List<LogParticipant> findByLog(Log log);

    List<LogParticipant> findByLogAndActive(Log log, Boolean active);

    List<LogParticipant> findByUser(User user);

    List<LogParticipant> findByUserAndActive(User user, Boolean active);

    Optional<LogParticipant> findByLogAndUser(Log log, User user);

    Optional<LogParticipant> findByLogAndUserAndActive(Log log, User user, Boolean active);

    Boolean existsByLogAndUser(Log log, User user);

    @Query("SELECT lp FROM LogParticipant lp WHERE lp.log.id = :logId AND lp.user.id = :userId")
    Optional<LogParticipant> findByLogIdAndUserId(@Param("logId") Long logId, @Param("userId") Long userId);

    @Query("SELECT lp FROM LogParticipant lp WHERE lp.log.id = :logId AND lp.active = true")
    List<LogParticipant> findActiveParticipantsByLogId(@Param("logId") Long logId);

    void deleteByLogAndUser(Log log, User user);
}
