package com.cars24.rcview.service;

import com.cars24.rcview.entity.AuditLog.AuditAction;
import com.cars24.rcview.repository.AuditLogRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final ConfigService configService;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    private final Map<String, Bucket> perUserBuckets = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSearchTime = new ConcurrentHashMap<>();

    /** Minimum interval between search requests per user (prevents abuse from DevTools / curl). */
    private static final long SEARCH_COOLDOWN_MS = 2000;

    private static final List<AuditAction> DAILY_LIMIT_ACTIONS = List.of(
            AuditAction.SEARCH, AuditAction.CACHE_HIT, AuditAction.API_CALL);

    public RateLimitService(ConfigService configService, AuditLogRepository auditLogRepository) {
        this.configService = configService;
        this.auditLogRepository = auditLogRepository;
    }

    public boolean allowRequest(String userId) {
        Bucket bucket = perUserBuckets.computeIfAbsent(userId, k -> createBucket());
        return bucket.tryConsume(1);
    }

    public boolean withinDailyLimit(String userId) {
        if (devMode) return true;
        int limit = configService.getRateLimitPerDayDefault();
        Instant since = Instant.now().minus(Duration.ofDays(1));
        long count = auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(userId, DAILY_LIMIT_ACTIONS, since);
        return count < limit;
    }

    public long getRemainingDailyCount(String userId) {
        if (devMode) return 999L;
        int limit = configService.getRateLimitPerDayDefault();
        Instant since = Instant.now().minus(Duration.ofDays(1));
        long count = auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(userId, DAILY_LIMIT_ACTIONS, since);
        return Math.max(0, limit - count);
    }

    /**
     * Returns true if enough time has passed since this user's last search.
     * Enforces a mandatory cooldown even for users calling the API directly.
     */
    public boolean searchCooldownPassed(String userId) {
        Instant now = Instant.now();
        Instant last = lastSearchTime.get(userId);
        if (last != null && Duration.between(last, now).toMillis() < SEARCH_COOLDOWN_MS) {
            return false;
        }
        lastSearchTime.put(userId, now);
        return true;
    }

    private Bucket createBucket() {
        int perSecond = Math.max(1, configService.getRateLimitPerSecond());
        Bandwidth limit = Bandwidth.classic(perSecond, Refill.greedy(perSecond, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
