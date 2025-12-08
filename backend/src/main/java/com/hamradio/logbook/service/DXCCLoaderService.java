package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.DXCCPrefix;
import com.hamradio.logbook.repository.DXCCPrefixRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for loading DXCC prefix data from CTY.DAT file
 * CTY.DAT is the standard amateur radio country file format
 *
 * Format: Country:Continent:ITU Zone:CQ Zone:Lat:Lon:UTC Offset:Primary Prefix;
 *         Followed by alternate prefixes and special cases
 *
 * Example:
 * United States:NA:08:05:37.53:-97.14:-5.0:K;
 *     =W1AW,KH6,KL7,KP2,KP4,etc.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DXCCLoaderService {

    private final DXCCPrefixRepository dxccPrefixRepository;

    // CTY.DAT line pattern: Country:Continent:ITU:CQ:Lat:Lon:Offset:Prefix;
    private static final Pattern COUNTRY_PATTERN = Pattern.compile(
        "^(.+?):\\s*([A-Z]{2}):\\s*(\\d+):\\s*(\\d+):\\s*(-?[\\d.]+):\\s*(-?[\\d.]+):\\s*(-?[\\d.]+):\\s*([A-Z0-9/]+);\\s*$"
    );

    /**
     * Load DXCC data from CTY.DAT file in classpath
     */
    @Transactional
    public LoadResult loadFromClasspath(String resourcePath) {
        log.info("Loading DXCC data from classpath: {}", resourcePath);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return loadFromInputStream(is);
        } catch (IOException e) {
            log.error("Failed to load DXCC data from classpath: {}", e.getMessage());
            throw new RuntimeException("Failed to load DXCC data", e);
        }
    }

    /**
     * Load DXCC data from input stream
     */
    @Transactional
    public LoadResult loadFromInputStream(InputStream inputStream) throws IOException {
        log.info("Loading DXCC data from input stream");

        int countryCount = 0;
        int prefixCount = 0;
        List<DXCCPrefix> prefixesToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            String currentCountry = null;
            String currentContinent = null;
            Integer currentItuZone = null;
            Integer currentCqZone = null;
            BigDecimal currentLat = null;
            BigDecimal currentLon = null;
            Integer currentDxccCode = 1; // Start at 1, increment for each country

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Check if this is a country line (ends with semicolon after country data)
                Matcher matcher = COUNTRY_PATTERN.matcher(line);
                if (matcher.matches()) {
                    // New country definition
                    currentCountry = matcher.group(1).trim();
                    currentContinent = matcher.group(2).trim();
                    currentItuZone = Integer.parseInt(matcher.group(3));
                    currentCqZone = Integer.parseInt(matcher.group(4));
                    currentLat = new BigDecimal(matcher.group(5));
                    currentLon = new BigDecimal(matcher.group(6));
                    String primaryPrefix = matcher.group(8).trim();

                    log.debug("Parsing country: {} ({}) - Primary prefix: {}",
                        currentCountry, currentContinent, primaryPrefix);

                    // Add primary prefix
                    prefixesToSave.add(createPrefix(
                        primaryPrefix,
                        currentDxccCode,
                        currentCountry,
                        currentContinent,
                        currentCqZone,
                        currentItuZone,
                        currentLat,
                        currentLon,
                        false
                    ));
                    prefixCount++;
                    countryCount++;
                    currentDxccCode++;

                } else if (currentCountry != null) {
                    // This is a prefix line (alternate prefixes for current country)
                    // Parse comma-separated prefixes
                    String[] prefixes = line.split("[,;]");
                    for (String prefix : prefixes) {
                        prefix = prefix.trim();
                        if (prefix.isEmpty()) {
                            continue;
                        }

                        boolean exactMatch = false;

                        // Check for exact match prefix (starts with =)
                        if (prefix.startsWith("=")) {
                            exactMatch = true;
                            prefix = prefix.substring(1);
                        }

                        // Remove any trailing characters that aren't part of the prefix
                        prefix = prefix.replaceAll("[\\(\\)\\[\\]\\{\\}<>].*$", "").trim();

                        if (!prefix.isEmpty()) {
                            prefixesToSave.add(createPrefix(
                                prefix,
                                currentDxccCode - 1, // Use current country's DXCC code
                                currentCountry,
                                currentContinent,
                                currentCqZone,
                                currentItuZone,
                                currentLat,
                                currentLon,
                                exactMatch
                            ));
                            prefixCount++;
                        }
                    }
                }
            }
        }

        // Save all prefixes to database
        log.info("Saving {} prefixes for {} countries to database", prefixCount, countryCount);

        // Clear existing data
        dxccPrefixRepository.deleteAll();

        // Save in batches for better performance
        int batchSize = 1000;
        for (int i = 0; i < prefixesToSave.size(); i += batchSize) {
            int end = Math.min(i + batchSize, prefixesToSave.size());
            dxccPrefixRepository.saveAll(prefixesToSave.subList(i, end));
            log.debug("Saved batch {}/{}", end, prefixesToSave.size());
        }

        log.info("Successfully loaded {} countries with {} prefixes", countryCount, prefixCount);

        return LoadResult.builder()
            .success(true)
            .countriesLoaded(countryCount)
            .prefixesLoaded(prefixCount)
            .message("Successfully loaded DXCC data")
            .build();
    }

    /**
     * Create DXCCPrefix entity
     */
    private DXCCPrefix createPrefix(
            String prefix,
            Integer dxccCode,
            String entityName,
            String continent,
            Integer cqZone,
            Integer ituZone,
            BigDecimal lat,
            BigDecimal lon,
            boolean exactMatch
    ) {
        return DXCCPrefix.builder()
            .prefix(prefix)
            .dxccCode(dxccCode)
            .entityName(entityName)
            .continent(continent)
            .cqZone(cqZone)
            .ituZone(ituZone)
            .lat(lat)
            .lon(lon)
            .exactMatch(exactMatch)
            .build();
    }

    /**
     * Check if DXCC database is populated
     */
    public boolean isLoaded() {
        return dxccPrefixRepository.exists();
    }

    /**
     * Get count of loaded prefixes
     */
    public long getPrefixCount() {
        return dxccPrefixRepository.count();
    }

    // ===== Data Transfer Objects =====

    @lombok.Data
    @lombok.Builder
    public static class LoadResult {
        private boolean success;
        private int countriesLoaded;
        private int prefixesLoaded;
        private String message;
        private String error;
    }
}
