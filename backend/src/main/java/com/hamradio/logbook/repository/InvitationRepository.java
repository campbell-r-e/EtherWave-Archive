package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.Invitation;
import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByInvitee(User invitee);

    List<Invitation> findByInviteeAndStatus(User invitee, Invitation.InvitationStatus status);

    List<Invitation> findByInviter(User inviter);

    List<Invitation> findByLog(Log log);

    List<Invitation> findByLogAndStatus(Log log, Invitation.InvitationStatus status);

    List<Invitation> findByStatus(Invitation.InvitationStatus status);

    Optional<Invitation> findByIdAndInvitee(Long id, User invitee);

    Optional<Invitation> findByIdAndInviter(Long id, User inviter);

    @Query("SELECT i FROM Invitation i WHERE i.invitee = :user AND i.status = 'PENDING' " +
           "AND (i.expiresAt IS NULL OR i.expiresAt > CURRENT_TIMESTAMP)")
    List<Invitation> findPendingInvitationsForUser(@Param("user") User user);

    @Query("SELECT i FROM Invitation i WHERE i.log.id = :logId AND i.status = 'PENDING'")
    List<Invitation> findPendingInvitationsByLogId(@Param("logId") Long logId);

    Boolean existsByLogAndInviteeAndStatus(Log log, User invitee, Invitation.InvitationStatus status);
}
