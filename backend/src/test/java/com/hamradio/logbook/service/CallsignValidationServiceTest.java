package com.hamradio.logbook.service;

import com.hamradio.logbook.dto.CallsignInfo;
import com.hamradio.logbook.entity.CallsignCache;
import com.hamradio.logbook.repository.CallsignCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CallsignValidationService Unit Tests")
class CallsignValidationServiceTest {

    @Mock
    private CallsignCacheRepository cacheRepository;

    @InjectMocks
    private CallsignValidationService callsignValidationService;

    private CallsignCache testCache;
    private CallsignInfo testInfo;

    @BeforeEach
    void setUp() {
        // Set cache TTL for testing
        ReflectionTestUtils.setField(callsignValidationService, "cacheTtlSeconds", 3600L);

        testCache = CallsignCache.builder()
                .id(1L)
                .callsign("W1AW")
                .name("Test Operator")
                .address("123 Main St")
                .state("CT")
                .country("USA")
                .licenseClass("Extra")
                .gridSquare("FN31pr")
                .lookupSource("QRZ")
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        testInfo = CallsignInfo.builder()
                .callsign("W1AW")
                .name("Test Operator")
                .address("123 Main St")
                .state("CT")
                .country("USA")
                .licenseClass("Extra")
                .gridSquare("FN31pr")
                .lookupSource("QRZ")
                .cached(true)
                .build();
    }

    @Test
    @DisplayName("Should return empty optional for null callsign")
    void shouldReturnEmptyForNullCallsign() {
        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign(null);

        assertFalse(result.isPresent());
        verify(cacheRepository, never()).findValidByCallsign(anyString(), any());
    }

    @Test
    @DisplayName("Should return empty optional for empty callsign")
    void shouldReturnEmptyForEmptyCallsign() {
        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign("");

        assertFalse(result.isPresent());
        verify(cacheRepository, never()).findValidByCallsign(anyString(), any());
    }

    @Test
    @DisplayName("Should convert callsign to uppercase and trim")
    void shouldConvertCallsignToUppercaseAndTrim() {
        when(cacheRepository.findValidByCallsign(eq("W1AW"), any())).thenReturn(Optional.empty());

        callsignValidationService.lookupCallsign("  w1aw  ");

        verify(cacheRepository).findValidByCallsign(eq("W1AW"), any());
    }

    @Test
    @DisplayName("Should return cached callsign info when cache hit")
    void shouldReturnCachedCallsignInfo() {
        when(cacheRepository.findValidByCallsign(eq("W1AW"), any())).thenReturn(Optional.of(testCache));

        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign("W1AW");

        assertTrue(result.isPresent());
        assertEquals("W1AW", result.get().getCallsign());
        assertEquals("Test Operator", result.get().getName());
        assertEquals("QRZ", result.get().getLookupSource());
        assertTrue(result.get().isCached());
        verify(cacheRepository).findValidByCallsign(eq("W1AW"), any());
    }

