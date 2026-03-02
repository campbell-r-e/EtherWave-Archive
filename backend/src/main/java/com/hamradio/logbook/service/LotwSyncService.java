package com.hamradio.logbook.service;

import com.hamradio.logbook.entity.Log;
import com.hamradio.logbook.entity.QSO;
import com.hamradio.logbook.entity.User;
import com.hamradio.logbook.repository.LogRepository;
import com.hamradio.logbook.repository.QSORepository;
import com.hamradio.logbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syncs LoTW (Logbook of the World) QSL confirmations into a log.
 *
 * Downloads the user's LoTW ADIF report for confirmed QSOs,
 * then matches against existing QSOs in the log and updates lotwRcvd = 'Y'.
 *
 * Credentials are used only for this request and never stored.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LotwSyncService {

    private static final String LOTW_URL = "https://lotw.arrl.org/lotwuser/lotwreport.adi";

    private final QSORepository qsoRepository;
    private final LogRepository logRepository;
    private final UserRepository userRepository;
    private final LogService logService;
    private final RestTemplate restTemplate = new RestTemplate();

    public record SyncResult(
            int downloaded,   // number of confirmed QSOs from LoTW
            int matched,      // number matched to our log's QSOs
            int updated,      // number where lotwRcvd was newly set to 'Y'
            String message
    ) {}

    /**
     * Download LoTW confirmations and update matching QSOs in the log.
     *
     * @param logId       log to sync into
     * @param username    authenticated user (must have access to the log)
     * @param lotwCall    LoTW login callsign
     * @param lotwPass    LoTW password (not stored after this method returns)
     * @param since       optional — only fetch QSLs confirmed after this date
     */
    @Transactional
    public SyncResult sync(Long logId, String username, String lotwCall, String lotwPass,
                           LocalDate since) {
        // Permission check
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Log logEntity = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + logId));
        if (!logService.hasAccess(logEntity, user)) {
            throw new SecurityException("User does not have access to this log");
        }

        // Build LoTW URL
        String url = buildLotwUrl(lotwCall, lotwPass, since);

        // Fetch ADIF from LoTW
        String adif;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "EtherWaveArchive/1.0");
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return new SyncResult(0, 0, 0, "LoTW returned error: " + response.getStatusCode());
            }
            adif = response.getBody();
        } catch (Exception e) {
            log.error("LoTW fetch failed: {}", e.getMessage());
            return new SyncResult(0, 0, 0, "Failed to contact LoTW: " + e.getMessage());
        }

        // Verify it's actually an ADIF response (LoTW returns HTML on bad credentials)
        if (!adif.contains("<EOH>") && !adif.contains("<eoh>")) {
            return new SyncResult(0, 0, 0,
                    "Invalid LoTW response — check callsign and password");
        }

        // Parse confirmed QSOs from ADIF
        List<LotwQso> lotwQsos = parseAdif(adif);
        log.info("Downloaded {} confirmed QSOs from LoTW for {}", lotwQsos.size(), lotwCall);

        // Load all QSOs for this log
        List<QSO> logQsos = qsoRepository.findAllByLogId(logId);

        int matched = 0;
        int updated = 0;

        for (LotwQso lqso : lotwQsos) {
            for (QSO qso : logQsos) {
                if (isMatch(qso, lqso)) {
                    matched++;
                    if (!"Y".equalsIgnoreCase(qso.getLotwRcvd())) {
                        qso.setLotwRcvd("Y");
                        qsoRepository.save(qso);
                        updated++;
                    }
                    break;
                }
            }
        }

        return new SyncResult(lotwQsos.size(), matched, updated,
                String.format("Downloaded %d confirmations; matched %d; updated %d",
                        lotwQsos.size(), matched, updated));
    }

    // -----------------------------------------------------------------------

    private String buildLotwUrl(String call, String pass, LocalDate since) {
        StringBuilder sb = new StringBuilder(LOTW_URL);
        sb.append("?login=").append(URLEncoder.encode(call, StandardCharsets.UTF_8));
        sb.append("&password=").append(URLEncoder.encode(pass, StandardCharsets.UTF_8));
        sb.append("&qso_query=1");
        sb.append("&qso_qsl=yes");
        if (since != null) {
            sb.append("&qso_qslsince=")
              .append(since.format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        return sb.toString();
    }

    // ADIF field extraction — e.g. <CALL:4>W1AW or <CALL:4>w1aw
    private static final Pattern FIELD_PATTERN =
            Pattern.compile("<([A-Z_]+):(\\d+)(?::\\S*)?>([^<]*)", Pattern.CASE_INSENSITIVE);

    private record LotwQso(String callsign, LocalDate date, String band, String mode) {}

    private List<LotwQso> parseAdif(String adif) {
        List<LotwQso> result = new ArrayList<>();

        // Split on <EOR> (end of record)
        String[] records = adif.split("(?i)<EOR>");
        for (String record : records) {
            String callsign = extractField(record, "CALL");
            String dateStr  = extractField(record, "QSO_DATE");
            String band     = extractField(record, "BAND");
            String mode     = extractField(record, "MODE");

            if (callsign.isBlank() || dateStr.isBlank()) continue;

            try {
                LocalDate date = parseAdifDate(dateStr);
                result.add(new LotwQso(callsign.toUpperCase(), date,
                        band.toLowerCase(), mode.toUpperCase()));
            } catch (Exception e) {
                // Skip malformed records
            }
        }
        return result;
    }

    private String extractField(String record, String fieldName) {
        Pattern p = Pattern.compile(
                "<" + fieldName + ":(\\d+)(?::\\S*)?>([^<]*)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(record);
        if (m.find()) {
            return m.group(2).trim();
        }
        return "";
    }

    /** Parse ADIF date YYYYMMDD → LocalDate */
    private LocalDate parseAdifDate(String adifDate) {
        String d = adifDate.trim();
        if (d.length() >= 8) {
            int year  = Integer.parseInt(d.substring(0, 4));
            int month = Integer.parseInt(d.substring(4, 6));
            int day   = Integer.parseInt(d.substring(6, 8));
            return LocalDate.of(year, month, day);
        }
        throw new IllegalArgumentException("Bad ADIF date: " + adifDate);
    }

    /** Match a log QSO against a LoTW confirmed QSO */
    private boolean isMatch(QSO qso, LotwQso lqso) {
        // Callsign must match (case insensitive)
        if (qso.getCallsign() == null) return false;
        if (!qso.getCallsign().equalsIgnoreCase(lqso.callsign())) return false;

        // Date must match
        if (qso.getQsoDate() == null || !qso.getQsoDate().equals(lqso.date())) return false;

        // Band must match if both present (LoTW always has band)
        if (lqso.band() != null && !lqso.band().isBlank() && qso.getBand() != null) {
            if (!normalizeBand(qso.getBand()).equalsIgnoreCase(lqso.band())) return false;
        }

        return true;
    }

    /** Normalize band string: "20M" → "20m", "20 m" → "20m" */
    private String normalizeBand(String band) {
        return band.trim().toLowerCase().replace(" ", "");
    }
}
