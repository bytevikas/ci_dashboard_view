package com.cars24.rcview.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public boolean withinDailyLimit(String userId) {
        if (devMode) return true;
        int limit = configService.getRateLimitPerDayDefault();
        var since = java.time.Instant.now().minus(java.time.Duration.ofDays(1));
        long count = auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                userId, com.cars24.rcview.entity.AuditLog.AuditAction.SEARCH, since);
        return count < limit;
    }

    public long getRemainingDailyCount(String userId) {
        if (devMode) return 999L;
        int limit = configService.getRateLimitPerDayDefault();
        var since = java.time.Instant.now().minus(java.time.Duration.ofDays(1));
        long count = auditLogRepository.countByUserIdAndActionAndCreatedAtAfter(
                userId, com.cars24.rcview.entity.AuditLog.AuditAction.SEARCH, since);
        return Math.max(0, limit - count);
    }
}
