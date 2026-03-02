package com.hamradio.logbook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamradio.logbook.dto.CallsignInfo;
import com.hamradio.logbook.entity.CallsignCache;
import com.hamradio.logbook.repository.CallsignCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for validating and looking up callsigns
 * Supports QRZ XML API and FCC ULS with caching
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallsignValidationService {

    private static final String QRZ_XML_URL = "https://xmldata.qrz.com/xml/current/";
    private static final String QRZ_AGENT = "EtherWaveArchive/1.0";
    private static final String FCC_API_URL = "https://data.fcc.gov/api/license-view/basicSearch/getLicenses";

    // Session key expires roughly every 24 hours; refresh proactively after 20 hours
    private static final long SESSION_TTL_SECONDS = 72_000;

    private final CallsignCacheRepository cacheRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${qrz.api.username:}")
    private String qrzUsername;

    @Value("${qrz.api.password:}")
    private String qrzPassword;

    @Value("${callsign.validation.cache.ttl:3600}")
    private long cacheTtlSeconds;

    // QRZ session state — volatile for thread visibility
    private volatile String qrzSessionKey;
    private volatile LocalDateTime qrzSessionExpiry = LocalDateTime.MIN;

    /**
     * Lookup callsign information with caching
     */
    @Transactional
    public Optional<CallsignInfo> lookupCallsign(String callsign) {
        if (callsign == null || callsign.isEmpty()) {
            return Optional.empty();
        }

        callsign = callsign.toUpperCase().trim();

        // Check cache first
        Optional<CallsignCache> cached = cacheRepository.findValidByCallsign(callsign, LocalDateTime.now());
        if (cached.isPresent()) {
            log.debug("Cache hit for callsign: {}", callsign);
            return Optional.of(fromCache(cached.get()));
        }

        log.debug("Cache miss for callsign: {}, attempting external lookup", callsign);

        // Try QRZ if credentials are configured
        if (hasQrzCredentials()) {
            Optional<CallsignInfo> qrzInfo = lookupQRZ(callsign);
            if (qrzInfo.isPresent()) {
                cacheCallsignInfo(callsign, qrzInfo.get(), "QRZ");
                return qrzInfo;
            }
        }

        // Try FCC database for US callsigns
        if (isUSCallsign(callsign)) {
            Optional<CallsignInfo> fccInfo = lookupFCC(callsign);
            if (fccInfo.isPresent()) {
                cacheCallsignInfo(callsign, fccInfo.get(), "FCC");
                return fccInfo;
            }
        }

        log.warn("No information found for callsign: {}", callsign);
        return Optional.empty();
    }

    /**
     * Lookup callsign via QRZ XML API
     * Uses session-based authentication with automatic session refresh
     */
    private Optional<CallsignInfo> lookupQRZ(String callsign) {
        try {
            String sessionKey = getQrzSessionKey();
            if (sessionKey == null) {
                log.warn("Could not obtain QRZ session key");
                return Optional.empty();
            }

            String url = QRZ_XML_URL + "?s=" + encode(sessionKey) + "&callsign=" + encode(callsign);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return Optional.empty();
            }

            String xml = response.getBody();

            // Check for session timeout in response — refresh and retry once
            if (xml.contains("Session Timeout") || xml.contains("Invalid session key")) {
                log.info("QRZ session expired, refreshing...");
                qrzSessionKey = null;
                qrzSessionExpiry = LocalDateTime.MIN;
                sessionKey = getQrzSessionKey();
                if (sessionKey == null) {
                    return Optional.empty();
                }
                url = QRZ_XML_URL + "?s=" + encode(sessionKey) + "&callsign=" + encode(callsign);
                response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                    return Optional.empty();
                }
                xml = response.getBody();
            }

            return parseQrzCallsignResponse(xml);

        } catch (Exception e) {
            log.error("Error looking up callsign on QRZ: {}", callsign, e);
            return Optional.empty();
        }
    }

    /**
     * Get or refresh QRZ session key
     */
    private synchronized String getQrzSessionKey() {
        if (qrzSessionKey != null && LocalDateTime.now().isBefore(qrzSessionExpiry)) {
            return qrzSessionKey;
        }

        try {
            String url = QRZ_XML_URL + "?username=" + encode(qrzUsername)
                    + "&password=" + encode(qrzPassword)
                    + "&agent=" + encode(QRZ_AGENT);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("QRZ authentication failed: HTTP {}", response.getStatusCode());
                return null;
            }

            String xml = response.getBody();
            Document doc = parseXml(xml);
            if (doc == null) {
                return null;
            }

            // Check for session error
            NodeList errorNodes = doc.getElementsByTagName("Error");
            if (errorNodes.getLength() > 0) {
                log.error("QRZ authentication error: {}", errorNodes.item(0).getTextContent());
                return null;
            }

            NodeList keyNodes = doc.getElementsByTagName("Key");
            if (keyNodes.getLength() == 0) {
                log.error("No session key in QRZ response");
                return null;
            }

            qrzSessionKey = keyNodes.item(0).getTextContent().trim();
            qrzSessionExpiry = LocalDateTime.now().plusSeconds(SESSION_TTL_SECONDS);
            log.info("QRZ session established");
            return qrzSessionKey;

        } catch (Exception e) {
            log.error("Error authenticating with QRZ", e);
            return null;
        }
    }

    /**
     * Parse QRZ XML callsign lookup response
     */
    private Optional<CallsignInfo> parseQrzCallsignResponse(String xml) {
        try {
            Document doc = parseXml(xml);
            if (doc == null) {
                return Optional.empty();
            }

            // Check for errors in the session element
            NodeList errorNodes = doc.getElementsByTagName("Error");
            if (errorNodes.getLength() > 0) {
                log.debug("QRZ returned error: {}", errorNodes.item(0).getTextContent());
                return Optional.empty();
            }

            NodeList callsignNodes = doc.getElementsByTagName("Callsign");
            if (callsignNodes.getLength() == 0) {
                return Optional.empty();
            }

            Element callsignEl = (Element) callsignNodes.item(0);

            String firstName = getElementText(callsignEl, "fname");
            String lastName = getElementText(callsignEl, "name");
            String fullName = buildFullName(firstName, lastName);

            // addr2 is city, addr1 is street
            String addr1 = getElementText(callsignEl, "addr1");
            String addr2 = getElementText(callsignEl, "addr2");
            String address = buildAddress(addr1, addr2);

            CallsignInfo info = CallsignInfo.builder()
                    .callsign(getElementText(callsignEl, "call"))
                    .name(fullName)
                    .address(address)
                    .state(getElementText(callsignEl, "state"))
                    .country(getElementText(callsignEl, "country"))
                    .licenseClass(getElementText(callsignEl, "class"))
                    .gridSquare(getElementText(callsignEl, "grid"))
                    .lookupSource("QRZ")
                    .cached(false)
                    .build();

            return Optional.of(info);

        } catch (Exception e) {
            log.error("Error parsing QRZ XML response", e);
            return Optional.empty();
        }
    }

    /**
     * Lookup callsign via FCC ULS API (US callsigns only)
     */
    private Optional<CallsignInfo> lookupFCC(String callsign) {
        try {
            String url = FCC_API_URL + "?searchValue=" + encode(callsign) + "&format=json";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (!"OK".equals(root.path("status").asText())) {
                return Optional.empty();
            }

            JsonNode licenses = root.path("Licenses").path("License");
            if (!licenses.isArray() || licenses.isEmpty()) {
                return Optional.empty();
            }

            // Find the exact callsign match (FCC search may return partial matches)
            for (JsonNode license : licenses) {
                String licCallsign = license.path("callsign").asText();
                if (!callsign.equalsIgnoreCase(licCallsign)) {
                    continue;
                }
                String status = license.path("statusDesc").asText();
                if (!"Active".equalsIgnoreCase(status)) {
                    log.debug("FCC license for {} is not active: {}", callsign, status);
                    continue;
                }

                CallsignInfo info = CallsignInfo.builder()
                        .callsign(licCallsign.toUpperCase())
                        .name(license.path("licName").asText(null))
                        .country("United States")
                        .lookupSource("FCC")
                        .cached(false)
                        .build();

                return Optional.of(info);
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error looking up callsign on FCC: {}", callsign, e);
            return Optional.empty();
        }
    }

    // ==================== HELPERS ====================

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Failed to parse XML response", e);
            return null;
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String text = nodes.item(0).getTextContent();
        return (text != null && !text.isBlank()) ? text.trim() : null;
    }

    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    private String buildAddress(String addr1, String addr2) {
        if (addr1 == null && addr2 == null) return null;
        if (addr1 == null) return addr2;
        if (addr2 == null) return addr1;
        return addr1 + ", " + addr2;
    }

    private void cacheCallsignInfo(String callsign, CallsignInfo info, String source) {
        CallsignCache cache = CallsignCache.builder()
                .callsign(callsign)
                .name(info.getName())
                .address(info.getAddress())
                .state(info.getState())
                .country(info.getCountry())
                .licenseClass(info.getLicenseClass())
                .gridSquare(info.getGridSquare())
                .lookupSource(source)
                .cachedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(cacheTtlSeconds))
                .build();

        cacheRepository.findByCallsign(callsign).ifPresent(existing ->
                cacheRepository.deleteById(existing.getId())
        );

        cacheRepository.save(cache);
        log.info("Cached callsign info for {} from {}", callsign, source);
    }

    private CallsignInfo fromCache(CallsignCache cache) {
        return CallsignInfo.builder()
                .callsign(cache.getCallsign())
                .name(cache.getName())
                .address(cache.getAddress())
                .state(cache.getState())
                .country(cache.getCountry())
                .licenseClass(cache.getLicenseClass())
                .gridSquare(cache.getGridSquare())
                .lookupSource(cache.getLookupSource())
                .cached(true)
                .build();
    }

    private boolean hasQrzCredentials() {
        return qrzUsername != null && !qrzUsername.isEmpty()
                && qrzPassword != null && !qrzPassword.isEmpty();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isUSCallsign(String callsign) {
        if (callsign == null || callsign.length() < 3) {
            return false;
        }
        char first = callsign.charAt(0);
        return first == 'K' || first == 'N' || first == 'W' ||
                (first == 'A' && callsign.length() > 1 && callsign.charAt(1) >= 'A' && callsign.charAt(1) <= 'L');
    }

    /**
     * Clean up expired cache entries
     */
    @Transactional
    public void cleanupExpiredCache() {
        cacheRepository.deleteExpired(LocalDateTime.now());
        log.info("Cleaned up expired callsign cache entries");
    }
}
