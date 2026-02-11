package com.cars24.rcview.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ConfigService configService;
    private final com.cars24.rcview.repository.AuditLogRepository auditLogRepository;

    @org.springframework.beans.factory.annotation.Value("${app.dev-mode:false}")
    private boolean devMode;

    private final Map<String, Bucket> perUserBuckets = new ConcurrentHashMap<>();

    public boolean allowRequest(String userId) {
        Bucket bucket = perUserBuckets.computeIfAbsent(userId, k -> createBucket());
        return bucket.tryConsume(1);
    }

    private Bucket createBucket() {
        int perSecond = Math.max(1, configService.getRateLimitPerSecond());
        Bandwidth limit = Bandwidth.classic(perSecond, Refill.greedy(perSecond, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /** Actions that count toward the daily search limit (every search attempt that consumes the limit). */
    private static final List<com.cars24.rcview.entity.AuditLog.AuditAction> DAILY_LIMIT_ACTIONS = List.of(
            com.cars24.rcview.entity.AuditLog.AuditAction.SEARCH,
            com.cars24.rcview.entity.AuditLog.AuditAction.CACHE_HIT,
            com.cars24.rcview.entity.AuditLog.AuditAction.API_CALL);

    public boolean withinDailyLimit(String userId) {
        if (devMode) return true;
        int limit = configService.getRateLimitPerDayDefault();
        var since = java.time.Instant.now().minus(java.time.Duration.ofDays(1));
        long count = auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(userId, DAILY_LIMIT_ACTIONS, since);
        return count < limit;
    }

    public long getRemainingDailyCount(String userId) {
        if (devMode) return 999L;
        int limit = configService.getRateLimitPerDayDefault();
        var since = java.time.Instant.now().minus(java.time.Duration.ofDays(1));
        long count = auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(userId, DAILY_LIMIT_ACTIONS, since);
        return Math.max(0, limit - count);
    }
}
