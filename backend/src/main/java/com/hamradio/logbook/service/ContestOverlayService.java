package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.repository.QSORepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for contest-specific map overlays and multiplier tracking
 * Provides geographic boundary data and worked/needed calculations
 *
 * Supports:
 * - ARRL Field Day sections (US/Canada state/province boundaries)
 * - CQ zones (40 zones worldwide)
 * - ITU zones (90 zones worldwide)
 * - IARU zones (for IARU HF Championship)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContestOverlayService {

    private final QSORepository qsoRepository;
    private final DXCCLookupService dxccLookupService;
    private final ObjectMapper objectMapper;

    /**
     * Get CQ zone overlay with worked/needed status
     */
    @Cacheable(value = "cqZoneOverlay", key = "#logId")
    public ZoneOverlayResponse getCQZoneOverlay(Long logId) {
        log.info("Generating CQ zone overlay for log {}", logId);

        // Get all QSOs for this log
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        // Extract CQ zones from QSOs (from DXCC lookup or QSO field)
        Set<Integer> workedZones = new HashSet<>();
        Map<Integer, Integer> zoneQSOCounts = new HashMap<>();

        for (QSO qso : qsos) {
            Integer cqZone = getCQZoneFromQSO(qso);
            if (cqZone != null && cqZone >= 1 && cqZone <= 40) {
                workedZones.add(cqZone);
                zoneQSOCounts.put(cqZone, zoneQSOCounts.getOrDefault(cqZone, 0) + 1);
            }
        }

        // Build zone status list (1-40)
        List<ZoneStatus> zones = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            zones.add(ZoneStatus.builder()
                .zoneNumber(i)
                .zoneName("CQ " + i)
                .worked(workedZones.contains(i))
                .qsoCount(zoneQSOCounts.getOrDefault(i, 0))
                .build());
        }

        return ZoneOverlayResponse.builder()
            .overlayType("CQ_ZONES")
            .totalZones(40)
            .workedCount(workedZones.size())
            .neededCount(40 - workedZones.size())
            .zones(zones)
            .build();
    }

    /**
     * Get ITU zone overlay with worked/needed status
     */
    @Cacheable(value = "ituZoneOverlay", key = "#logId")
    public ZoneOverlayResponse getITUZoneOverlay(Long logId) {
        log.info("Generating ITU zone overlay for log {}", logId);

        // Get all QSOs for this log
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        // Extract ITU zones from QSOs (from DXCC lookup or QSO field)
        Set<Integer> workedZones = new HashSet<>();
        Map<Integer, Integer> zoneQSOCounts = new HashMap<>();

        for (QSO qso : qsos) {
            Integer ituZone = getITUZoneFromQSO(qso);
            if (ituZone != null && ituZone >= 1 && ituZone <= 90) {
                workedZones.add(ituZone);
                zoneQSOCounts.put(ituZone, zoneQSOCounts.getOrDefault(ituZone, 0) + 1);
            }
        }

        // Build zone status list (1-90)
        List<ZoneStatus> zones = new ArrayList<>();
        for (int i = 1; i <= 90; i++) {
            zones.add(ZoneStatus.builder()
                .zoneNumber(i)
                .zoneName("ITU " + i)
                .worked(workedZones.contains(i))
                .qsoCount(zoneQSOCounts.getOrDefault(i, 0))
                .build());
        }

        return ZoneOverlayResponse.builder()
            .overlayType("ITU_ZONES")
            .totalZones(90)
            .workedCount(workedZones.size())
            .neededCount(90 - workedZones.size())
            .zones(zones)
            .build();
    }

    /**
     * Get ARRL section overlay (US/Canada sections for Field Day, Sweepstakes, etc.)
     */
    @Cacheable(value = "arrlSectionOverlay", key = "#logId")
    public SectionOverlayResponse getARRLSectionOverlay(Long logId) {
        log.info("Generating ARRL section overlay for log {}", logId);

        // Get all QSOs for this log
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        // Extract sections from QSOs (from exchange field)
        Set<String> workedSections = new HashSet<>();
        Map<String, Integer> sectionQSOCounts = new HashMap<>();

        for (QSO qso : qsos) {
            String section = getARRLSectionFromQSO(qso);
            if (section != null && !section.isBlank()) {
                section = section.toUpperCase().trim();
                workedSections.add(section);
                sectionQSOCounts.put(section, sectionQSOCounts.getOrDefault(section, 0) + 1);
            }
        }

        // Get list of all ARRL sections (83 total)
        List<String> allSections = getAllARRLSections();

        // Build section status list
        List<SectionStatus> sections = allSections.stream()
            .map(section -> SectionStatus.builder()
                .sectionCode(section)
                .sectionName(getARRLSectionName(section))
                .worked(workedSections.contains(section))
                .qsoCount(sectionQSOCounts.getOrDefault(section, 0))
                .build())
            .collect(Collectors.toList());

        return SectionOverlayResponse.builder()
            .overlayType("ARRL_SECTIONS")
            .totalSections(allSections.size())
            .workedCount(workedSections.size())
            .neededCount(allSections.size() - workedSections.size())
            .sections(sections)
            .build();
    }

    /**
     * Get DXCC entity overlay (worked vs needed)
     */
    @Cacheable(value = "dxccOverlay", key = "#logId")
    public DXCCOverlayResponse getDXCCOverlay(Long logId) {
        log.info("Generating DXCC overlay for log {}", logId);

        // Get all QSOs for this log
        List<QSO> qsos = qsoRepository.findAllByLogId(logId);

        // Extract DXCC entities from QSOs
        Set<Integer> workedDXCC = new HashSet<>();
        Map<Integer, Integer> dxccQSOCounts = new HashMap<>();
        Map<Integer, String> dxccNames = new HashMap<>();

        for (QSO qso : qsos) {
            String callsign = qso.getCallsign();
            if (callsign != null && !callsign.isBlank()) {
                DXCCLookupService.DXCCInfo dxcc = dxccLookupService.lookup(callsign);
                if (dxcc != null && dxcc.getDxccCode() != null) {
                    Integer dxccCode = dxcc.getDxccCode();
                    workedDXCC.add(dxccCode);
                    dxccQSOCounts.put(dxccCode, dxccQSOCounts.getOrDefault(dxccCode, 0) + 1);
                    dxccNames.putIfAbsent(dxccCode, dxcc.getEntityName());
                }
            }
        }

        // Build DXCC status list
        List<DXCCStatus> entities = workedDXCC.stream()
            .map(dxccCode -> DXCCStatus.builder()
                .dxccCode(dxccCode)
                .entityName(dxccNames.get(dxccCode))
                .qsoCount(dxccQSOCounts.get(dxccCode))
                .build())
            .sorted(Comparator.comparing(DXCCStatus::getEntityName))
            .collect(Collectors.toList());

        return DXCCOverlayResponse.builder()
            .overlayType("DXCC_ENTITIES")
            .totalEntities(340) // Current DXCC list has 340 entities
            .workedCount(workedDXCC.size())
            .neededCount(340 - workedDXCC.size())
            .entities(entities)
            .build();
    }

    /**
     * Get multiplier summary for a contest type
     */
    public MultiplierSummary getMultiplierSummary(Long logId, ContestType contestType) {
        log.info("Generating multiplier summary for log {} - contest type: {}", logId, contestType);

        return switch (contestType) {
            case ARRL_FIELD_DAY, ARRL_SWEEPSTAKES -> {
                SectionOverlayResponse sections = getARRLSectionOverlay(logId);
                yield MultiplierSummary.builder()
                    .contestType(contestType.name())
                    .multiplierType("ARRL Sections")
                    .totalMultipliers(sections.getTotalSections())
                    .workedMultipliers(sections.getWorkedCount())
                    .neededMultipliers(sections.getNeededCount())
                    .percentageComplete((sections.getWorkedCount() * 100.0) / sections.getTotalSections())
                    .build();
            }
            case CQ_WW_DX, CQ_WPX -> {
                ZoneOverlayResponse zones = getCQZoneOverlay(logId);
                yield MultiplierSummary.builder()
                    .contestType(contestType.name())
                    .multiplierType("CQ Zones")
                    .totalMultipliers(zones.getTotalZones())
                    .workedMultipliers(zones.getWorkedCount())
                    .neededMultipliers(zones.getNeededCount())
                    .percentageComplete((zones.getWorkedCount() * 100.0) / zones.getTotalZones())
                    .build();
            }
            case IARU_HF -> {
                ZoneOverlayResponse zones = getITUZoneOverlay(logId);
                yield MultiplierSummary.builder()
                    .contestType(contestType.name())
                    .multiplierType("ITU Zones")
                    .totalMultipliers(zones.getTotalZones())
                    .workedMultipliers(zones.getWorkedCount())
                    .neededMultipliers(zones.getNeededCount())
                    .percentageComplete((zones.getWorkedCount() * 100.0) / zones.getTotalZones())
                    .build();
            }
            case DXCC_CHALLENGE -> {
                DXCCOverlayResponse dxcc = getDXCCOverlay(logId);
                yield MultiplierSummary.builder()
                    .contestType(contestType.name())
                    .multiplierType("DXCC Entities")
                    .totalMultipliers(dxcc.getTotalEntities())
                    .workedMultipliers(dxcc.getWorkedCount())
                    .neededMultipliers(dxcc.getNeededCount())
                    .percentageComplete((dxcc.getWorkedCount() * 100.0) / dxcc.getTotalEntities())
                    .build();
            }
        };
    }

    // ===== Helper Methods =====

    /**
     * Extract CQ zone from QSO
     * Priority: QSO.cqZone field, then DXCC lookup
     */
    private Integer getCQZoneFromQSO(QSO qso) {
        // First try QSO field (if explicitly logged)
        if (qso.getCqZone() != null) {
            return qso.getCqZone();
        }

        // Fallback to DXCC lookup
        String callsign = qso.getCallsign();
        if (callsign != null && !callsign.isBlank()) {
            DXCCLookupService.DXCCInfo dxcc = dxccLookupService.lookup(callsign);
            if (dxcc != null) {
                return dxcc.getCqZone();
            }
        }

        return null;
    }

    /**
     * Extract ITU zone from QSO
     */
    private Integer getITUZoneFromQSO(QSO qso) {
        // First try QSO field (if explicitly logged)
        if (qso.getItuZone() != null) {
            return qso.getItuZone();
        }

        // Fallback to DXCC lookup
        String callsign = qso.getCallsign();
        if (callsign != null && !callsign.isBlank()) {
            DXCCLookupService.DXCCInfo dxcc = dxccLookupService.lookup(callsign);
            if (dxcc != null) {
                return dxcc.getItuZone();
            }
        }

        return null;
    }

    /**
     * Extract ARRL section from QSO contest data
     * Checks contestData JSON field for "section" key
     */
    private String getARRLSectionFromQSO(QSO qso) {
        // ARRL sections are stored in contestData JSON field
        if (qso.getContestData() != null && !qso.getContestData().isBlank()) {
            try {
                Map<String, Object> contestData = objectMapper.readValue(
                    qso.getContestData(),
                    Map.class
                );

                // Look for "section" key in contest data
                Object section = contestData.get("section");
                if (section != null) {
                    String sectionStr = section.toString().toUpperCase().trim();

                    // Validate against known sections
                    List<String> allSections = getAllARRLSections();
                    if (allSections.contains(sectionStr)) {
                        return sectionStr;
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse contest data for QSO {}: {}", qso.getId(), e.getMessage());
            }
        }

        return null;
    }

    /**
     * Get list of all 83 ARRL sections
     */
    private List<String> getAllARRLSections() {
        return Arrays.asList(
            // US Sections
            "CT", "EMA", "ME", "NH", "RI", "VT", "WMA",  // New England Division
            "ENY", "NLI", "NNJ", "NNY", "SNJ", "WNY",    // Atlantic Division
            "DE", "EPA", "MDC", "WPA",                    // Delta Division
            "AL", "GA", "KY", "NC", "NFL", "PR", "SC", "SFL", "TN", "VI", "VA", "WCF", // Southeastern Division
            "AR", "LA", "MS", "NM", "NTX", "OK", "STX", "WTX", // Delta Division & West Gulf Division
            "EB", "LAX", "ORG", "SB", "SC", "SCV", "SDG", "SF", "SJV", "SV", "PAC", // Pacific Division
            "AZ", "EWA", "ID", "MT", "NV", "OR", "UT", "WWA", "WY", "AK", // Northwestern Division
            "MI", "OH", "WV",                             // Great Lakes Division
            "IL", "IN", "WI",                             // Central Division
            "CO", "IA", "KS", "MO", "MN", "NE", "ND", "SD", // Rocky Mountain & Dakota Division
            // Canadian Sections
            "AB", "BC", "GTA", "MB", "NB", "NL", "NS", "NT", "ONE", "ONN", "ONS", "PE", "QC", "SK", "TER"
        );
    }

    /**
     * Get human-readable name for ARRL section code
     */
    private String getARRLSectionName(String code) {
        // Map of section codes to names (subset shown)
        Map<String, String> sectionNames = Map.ofEntries(
            Map.entry("CT", "Connecticut"),
            Map.entry("EMA", "Eastern Massachusetts"),
            Map.entry("ME", "Maine"),
            Map.entry("NH", "New Hampshire"),
            Map.entry("RI", "Rhode Island"),
            Map.entry("VT", "Vermont"),
            Map.entry("WMA", "Western Massachusetts"),
            Map.entry("ENY", "Eastern New York"),
            Map.entry("NLI", "Northern Long Island"),
            Map.entry("NNJ", "Northern New Jersey"),
            Map.entry("AL", "Alabama"),
            Map.entry("GA", "Georgia"),
            Map.entry("KY", "Kentucky"),
            Map.entry("NC", "North Carolina"),
            Map.entry("SC", "South Carolina"),
            Map.entry("TN", "Tennessee"),
            Map.entry("VA", "Virginia"),
            Map.entry("AB", "Alberta"),
            Map.entry("BC", "British Columbia"),
            Map.entry("MB", "Manitoba"),
            Map.entry("SK", "Saskatchewan")
            // ... (full list would include all 83 sections)
        );

        return sectionNames.getOrDefault(code, code);
    }

    // ===== Data Transfer Objects =====

    @Data
    @Builder
    public static class ZoneOverlayResponse {
        private String overlayType; // CQ_ZONES, ITU_ZONES
        private int totalZones;
        private int workedCount;
        private int neededCount;
        private List<ZoneStatus> zones;
    }

    @Data
    @Builder
    public static class ZoneStatus {
        private Integer zoneNumber;
        private String zoneName;
        private boolean worked;
        private int qsoCount;
    }

    @Data
    @Builder
    public static class SectionOverlayResponse {
        private String overlayType; // ARRL_SECTIONS
        private int totalSections;
        private int workedCount;
        private int neededCount;
        private List<SectionStatus> sections;
    }

    @Data
    @Builder
    public static class SectionStatus {
        private String sectionCode;
        private String sectionName;
        private boolean worked;
        private int qsoCount;
    }

    @Data
    @Builder
    public static class DXCCOverlayResponse {
        private String overlayType; // DXCC_ENTITIES
        private int totalEntities;
        private int workedCount;
        private int neededCount;
        private List<DXCCStatus> entities;
    }

    @Data
    @Builder
    public static class DXCCStatus {
        private Integer dxccCode;
        private String entityName;
        private int qsoCount;
    }

    @Data
    @Builder
    public static class MultiplierSummary {
        private String contestType;
        private String multiplierType;
        private int totalMultipliers;
        private int workedMultipliers;
        private int neededMultipliers;
        private double percentageComplete;
    }

    public enum ContestType {
        ARRL_FIELD_DAY,
        ARRL_SWEEPSTAKES,
        CQ_WW_DX,
        CQ_WPX,
        IARU_HF,
        DXCC_CHALLENGE
    }
}
