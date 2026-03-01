package com.hamradio.logbook.testutil;

import com.hamradio.logbook.entity.*;
import com.hamradio.logbook.entity.LogParticipant.ParticipantRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Test data builder for creating test entities
 * Updated to match current entity structure
 */
public class TestDataBuilder {

    // ==================== USER BUILDERS ====================

    public static User.UserBuilder basicUser() {
        return User.builder()
            .username("testuser")
            .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG") // "password"
            .callsign("W1TEST")
            .fullName("Test User")
            .gridSquare("FN31pr")
            .roles(new HashSet<>(Set.of(User.Role.ROLE_USER)))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true);
    }

    public static User.UserBuilder adminUser() {
        return User.builder()
            .username("admin")
            .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG") // "password"
            .callsign("W1ADMIN")
            .fullName("Admin User")
            .gridSquare("FN31pr")
            .roles(new HashSet<>(Set.of(User.Role.ROLE_USER, User.Role.ROLE_ADMIN)))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true);
    }

    // ==================== STATION BUILDERS ====================

    public static Station.StationBuilder basicStation() {
        return Station.builder()
            .stationName("Test Station")
            .callsign("W1TEST")
            .location("Boston, MA")
            .gridSquare("FN42aa")
            .antenna("Dipole")
            .powerWatts(100)
            .rigModel("Icom IC-7300")
            .rigControlEnabled(false);
    }

    public static Station.StationBuilder rigControlStation() {
        return Station.builder()
            .stationName("Rig Control Station")
            .callsign("W1RC")
            .location("Cambridge, MA")
            .gridSquare("FN42aa")
            .antenna("Beam")
            .powerWatts(1500)
            .rigModel("Yaesu FT-991A")
            .rigControlEnabled(true)
            .rigControlHost("localhost")
            .rigControlPort(4532);
    }

    // ==================== OPERATOR BUILDERS ====================

    public static Operator.OperatorBuilder basicOperator() {
        return Operator.builder()
            .callsign("W1OP")
            .name("John Operator")
            .email("operator@example.com")
            .licenseClass("EXTRA");
    }

    // ==================== LOG BUILDERS ====================

    public static Log.LogBuilder personalLog(User creator) {
        return Log.builder()
            .name("Personal Log")
            .description("Test personal log")
            .type(Log.LogType.PERSONAL)
            .creator(creator)
            .active(true)
            .editable(true)
            .isPublic(false)
            .startDate(LocalDateTime.now().minusDays(7))
            .endDate(LocalDateTime.now().plusDays(7));
    }

    public static Log.LogBuilder sharedLog(User creator) {
        return Log.builder()
            .name("Shared Log")
            .description("Test shared log")
            .type(Log.LogType.SHARED)
            .creator(creator)
            .active(true)
            .editable(true)
            .isPublic(false)
            .startDate(LocalDateTime.now().minusDays(7))
            .endDate(LocalDateTime.now().plusDays(7));
    }

    public static Log.LogBuilder contestLog(User creator, Contest contest) {
        return Log.builder()
            .name("Contest Log - " + contest.getContestName())
            .description("Contest log for " + contest.getContestCode())
            .type(Log.LogType.SHARED)
            .creator(creator)
            .contest(contest)
            .active(true)
            .editable(true)
            .isPublic(false)
            .startDate(contest.getStartDate())
            .endDate(contest.getEndDate());
    }

    // ==================== CONTEST BUILDERS ====================

    public static Contest.ContestBuilder basicContest() {
        return Contest.builder()
            .contestCode("TEST-CONTEST")
            .contestName("Test Contest")
            .description("A test contest for validation")
            .isActive(true)
            .startDate(LocalDateTime.of(2024, 6, 22, 14, 0))
            .endDate(LocalDateTime.of(2024, 6, 23, 20, 0));
    }

    public static Contest.ContestBuilder fieldDayContest() {
        return Contest.builder()
            .contestCode("ARRL-FD")
            .contestName("ARRL Field Day")
            .description("ARRL Field Day Contest")
            .isActive(true)
            .validatorClass("com.hamradio.logbook.validation.FieldDayValidator")
            .startDate(LocalDateTime.of(2024, 6, 22, 14, 0))
            .endDate(LocalDateTime.of(2024, 6, 23, 20, 0));
    }

    public static Contest.ContestBuilder potaContest() {
        return Contest.builder()
            .contestCode("POTA")
            .contestName("Parks on the Air")
            .description("POTA Activation")
            .isActive(true)
            .validatorClass("com.hamradio.logbook.validation.POTAValidator");
    }

    public static Contest.ContestBuilder sotaContest() {
        return Contest.builder()
            .contestCode("SOTA")
            .contestName("Summits on the Air")
            .description("SOTA Activation")
            .isActive(true)
            .validatorClass("com.hamradio.logbook.validation.SOTAValidator");
    }

    public static Contest.ContestBuilder winterFieldDayContest() {
        return Contest.builder()
            .contestCode("ARRL-WFD")
            .contestName("ARRL Winter Field Day")
            .description("ARRL Winter Field Day Contest")
            .isActive(true)
            .validatorClass("com.hamradio.logbook.validation.WinterFieldDayValidator")
            .startDate(LocalDateTime.of(2024, 1, 27, 14, 0))
            .endDate(LocalDateTime.of(2024, 1, 28, 20, 0));
    }

    // ==================== QSO BUILDERS ====================

    public static QSO.QSOBuilder basicQSO(Log log, Station station) {
        return QSO.builder()
            .log(log)
            .station(station)
            .callsign("W1AW")
            .frequencyKhz(14250000L)
            .mode("SSB")
            .band("20m")
            .qsoDate(LocalDate.now())
            .timeOn(LocalTime.of(14, 30))
            .rstSent("59")
            .rstRcvd("59")
            .powerWatts(100)
            .gridSquare("FN31pr")
            .country("United States")
            .isValid(true);
    }

    public static QSO.QSOBuilder contestQSO(Log log, Station station, Contest contest) {
        return QSO.builder()
            .log(log)
            .station(station)
            .contest(contest)
            .callsign("W1AW")
            .frequencyKhz(14250000L)
            .mode("SSB")
            .band("20m")
            .qsoDate(LocalDate.now())
            .timeOn(LocalTime.of(14, 30))
            .rstSent("59")
            .rstRcvd("59")
            .powerWatts(100)
            .gridSquare("FN31pr")
            .country("United States")
            .isValid(true);
    }

    public static QSO.QSOBuilder fieldDayQSO(Log log, Station station, Contest contest) {
        return QSO.builder()
            .log(log)
            .station(station)
            .contest(contest)
            .callsign("W1AW")
            .frequencyKhz(14250000L)
            .mode("SSB")
            .band("20m")
            .qsoDate(LocalDate.now())
            .timeOn(LocalTime.of(14, 30))
            .rstSent("59")
            .rstRcvd("59")
            .powerWatts(100)
            .gridSquare("FN31pr")
            .country("United States")
            .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
            .isValid(true);
    }

    public static QSO.QSOBuilder potaQSO(Log log, Station station, Contest contest) {
        return QSO.builder()
            .log(log)
            .station(station)
            .contest(contest)
            .callsign("W1AW")
            .frequencyKhz(14250000L)
            .mode("SSB")
            .band("20m")
            .qsoDate(LocalDate.now())
            .timeOn(LocalTime.of(14, 30))
            .rstSent("59")
            .rstRcvd("59")
            .powerWatts(100)
            .gridSquare("FN31pr")
            .country("United States")
            .contestData("{\"park_ref\":\"K-0817\"}")
            .isValid(true);
    }

    public static QSO.QSOBuilder sotaQSO(Log log, Station station, Contest contest) {
        return QSO.builder()
            .log(log)
            .station(station)
            .contest(contest)
            .callsign("W1AW")
            .frequencyKhz(14250000L)
            .mode("SSB")
            .band("20m")
            .qsoDate(LocalDate.now())
            .timeOn(LocalTime.of(14, 30))
            .rstSent("59")
            .rstRcvd("59")
            .powerWatts(100)
            .gridSquare("FN31pr")
            .country("United States")
            .contestData("{\"summit_ref\":\"W7W/NG-001\"}")
            .isValid(true);
    }

    // ==================== INVITATION BUILDERS ====================

    public static Invitation.InvitationBuilder basicInvitation(Log log, User inviter, User invitee) {
        return Invitation.builder()
            .log(log)
            .inviter(inviter)
            .invitee(invitee)
            .proposedRole(LogParticipant.ParticipantRole.STATION)
            .status(Invitation.InvitationStatus.PENDING)
            .message("Join our log!");
    }

    public static Invitation.InvitationBuilder acceptedInvitation(Log log, User inviter, User invitee) {
        return Invitation.builder()
            .log(log)
            .inviter(inviter)
            .invitee(invitee)
            .proposedRole(LogParticipant.ParticipantRole.STATION)
            .status(Invitation.InvitationStatus.ACCEPTED)
            .respondedAt(LocalDateTime.now());
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Create a persisted user (requires repository)
     */
    public static User createPersistedUser(String username, String callsign) {
        return User.builder()
            .username(username)
            .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
            .callsign(callsign)
            .fullName(username + " Full Name")
            .gridSquare("FN31pr")
            .roles(new HashSet<>(Set.of(User.Role.ROLE_USER)))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();
    }
}
