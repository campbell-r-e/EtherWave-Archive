package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.CallsignCache;
import com.hamradio.logbook.repository.CallsignCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Callsign Validation Service Tests")
class CallsignValidationServiceTest {

    @Mock
    private CallsignCacheRepository callsignCacheRepository;

    @InjectMocks
    private CallsignValidationService callsignValidationService;

    // ==================== VALID CALLSIGN FORMAT TESTS ====================

    @ParameterizedTest(name = "Callsign {0} should be valid")
    @ValueSource(strings = {
            // US callsigns
            "W1AW", "K2ABC", "N3XYZ", "KA1BCD", "WB2EFG", "KC3HIJ",
            // Canadian callsigns
            "VE3ABC", "VA7XYZ",
            // UK callsigns
            "G3ABC", "M0XYZ", "2E0ABC",
            // German callsigns
            "DL1ABC", "DJ2XYZ",
            // Japanese callsigns
            "JA1ABC", "JR2XYZ",
            // Special event callsigns
            "W100AW", "K1USA",
            // Portable/Mobile
            "W1AW/P", "K2ABC/M", "N3XYZ/MM"
    })
    @DisplayName("validateFormat - Valid Callsigns - Returns True")
    void validateFormat_validCallsigns_returnsTrue(String callsign) {
        // Act
        boolean result = callsignValidationService.validateFormat(callsign);

        // Assert
        assertThat(result).isTrue();
    }

    // ==================== INVALID CALLSIGN FORMAT TESTS ====================

