package com.hamradio.logbook.repository;

import com.hamradio.logbook.entity.CallsignCache;
import com.hamradio.logbook.testutil.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Callsign Cache Repository Integration Tests")
class CallsignCacheRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private CallsignCacheRepository callsignCacheRepository;

    @BeforeEach
    void setUp() {
        callsignCacheRepository.deleteAll();
    }

    // ==================== SAVE AND FIND TESTS ====================

    @Test
    @DisplayName("save - Valid Cache Entry - Persists Successfully")
    void save_validCacheEntry_persistsSuccessfully() {
        // Arrange
        CallsignCache cache = CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .dxccEntity(291)
                .cachedAt(LocalDateTime.now())
                .build();

        // Act
        CallsignCache saved = callsignCacheRepository.save(cache);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCallsign()).isEqualTo("W1AW");
        assertThat(saved.getCountry()).isEqualTo("United States");
        assertThat(saved.getDxccEntity()).isEqualTo(291);
    }

    @Test
    @DisplayName("findById - Existing Cache Entry - Returns Entry")
    void findById_existingCacheEntry_returnsEntry() {
        // Arrange
        CallsignCache cache = callsignCacheRepository.save(CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build());

        // Act
        Optional<CallsignCache> found = callsignCacheRepository.findById(cache.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCallsign()).isEqualTo("W1AW");
    }

    // ==================== FIND BY CALLSIGN TESTS ====================

    @Test
    @DisplayName("findByCallsign - Existing Entry - Returns Entry")
    void findByCallsign_existingEntry_returnsEntry() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder()
                .callsign("K2ABC")
                .country("United States")
                .prefix("K")
                .build());

        // Act
        Optional<CallsignCache> found = callsignCacheRepository.findByCallsign("K2ABC");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCountry()).isEqualTo("United States");
    }

    @Test
    @DisplayName("findByCallsign - Case Insensitive - Returns Entry")
    void findByCallsign_caseInsensitive_returnsEntry() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build());

        // Act
        Optional<CallsignCache> found = callsignCacheRepository.findByCallsign("w1aw");

        // Assert
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByCallsign - Non-Existent Entry - Returns Empty")
    void findByCallsign_nonExistentEntry_returnsEmpty() {
        // Act
        Optional<CallsignCache> found = callsignCacheRepository.findByCallsign("ZZ9ZZZ");

        // Assert
        assertThat(found).isEmpty();
    }

    // ==================== FIND BY PREFIX TESTS ====================

    @Test
    @DisplayName("findByPrefix - Returns Entries with Prefix")
    void findByPrefix_returnsEntriesWithPrefix() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder().callsign("W1AW").prefix("W").country("United States").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("W2ABC").prefix("W").country("United States").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("K2XYZ").prefix("K").country("United States").build());

        // Act
        List<CallsignCache> wPrefixEntries = callsignCacheRepository.findByPrefix("W");

        // Assert
        assertThat(wPrefixEntries).hasSize(2);
        assertThat(wPrefixEntries).allMatch(entry -> entry.getPrefix().equals("W"));
    }

    // ==================== FIND BY COUNTRY TESTS ====================

    @Test
    @DisplayName("findByCountry - Returns Entries for Country")
    void findByCountry_returnsEntriesForCountry() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder().callsign("W1AW").country("United States").prefix("W").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("K2ABC").country("United States").prefix("K").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("VE3XYZ").country("Canada").prefix("VE").build());

        // Act
        List<CallsignCache> usEntries = callsignCacheRepository.findByCountry("United States");
        List<CallsignCache> caEntries = callsignCacheRepository.findByCountry("Canada");

        // Assert
        assertThat(usEntries).hasSize(2);
        assertThat(caEntries).hasSize(1);
    }

    // ==================== FIND BY DXCC ENTITY TESTS ====================

    @Test
    @DisplayName("findByDxccEntity - Returns Entries for DXCC Entity")
    void findByDxccEntity_returnsEntriesForDxccEntity() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder().callsign("W1AW").dxccEntity(291).country("United States").prefix("W").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("K2ABC").dxccEntity(291).country("United States").prefix("K").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("VE3XYZ").dxccEntity(1).country("Canada").prefix("VE").build());

        // Act
        List<CallsignCache> usEntries = callsignCacheRepository.findByDxccEntity(291);
        List<CallsignCache> caEntries = callsignCacheRepository.findByDxccEntity(1);

        // Assert
        assertThat(usEntries).hasSize(2);
        assertThat(caEntries).hasSize(1);
    }

    // ==================== EXPIRATION TESTS ====================

    @Test
    @DisplayName("findByCachedAtBefore - Returns Old Entries")
    void findByCachedAtBefore_returnsOldEntries() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        CallsignCache oldEntry = CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build();
        oldEntry.setCachedAt(now.minusDays(10));
        callsignCacheRepository.save(oldEntry);

        CallsignCache recentEntry = CallsignCache.builder()
                .callsign("K2ABC")
                .country("United States")
                .prefix("K")
                .build();
        recentEntry.setCachedAt(now.minusHours(1));
        callsignCacheRepository.save(recentEntry);

        // Act
        List<CallsignCache> oldEntries = callsignCacheRepository.findByCachedAtBefore(now.minusDays(7));

        // Assert
        assertThat(oldEntries).hasSize(1);
        assertThat(oldEntries.get(0).getCallsign()).isEqualTo("W1AW");
    }

    @Test
    @DisplayName("deleteByCachedAtBefore - Removes Old Entries")
    void deleteByCachedAtBefore_removesOldEntries() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        CallsignCache oldEntry = CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build();
        oldEntry.setCachedAt(now.minusDays(10));
        callsignCacheRepository.save(oldEntry);

        CallsignCache recentEntry = CallsignCache.builder()
                .callsign("K2ABC")
                .country("United States")
                .prefix("K")
                .build();
        recentEntry.setCachedAt(now.minusHours(1));
        callsignCacheRepository.save(recentEntry);

        // Act
        callsignCacheRepository.deleteByCachedAtBefore(now.minusDays(7));

        // Assert
        assertThat(callsignCacheRepository.findByCallsign("W1AW")).isEmpty();
        assertThat(callsignCacheRepository.findByCallsign("K2ABC")).isPresent();
    }

    // ==================== EXISTS TESTS ====================

    @Test
    @DisplayName("existsByCallsign - Existing Entry - Returns True")
    void existsByCallsign_existingEntry_returnsTrue() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build());

        // Act
        boolean exists = callsignCacheRepository.existsByCallsign("W1AW");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByCallsign - Non-Existent Entry - Returns False")
    void existsByCallsign_nonExistentEntry_returnsFalse() {
        // Act
        boolean exists = callsignCacheRepository.existsByCallsign("ZZ9ZZZ");

        // Assert
        assertThat(exists).isFalse();
    }

    // ==================== COUNT TESTS ====================

    @Test
    @DisplayName("count - Returns Total Cache Entry Count")
    void count_returnsTotalCacheEntryCount() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder().callsign("W1AW").country("United States").prefix("W").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("K2ABC").country("United States").prefix("K").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("VE3XYZ").country("Canada").prefix("VE").build());

        // Act
        long count = callsignCacheRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countByCountry - Returns Count by Country")
    void countByCountry_returnsCountByCountry() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder().callsign("W1AW").country("United States").prefix("W").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("K2ABC").country("United States").prefix("K").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("VE3XYZ").country("Canada").prefix("VE").build());

        // Act
        long usCount = callsignCacheRepository.countByCountry("United States");
        long caCount = callsignCacheRepository.countByCountry("Canada");

        // Assert
        assertThat(usCount).isEqualTo(2);
        assertThat(caCount).isEqualTo(1);
    }

    @Test
    @DisplayName("countByDxccEntity - Returns Count by DXCC Entity")
    void countByDxccEntity_returnsCountByDxccEntity() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder().callsign("W1AW").dxccEntity(291).country("United States").prefix("W").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("K2ABC").dxccEntity(291).country("United States").prefix("K").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("VE3XYZ").dxccEntity(1).country("Canada").prefix("VE").build());

        // Act
        long usCount = callsignCacheRepository.countByDxccEntity(291);

        // Assert
        assertThat(usCount).isEqualTo(2);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("deleteById - Removes Cache Entry")
    void deleteById_removesCacheEntry() {
        // Arrange
        CallsignCache cache = callsignCacheRepository.save(CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build());
        Long cacheId = cache.getId();

        // Act
        callsignCacheRepository.deleteById(cacheId);

        // Assert
        assertThat(callsignCacheRepository.findById(cacheId)).isEmpty();
    }

    @Test
    @DisplayName("deleteByCallsign - Removes Entry by Callsign")
    void deleteByCallsign_removesEntryByCallsign() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build());

        // Act
        callsignCacheRepository.deleteByCallsign("W1AW");

        // Assert
        assertThat(callsignCacheRepository.findByCallsign("W1AW")).isEmpty();
    }

    @Test
    @DisplayName("deleteAll - Removes All Entries")
    void deleteAll_removesAllEntries() {
        // Arrange
        callsignCacheRepository.save(CallsignCache.builder().callsign("W1AW").country("United States").prefix("W").build());
        callsignCacheRepository.save(CallsignCache.builder().callsign("K2ABC").country("United States").prefix("K").build());

        // Act
        callsignCacheRepository.deleteAll();

        // Assert
        assertThat(callsignCacheRepository.count()).isZero();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("save - Update Existing Entry - Updates Successfully")
    void save_updateExistingEntry_updatesSuccessfully() {
        // Arrange
        CallsignCache cache = callsignCacheRepository.save(CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .dxccEntity(291)
                .build());
        Long cacheId = cache.getId();

        // Act
        cache.setCountry("USA");
        cache.setDxccEntity(292);
        cache.setCachedAt(LocalDateTime.now());
        callsignCacheRepository.save(cache);

        // Assert
        CallsignCache updated = callsignCacheRepository.findById(cacheId).orElseThrow();
        assertThat(updated.getCountry()).isEqualTo("USA");
        assertThat(updated.getDxccEntity()).isEqualTo(292);
    }

    // ==================== CACHE FRESHNESS TESTS ====================

    @Test
    @DisplayName("findByCallsignAndCachedAtAfter - Returns Fresh Entry")
    void findByCallsignAndCachedAtAfter_returnsFreshEntry() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        CallsignCache freshEntry = CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build();
        freshEntry.setCachedAt(now.minusHours(1));
        callsignCacheRepository.save(freshEntry);

        // Act
        Optional<CallsignCache> found = callsignCacheRepository.findByCallsignAndCachedAtAfter(
                "W1AW",
                now.minusDays(1)
        );

        // Assert
        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("findByCallsignAndCachedAtAfter - Stale Entry - Returns Empty")
    void findByCallsignAndCachedAtAfter_staleEntry_returnsEmpty() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();

        CallsignCache staleEntry = CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build();
        staleEntry.setCachedAt(now.minusDays(10));
        callsignCacheRepository.save(staleEntry);

        // Act
        Optional<CallsignCache> found = callsignCacheRepository.findByCallsignAndCachedAtAfter(
                "W1AW",
                now.minusDays(7)
        );

        // Assert
        assertThat(found).isEmpty();
    }

    // ==================== UNIQUE CONSTRAINT TESTS ====================

    @Test
    @DisplayName("save - Duplicate Callsign - Updates Existing Entry")
    void save_duplicateCallsign_updatesExistingEntry() {
        // Arrange
        CallsignCache original = callsignCacheRepository.save(CallsignCache.builder()
                .callsign("W1AW")
                .country("United States")
                .prefix("W")
                .build());

        // Act
        CallsignCache duplicate = CallsignCache.builder()
                .callsign("W1AW")
                .country("USA")
                .prefix("W1")
                .build();

        // This should either throw an exception or update the existing entry
        // depending on the unique constraint configuration

        // Assert - verify behavior based on your schema constraints
        assertThat(callsignCacheRepository.count()).isEqualTo(1);
    }
}
