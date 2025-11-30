package com.hamradio.logbook.testutil;

import com.hamradio.logbook.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * Test data builders for creating test entities with realistic ham radio data
 */
public class TestDataBuilder {

    // ==================== User Builders ====================

    public static User.UserBuilder aValidUser() {
        return User.builder()
                .username("testuser")
                .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG") // BCrypt: "password123"
                .email("testuser@example.com")
                .callsign("W1TEST")
                .roles(Set.of(User.Role.ROLE_USER))
                .enabled(true);
    }

    public static User.UserBuilder anAdminUser() {
        return aValidUser()
                .username("admin")
                .email("admin@example.com")
                .callsign("W1ADMIN")
                .roles(Set.of(User.Role.ROLE_ADMIN));
    }

    // ==================== Station Builders ====================

    public static Station.StationBuilder aValidStation() {
        return Station.builder()
                .stationName("Home Station")
                .callsign("W1ABC")
                .gridSquare("FN31pr")
                .latitude(42.3601)
                .longitude(-71.0589)
                .power(100)
                .antenna("Dipole")
                .rigControlEnabled(false);
    }

    public static Station.StationBuilder aStationWithRigControl() {
        return aValidStation()
                .stationName("Contest Station")
                .rigControlEnabled(true)
                .rigControlUrl("http://localhost:8081");
    }

    // ==================== Operator Builders ====================

    public static Operator.OperatorBuilder aValidOperator() {
        return Operator.builder()
                .callsign("K1OPR")
                .firstName("John")
                .lastName("Doe")
                .licenseClass("GENERAL")
                .gridSquare("FN31pr");
    }

    // ==================== Contest Builders ====================

    public static Contest.ContestBuilder aFieldDayContest() {
        return Contest.builder()
                .contestName("ARRL Field Day 2025")
                .contestCode("ARRL-FD")
                .startDate(LocalDateTime.of(2025, 6, 28, 18, 0))
                .endDate(LocalDateTime.of(2025, 6, 29, 17, 59))
                .validatorClass("com.hamradio.logbook.validation.FieldDayValidator")
                .isActive(true);
    }

    public static Contest.ContestBuilder aPOTAContest() {
        return Contest.builder()
                .contestName("Parks on the Air")
                .contestCode("POTA")
                .validatorClass("com.hamradio.logbook.validation.POTAValidator")
                .isActive(true);
    }

    public static Contest.ContestBuilder aWinterFieldDayContest() {
        return Contest.builder()
                .contestName("Winter Field Day 2025")
                .contestCode("WFD")
                .startDate(LocalDateTime.of(2025, 1, 25, 18, 0))
                .endDate(LocalDateTime.of(2025, 1, 26, 17, 59))
                .validatorClass("com.hamradio.logbook.validation.WinterFieldDayValidator")
                .isActive(true);
    }

    // ==================== Log Builders ====================

