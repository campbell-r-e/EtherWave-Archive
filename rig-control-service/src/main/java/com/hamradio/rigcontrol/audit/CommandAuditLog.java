package com.hamradio.rigcontrol.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory circular buffer of rig command audit entries.
 *
 * Records who sent each command, the outcome, and any parameters.
 * Exposed via GET /api/rig/audit for inspection.
 *
 * Max entries is configurable via rig.audit.max-entries (default: 1000).
 */
@Component
@Slf4j
public class CommandAuditLog {

    @Value("${rig.audit.max-entries:1000}")
    private int maxEntries;

    private final Deque<AuditEntry> entries = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Record a command execution in the audit log.
     *
     * @param clientId the identifier of the client that sent the command
     * @param command  the command name (setFrequency, setMode, setPTT, etc.)
     * @param detail   optional detail string (e.g., "hz=14250000")
     * @param success  whether the command succeeded
     */
    public void record(String clientId, String command, String detail, boolean success) {
        lock.lock();
        try {
            while (entries.size() >= maxEntries) {
                entries.removeFirst();
            }
            entries.addLast(new AuditEntry(LocalDateTime.now(), clientId, command, detail, success));
        } finally {
            lock.unlock();
        }
        log.debug("Audit: client={} command={} detail={} success={}", clientId, command, detail, success);
    }

    /**
     * Return the most recent entries, up to {@code count}.
     *
     * @param count maximum number of entries to return (0 = all)
     */
    public List<AuditEntry> getRecent(int count) {
        lock.lock();
        try {
            List<AuditEntry> list = new ArrayList<>(entries);
            if (count > 0 && list.size() > count) {
                return list.subList(list.size() - count, list.size());
            }
            return list;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return the total number of entries recorded so far (including evicted entries).
     */
    public int size() {
        lock.lock();
        try {
            return entries.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Immutable audit entry record.
     */
    public record AuditEntry(
            LocalDateTime timestamp,
            String clientId,
            String command,
            String detail,
            boolean success
    ) {}
}