    @ParameterizedTest(name = "Callsign {0} should be invalid")
    @ValueSource(strings = {
            // Invalid formats
            "ABC", "12345", "TOOLONG123456", "",
            // Invalid characters
            "W1AW!", "K2ABC#", "N3@XYZ",
            // Missing number
            "WABC", "KABCD",
            // Invalid structure
            "1W2ABC", "A1B2C3"
    })
    @DisplayName("validateFormat - Invalid Callsigns - Returns False")
    void validateFormat_invalidCallsigns_returnsFalse(String callsign) {
        // Act
        boolean result = callsignValidationService.validateFormat(callsign);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateFormat - Null Callsign - Returns False")
    void validateFormat_nullCallsign_returnsFalse() {
        // Act
        boolean result = callsignValidationService.validateFormat(null);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== CALLSIGN LOOKUP TESTS ====================

    @Test
    @DisplayName("lookupCallsign - Valid Callsign in Cache - Returns Cached Data")
    void lookupCallsign_validCallsignInCache_returnsCachedData() {
        // Arrange
        CallsignCache cachedData = CallsignCache.builder()
                .callsign("W1AW")
                .name("Hiram Percy Maxim Memorial Station")
                .address("225 Main St")
                .city("Newington")
                .state("CT")
                .zipCode("06111")
                .country("United States")
                .gridSquare("FN31pr")
                .licenseClass("CLUB")
                .lastUpdated(LocalDateTime.now().minusDays(1))
                .build();

        when(callsignCacheRepository.findByCallsign("W1AW")).thenReturn(Optional.of(cachedData));

        // Act
        Optional<CallsignCache> result = callsignValidationService.lookupCallsign("W1AW");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getCallsign()).isEqualTo("W1AW");
        assertThat(result.get().getName()).isEqualTo("Hiram Percy Maxim Memorial Station");
        assertThat(result.get().getGridSquare()).isEqualTo("FN31pr");
        verify(callsignCacheRepository).findByCallsign("W1AW");
    }

    @Test
    @DisplayName("lookupCallsign - Callsign Not in Cache - Returns Empty")
    void lookupCallsign_callsignNotInCache_returnsEmpty() {
        // Arrange
        when(callsignCacheRepository.findByCallsign("K9NEW")).thenReturn(Optional.empty());

        // Act
        Optional<CallsignCache> result = callsignValidationService.lookupCallsign("K9NEW");

        // Assert
        assertThat(result).isEmpty();
        verify(callsignCacheRepository).findByCallsign("K9NEW");
    }

    @Test
    @DisplayName("lookupCallsign - Case Insensitive - Normalizes to Uppercase")
    void lookupCallsign_caseInsensitive_normalizesToUppercase() {
        // Arrange
        CallsignCache cachedData = CallsignCache.builder()
                .callsign("W1AW")
                .name("Test Station")
                .build();

        when(callsignCacheRepository.findByCallsign("W1AW")).thenReturn(Optional.of(cachedData));

        // Act
        Optional<CallsignCache> result = callsignValidationService.lookupCallsign("w1aw");

        // Assert
        assertThat(result).isPresent();
        verify(callsignCacheRepository).findByCallsign("W1AW");
    }

    // ==================== CACHE UPDATE TESTS ====================

    @Test
    @DisplayName("updateCache - Valid Callsign Data - Saves to Cache")
    void updateCache_validCallsignData_savesToCache() {
        // Arrange
        CallsignCache newData = CallsignCache.builder()
                .callsign("K2ABC")
                .name("Test Operator")
                .state("NY")
                .gridSquare("FN30")
                .build();

        when(callsignCacheRepository.save(any(CallsignCache.class))).thenReturn(newData);

        // Act
        callsignValidationService.updateCache(newData);

        // Assert
        verify(callsignCacheRepository).save(any(CallsignCache.class));
    }

    @Test
    @DisplayName("updateCache - Existing Callsign - Updates Timestamp")
    void updateCache_existingCallsign_updatesTimestamp() {
        // Arrange
        CallsignCache existingData = CallsignCache.builder()
                .callsign("W1AW")
                .name("Old Name")
                .lastUpdated(LocalDateTime.now().minusDays(30))
                .build();

        CallsignCache updatedData = CallsignCache.builder()
                .callsign("W1AW")
                .name("New Name")
                .lastUpdated(LocalDateTime.now())
                .build();

        when(callsignCacheRepository.findByCallsign("W1AW")).thenReturn(Optional.of(existingData));
        when(callsignCacheRepository.save(any(CallsignCache.class))).thenReturn(updatedData);

        // Act
        callsignValidationService.updateCache(updatedData);

        // Assert
        verify(callsignCacheRepository).save(argThat(cache ->
                cache.getCallsign().equals("W1AW") &&
                cache.getName().equals("New Name")
        ));
    }

    // ==================== CALLSIGN PREFIX TESTS ====================

    @Test
    @DisplayName("extractPrefix - US Callsigns - Returns Correct Prefix")
    void extractPrefix_usCallsigns_returnsCorrectPrefix() {
        // Act & Assert
        assertThat(callsignValidationService.extractPrefix("W1AW")).isEqualTo("W1");
        assertThat(callsignValidationService.extractPrefix("K2ABC")).isEqualTo("K2");
        assertThat(callsignValidationService.extractPrefix("N3XYZ")).isEqualTo("N3");
        assertThat(callsignValidationService.extractPrefix("KA1BCD")).isEqualTo("KA1");
    }

    @Test
    @DisplayName("extractPrefix - International Callsigns - Returns Correct Prefix")
    void extractPrefix_internationalCallsigns_returnsCorrectPrefix() {
        // Act & Assert
        assertThat(callsignValidationService.extractPrefix("VE3ABC")).isEqualTo("VE3");
        assertThat(callsignValidationService.extractPrefix("G3ABC")).isEqualTo("G3");
        assertThat(callsignValidationService.extractPrefix("DL1ABC")).isEqualTo("DL1");
        assertThat(callsignValidationService.extractPrefix("JA1ABC")).isEqualTo("JA1");
    }

    @Test
    @DisplayName("extractPrefix - Portable/Mobile - Strips Suffix")
    void extractPrefix_portableMobile_stripsSuffix() {
        // Act & Assert
        assertThat(callsignValidationService.extractPrefix("W1AW/P")).isEqualTo("W1");
        assertThat(callsignValidationService.extractPrefix("K2ABC/M")).isEqualTo("K2");
        assertThat(callsignValidationService.extractPrefix("N3XYZ/MM")).isEqualTo("N3");
    }

    // ==================== COUNTRY DETECTION TESTS ====================

    @Test
    @DisplayName("getCountryFromPrefix - US Prefixes - Returns United States")
    void getCountryFromPrefix_usPrefixes_returnsUnitedStates() {
        // Act & Assert
        assertThat(callsignValidationService.getCountryFromPrefix("W1")).isEqualTo("United States");
        assertThat(callsignValidationService.getCountryFromPrefix("K2")).isEqualTo("United States");
        assertThat(callsignValidationService.getCountryFromPrefix("N3")).isEqualTo("United States");
        assertThat(callsignValidationService.getCountryFromPrefix("KA1")).isEqualTo("United States");
    }

    @Test
    @DisplayName("getCountryFromPrefix - Canadian Prefixes - Returns Canada")
    void getCountryFromPrefix_canadianPrefixes_returnsCanada() {
        // Act & Assert
        assertThat(callsignValidationService.getCountryFromPrefix("VE3")).isEqualTo("Canada");
        assertThat(callsignValidationService.getCountryFromPrefix("VA7")).isEqualTo("Canada");
    }

    @Test
    @DisplayName("getCountryFromPrefix - UK Prefixes - Returns United Kingdom")
    void getCountryFromPrefix_ukPrefixes_returnsUnitedKingdom() {
        // Act & Assert
        assertThat(callsignValidationService.getCountryFromPrefix("G3")).isEqualTo("United Kingdom");
        assertThat(callsignValidationService.getCountryFromPrefix("M0")).isEqualTo("United Kingdom");
        assertThat(callsignValidationService.getCountryFromPrefix("2E0")).isEqualTo("United Kingdom");
    }

    @Test
    @DisplayName("getCountryFromPrefix - German Prefixes - Returns Germany")
    void getCountryFromPrefix_germanPrefixes_returnsGermany() {
        // Act & Assert
        assertThat(callsignValidationService.getCountryFromPrefix("DL1")).isEqualTo("Germany");
        assertThat(callsignValidationService.getCountryFromPrefix("DJ2")).isEqualTo("Germany");
    }

    @Test
    @DisplayName("getCountryFromPrefix - Japanese Prefixes - Returns Japan")
    void getCountryFromPrefix_japanesePrefixes_returnsJapan() {
        // Act & Assert
        assertThat(callsignValidationService.getCountryFromPrefix("JA1")).isEqualTo("Japan");
        assertThat(callsignValidationService.getCountryFromPrefix("JR2")).isEqualTo("Japan");
    }

    @Test
    @DisplayName("getCountryFromPrefix - Unknown Prefix - Returns Unknown")
    void getCountryFromPrefix_unknownPrefix_returnsUnknown() {
        // Act & Assert
        assertThat(callsignValidationService.getCountryFromPrefix("ZZZ")).isEqualTo("Unknown");
    }

    // ==================== CACHE EXPIRATION TESTS ====================

    @Test
    @DisplayName("isCacheExpired - Old Cache Entry - Returns True")
    void isCacheExpired_oldCacheEntry_returnsTrue() {
        // Arrange
        CallsignCache oldCache = CallsignCache.builder()
                .callsign("W1AW")
                .lastUpdated(LocalDateTime.now().minusDays(31)) // 31 days old
                .build();

        // Act
        boolean result = callsignValidationService.isCacheExpired(oldCache, 30);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isCacheExpired - Recent Cache Entry - Returns False")
    void isCacheExpired_recentCacheEntry_returnsFalse() {
        // Arrange
        CallsignCache recentCache = CallsignCache.builder()
                .callsign("W1AW")
                .lastUpdated(LocalDateTime.now().minusDays(15)) // 15 days old
                .build();

        // Act
        boolean result = callsignValidationService.isCacheExpired(recentCache, 30);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isCacheExpired - Exactly at Expiration - Returns False")
    void isCacheExpired_exactlyAtExpiration_returnsFalse() {
        // Arrange
        CallsignCache cache = CallsignCache.builder()
                .callsign("W1AW")
                .lastUpdated(LocalDateTime.now().minusDays(30)) // Exactly 30 days
                .build();

        // Act
        boolean result = callsignValidationService.isCacheExpired(cache, 30);

        // Assert
        assertThat(result).isFalse();
    }

    // ==================== SPECIAL CALLSIGN TESTS ====================

    @Test
    @DisplayName("validateFormat - Special Event Callsigns - Validates Correctly")
    void validateFormat_specialEventCallsigns_validatesCorrectly() {
        // Act & Assert
        assertThat(callsignValidationService.validateFormat("W100AW")).isTrue(); // Centennial
        assertThat(callsignValidationService.validateFormat("K1USA")).isTrue();
        assertThat(callsignValidationService.validateFormat("W1FD")).isTrue(); // Field Day
    }

    @Test
    @DisplayName("validateFormat - Vanity Callsigns - Validates Correctly")
    void validateFormat_vanityCallsigns_validatesCorrectly() {
        // Act & Assert
        assertThat(callsignValidationService.validateFormat("AA1A")).isTrue(); // Short vanity
        assertThat(callsignValidationService.validateFormat("W1W")).isTrue(); // Very short vanity
        assertThat(callsignValidationService.validateFormat("K1TTT")).isTrue(); // Contest club
    }

    @Test
    @DisplayName("validateFormat - Maritime Mobile - Validates Correctly")
    void validateFormat_maritimeMobile_validatesCorrectly() {
        // Act & Assert
        assertThat(callsignValidationService.validateFormat("W1AW/MM")).isTrue();
        assertThat(callsignValidationService.validateFormat("K2ABC/AM")).isTrue(); // Aeronautical Mobile
    }

    @Test
    @DisplayName("validateFormat - Tactical Callsigns - Handles Appropriately")
    void validateFormat_tacticalCallsigns_handlesAppropriately() {
        // Tactical callsigns like "NCS" or "EOC" are not valid amateur callsigns
        // Act & Assert
        assertThat(callsignValidationService.validateFormat("NCS")).isFalse();
        assertThat(callsignValidationService.validateFormat("EOC")).isFalse();
        assertThat(callsignValidationService.validateFormat("NET")).isFalse();
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("validateFormat - Whitespace - Trims and Validates")
    void validateFormat_whitespace_trimsAndValidates() {
        // Act & Assert
        assertThat(callsignValidationService.validateFormat(" W1AW ")).isTrue();
        assertThat(callsignValidationService.validateFormat("  K2ABC  ")).isTrue();
    }

    @Test
    @DisplayName("validateFormat - Lowercase - Normalizes to Uppercase")
    void validateFormat_lowercase_normalizesToUppercase() {
        // Act & Assert
        assertThat(callsignValidationService.validateFormat("w1aw")).isTrue();
        assertThat(callsignValidationService.validateFormat("k2abc")).isTrue();
    }

    @Test
    @DisplayName("lookupCallsign - Empty String - Returns Empty")
    void lookupCallsign_emptyString_returnsEmpty() {
        // Act
        Optional<CallsignCache> result = callsignValidationService.lookupCallsign("");

        // Assert
        assertThat(result).isEmpty();
        verify(callsignCacheRepository, never()).findByCallsign(any());
    }

    @Test
    @DisplayName("lookupCallsign - Whitespace Only - Returns Empty")
    void lookupCallsign_whitespaceOnly_returnsEmpty() {
        // Act
        Optional<CallsignCache> result = callsignValidationService.lookupCallsign("   ");

        // Assert
        assertThat(result).isEmpty();
        verify(callsignCacheRepository, never()).findByCallsign(any());
    }
}
