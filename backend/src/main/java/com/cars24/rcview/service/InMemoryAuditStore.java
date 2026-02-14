package com.cars24.rcview.service;

import com.cars24.rcview.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * In-memory store for audit log entries — used in dev mode so the admin
 * dashboard works without MongoDB. In production the data comes from the
 * real audit_logs collection; this store is only populated as a session-level
 * fallback.
 */
@Component
public class InMemoryAuditStore {

    private final List<AuditLog> logs = new CopyOnWriteArrayList<>();

    /** Append a log entry. Thread-safe. */
    public void save(AuditLog entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString());
        }
        logs.add(entry);
    }

    /** All SEARCH-action entries, newest first. */
    public List<AuditLog> getSearchLogs(int page, int size) {
        List<AuditLog> searchLogs = logs.stream()
                .filter(l -> l.getAction() == AuditLog.AuditAction.SEARCH)
                .sorted(Comparator.comparing(AuditLog::getCreatedAt).reversed())
                .collect(Collectors.toList());
        int from = page * size;
        if (from >= searchLogs.size()) return Collections.emptyList();
        int to = Math.min(from + size, searchLogs.size());
        return searchLogs.subList(from, to);
    }

    public long totalSearchCount() {
        return logs.stream().filter(l -> l.getAction() == AuditLog.AuditAction.SEARCH).count();
    }

    public boolean hasMoreSearchLogs(int page, int size) {
        long total = totalSearchCount();
        return (long) (page + 1) * size < total;
    }

    /** Count actions by a specific user since a given time. Used for rate-limit counting in dev mode. */
    public long countByUserAndActionsSince(String userId, Set<AuditLog.AuditAction> actions, Instant since) {
        return logs.stream()
                .filter(l -> actions.contains(l.getAction()))
                .filter(l -> userId.equals(l.getUserId()))
                .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(since))
                .count();
    }

    /** Compute aggregated search stats from in-memory entries. */
    public Map<String, Object> getSearchStats() {
        List<AuditLog> searchLogs = logs.stream()
                .filter(l -> l.getAction() == AuditLog.AuditAction.SEARCH)
                .collect(Collectors.toList());

        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        long totalSearches = searchLogs.size();
        long todaySearches = searchLogs.stream()
                .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(startOfToday))
                .count();
        long uniqueUsers = searchLogs.stream()
                .map(AuditLog::getUserEmail)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long uniqueRegNumbers = searchLogs.stream()
                .map(AuditLog::getRegistrationNumber)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // Top searchers
        List<Map<String, Object>> topSearchers = searchLogs.stream()
                .filter(l -> l.getUserEmail() != null)
                .collect(Collectors.groupingBy(AuditLog::getUserEmail, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("email", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSearches", totalSearches);
        stats.put("todaySearches", todaySearches);
        stats.put("uniqueUsers", uniqueUsers);
        stats.put("uniqueRegNumbers", uniqueRegNumbers);
        stats.put("topSearchers", topSearchers);
        return stats;
    }
}