    public static Log.LogBuilder aValidLog(User creator) {
        return Log.builder()
                .name("My Contest Log")
                .description("Field Day 2025 Log")
                .type(Log.LogType.PERSONAL)
                .creator(creator)
                .active(true)
                .editable(true)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(24));
    }

    public static Log.LogBuilder aFrozenLog(User creator) {
        return aValidLog(creator)
                .name("Frozen Log")
                .editable(false);
    }

    public static Log.LogBuilder aContestLog(User creator, Contest contest) {
        return aValidLog(creator)
                .name(contest.getContestName() + " Log")
                .type(Log.LogType.SHARED)
                .contest(contest);
    }

    // ==================== QSO Builders ====================

    public static QSO.QSOBuilder aValidQSO(Station station, Log log) {
        return QSO.builder()
                .station(station)
                .log(log)
                .callsign("W1AW")
                .frequencyKhz(14250L)
                .mode("SSB")
                .band("20m")
                .qsoDate(LocalDate.now())
                .timeOn(LocalTime.of(14, 30, 0))
                .rstSent("59")
                .rstRcvd("59")
                .isValid(true);
    }

    public static QSO.QSOBuilder aCWQSO(Station station, Log log) {
        return aValidQSO(station, log)
                .callsign("K2CW")
                .frequencyKhz(7030L)
                .mode("CW")
                .band("40m")
                .rstSent("599")
                .rstRcvd("599");
    }

    public static QSO.QSOBuilder anFT8QSO(Station station, Log log) {
        return aValidQSO(station, log)
                .callsign("N3FT")
                .frequencyKhz(14074L)
                .mode("FT8")
                .band("20m")
                .rstSent("-10")
                .rstRcvd("-15");
    }

    public static QSO.QSOBuilder aFieldDayQSO(Station station, Log log) {
        return aValidQSO(station, log)
                .callsign("W4FD")
                .contestData("{\"class\":\"2A\",\"section\":\"ORG\"}")
                .state("VA")
                .county("Arlington");
    }

    public static QSO.QSOBuilder aPOTAQSO(Station station, Log log) {
        return aValidQSO(station, log)
                .callsign("K5POT")
                .contestData("{\"park_ref\":\"K-0817\"}")
                .state("TX")
                .gridSquare("EM10");
    }

    // ==================== Invitation Builders ====================

    public static Invitation.InvitationBuilder aValidInvitation(Log log, User inviter, String inviteeEmail) {
        return Invitation.builder()
                .log(log)
                .inviter(inviter)
                .inviteeEmail(inviteeEmail)
                .role(LogParticipant.ParticipantRole.VIEWER)
                .status(Invitation.InvitationStatus.PENDING);
    }

    public static Invitation.InvitationBuilder anAcceptedInvitation(Log log, User inviter, String inviteeEmail) {
        return aValidInvitation(log, inviter, inviteeEmail)
                .status(Invitation.InvitationStatus.ACCEPTED);
    }

    // ==================== LogParticipant Builders ====================

    public static LogParticipant.LogParticipantBuilder aLogParticipant(Log log, User user, LogParticipant.ParticipantRole role) {
        return LogParticipant.builder()
                .log(log)
                .user(user)
                .role(role);
    }

    // ==================== Realistic Ham Radio Data ====================

    public static class RealisticData {
        public static final String[] CALLSIGNS = {
                "W1AW", "K1ABC", "N2XYZ", "KD6ABC", "WA7BNM",
                "VE3JOE", "G3ABC", "JA1ABC", "VK2ABC"
        };

        public static final String[] MODES = {
                "SSB", "CW", "FM", "AM", "RTTY", "PSK31", "FT8", "FT4", "MFSK", "OLIVIA"
        };

        public static final String[] BANDS = {
                "160m", "80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m",
                "6m", "2m", "1.25m", "70cm"
        };

        public static final String[] GRID_SQUARES = {
                "FN31pr", "EM12lv", "DM43ne", "CN87ts", "EL89wg",
                "EN52md", "DN31vr", "FM18lw"
        };

        public static final String[] STATES = {
                "CA", "TX", "FL", "NY", "PA", "IL", "OH", "MI", "GA", "NC",
                "NJ", "VA", "WA", "MA", "AZ", "TN", "IN", "MO", "MD", "WI"
        };

        public static final String[] ARRL_SECTIONS = {
                "ORG", "SCV", "LAX", "SF", "SB", "SDG", "SJV", "SV", "PAC", "EB",
                "AL", "GA", "KY", "NC", "NFL", "SFL", "WCF", "PR", "VI", "SC",
                "TN", "VA", "AR", "LA", "MS", "NM", "NTX", "OK", "STX", "WTX"
        };

        public static final String[] FIELD_DAY_CLASSES = {
                "1A", "2A", "3A", "4A", "5A", "6A", "1B", "1C", "1D", "1E", "1F"
        };
    }
}