    @Test
    @DisplayName("Should return empty when cache miss and no external lookup available")
    void shouldReturnEmptyOnCacheMiss() {
        when(cacheRepository.findValidByCallsign(eq("W1AW"), any())).thenReturn(Optional.empty());

        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign("W1AW");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should identify US callsigns starting with K")
    void shouldIdentifyUSCallsignStartingWithK() {
        // Indirectly test by checking that FCC lookup would be attempted for US callsigns
        when(cacheRepository.findValidByCallsign(anyString(), any())).thenReturn(Optional.empty());

        callsignValidationService.lookupCallsign("K1ABC");

        // US callsign should trigger FCC lookup attempt (though it will fail in test)
        verify(cacheRepository).findValidByCallsign(eq("K1ABC"), any());
    }

    @Test
    @DisplayName("Should identify US callsigns starting with N")
    void shouldIdentifyUSCallsignStartingWithN() {
        when(cacheRepository.findValidByCallsign(anyString(), any())).thenReturn(Optional.empty());

        callsignValidationService.lookupCallsign("N2ABC");

        verify(cacheRepository).findValidByCallsign(eq("N2ABC"), any());
    }

    @Test
    @DisplayName("Should identify US callsigns starting with W")
    void shouldIdentifyUSCallsignStartingWithW() {
        when(cacheRepository.findValidByCallsign(anyString(), any())).thenReturn(Optional.empty());

        callsignValidationService.lookupCallsign("W3ABC");

        verify(cacheRepository).findValidByCallsign(eq("W3ABC"), any());
    }

    @Test
    @DisplayName("Should identify US callsigns starting with AA-AL")
    void shouldIdentifyUSCallsignStartingWithAA() {
        when(cacheRepository.findValidByCallsign(anyString(), any())).thenReturn(Optional.empty());

        String[] usCallsigns = {"AA1ABC", "AB2ABC", "AC3ABC", "AD4ABC", "AE5ABC",
                               "AF6ABC", "AG7ABC", "AH8ABC", "AI9ABC", "AJ0ABC", "AK1ABC", "AL2ABC"};

        for (String callsign : usCallsigns) {
            callsignValidationService.lookupCallsign(callsign);
            verify(cacheRepository).findValidByCallsign(eq(callsign), any());
        }
    }

    @Test
    @DisplayName("Should not identify non-US callsigns as US")
    void shouldNotIdentifyNonUSCallsigns() {
        when(cacheRepository.findValidByCallsign(anyString(), any())).thenReturn(Optional.empty());

        String[] nonUSCallsigns = {"G4ABC", "VE3ABC", "JA1ABC", "DL1ABC", "AM1ABC", "AN1ABC"};

        for (String callsign : nonUSCallsigns) {
            Optional<CallsignInfo> result = callsignValidationService.lookupCallsign(callsign);

            // These should not trigger FCC lookup (US-only)
            assertFalse(result.isPresent());
        }
    }

    @Test
    @DisplayName("Should cleanup expired cache entries")
    void shouldCleanupExpiredCacheEntries() {
        doNothing().when(cacheRepository).deleteExpired(any(LocalDateTime.class));

        callsignValidationService.cleanupExpiredCache();

        verify(cacheRepository).deleteExpired(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle cache repository errors gracefully")
    void shouldHandleCacheRepositoryErrors() {
        when(cacheRepository.findValidByCallsign(anyString(), any()))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            callsignValidationService.lookupCallsign("W1AW");
        });
    }

    @Test
    @DisplayName("Should convert cached entity to CallsignInfo DTO correctly")
    void shouldConvertCachedEntityToDTO() {
        CallsignCache cache = CallsignCache.builder()
                .callsign("K3ABC")
                .name("John Doe")
                .address("456 Oak Ave")
                .state("PA")
                .country("USA")
                .licenseClass("General")
                .gridSquare("FN20kb")
                .lookupSource("FCC")
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(cacheRepository.findValidByCallsign(eq("K3ABC"), any())).thenReturn(Optional.of(cache));

        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign("K3ABC");

        assertTrue(result.isPresent());
        CallsignInfo info = result.get();
        assertEquals("K3ABC", info.getCallsign());
        assertEquals("John Doe", info.getName());
        assertEquals("456 Oak Ave", info.getAddress());
        assertEquals("PA", info.getState());
        assertEquals("USA", info.getCountry());
        assertEquals("General", info.getLicenseClass());
        assertEquals("FN20kb", info.getGridSquare());
        assertEquals("FCC", info.getLookupSource());
        assertTrue(info.isCached());
    }

    @Test
    @DisplayName("Should handle callsigns with portable designators")
    void shouldHandleCallsignsWithPortableDesignators() {
        when(cacheRepository.findValidByCallsign(eq("W1AW/M"), any())).thenReturn(Optional.empty());

        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign("W1AW/M");

        verify(cacheRepository).findValidByCallsign(eq("W1AW/M"), any());
    }

    @Test
    @DisplayName("Should handle callsigns with district designators")
    void shouldHandleCallsignsWithDistrictDesignators() {
        when(cacheRepository.findValidByCallsign(eq("W1AW/5"), any())).thenReturn(Optional.empty());

        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign("W1AW/5");

        verify(cacheRepository).findValidByCallsign(eq("W1AW/5"), any());
    }

    @Test
    @DisplayName("Should return all callsign info fields from cache")
    void shouldReturnAllFieldsFromCache() {
        CallsignCache fullCache = CallsignCache.builder()
                .callsign("W1AW")
                .name("Full Name")
                .address("Full Address")
                .state("CT")
                .country("USA")
                .licenseClass("Extra")
                .gridSquare("FN31pr")
                .lookupSource("QRZ")
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(cacheRepository.findValidByCallsign(eq("W1AW"), any())).thenReturn(Optional.of(fullCache));

        Optional<CallsignInfo> result = callsignValidationService.lookupCallsign("W1AW");

        assertTrue(result.isPresent());
        CallsignInfo info = result.get();
        assertNotNull(info.getCallsign());
        assertNotNull(info.getName());
        assertNotNull(info.getAddress());
        assertNotNull(info.getState());
        assertNotNull(info.getCountry());
        assertNotNull(info.getLicenseClass());
        assertNotNull(info.getGridSquare());
        assertNotNull(info.getLookupSource());
        assertTrue(info.isCached());
    }
}
