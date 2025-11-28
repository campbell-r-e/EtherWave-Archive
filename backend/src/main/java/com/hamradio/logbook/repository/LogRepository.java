package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

    List<Log> findByCreator(User creator);

    List<Log> findByCreatorAndActive(User creator, Boolean active);

    List<Log> findByType(Log.LogType type);

    Optional<Log> findByIdAndCreator(Long id, User creator);

    @Query("SELECT l FROM Log l WHERE l.creator = :user OR " +
           "EXISTS (SELECT 1 FROM LogParticipant lp WHERE lp.log = l AND lp.user = :user AND lp.active = true)")
    List<Log> findLogsByUser(@Param("user") User user);

    @Query("SELECT l FROM Log l WHERE (l.creator = :user OR " +
           "EXISTS (SELECT 1 FROM LogParticipant lp WHERE lp.log = l AND lp.user = :user AND lp.active = true)) " +
           "AND l.active = true")
    List<Log> findActiveLogsByUser(@Param("user") User user);

    @Query("SELECT COUNT(l) > 0 FROM Log l WHERE l.id = :logId AND " +
           "(l.creator = :user OR " +
           "EXISTS (SELECT 1 FROM LogParticipant lp WHERE lp.log.id = :logId AND lp.user = :user AND lp.active = true))")
    Boolean userHasAccessToLog(@Param("logId") Long logId, @Param("user") User user);
}
