package com.cars24.rcview.service;

import com.cars24.rcview.entity.AuditLog.AuditAction;
import com.cars24.rcview.repository.AuditLogRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final ConfigService configService;
    private final AuditLogRepository auditLogRepository;
    private final InMemoryAuditStore inMemoryAuditStore;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.dev-use-db:false}")
    private boolean devUseDb;

    private final Map<String, Bucket> perUserBuckets = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastSearchTime = new ConcurrentHashMap<>();

    /** Minimum interval between search requests per user (prevents abuse from DevTools / curl). */
    private static final long SEARCH_COOLDOWN_MS = 2000;

    private static final List<AuditAction> DAILY_LIMIT_ACTIONS = List.of(
            AuditAction.SEARCH, AuditAction.CACHE_HIT, AuditAction.API_CALL);

    private static final Set<AuditAction> DAILY_LIMIT_ACTIONS_SET = Set.of(
            AuditAction.SEARCH, AuditAction.CACHE_HIT, AuditAction.API_CALL);

    public RateLimitService(ConfigService configService, AuditLogRepository auditLogRepository,
                            InMemoryAuditStore inMemoryAuditStore) {
        this.configService = configService;
        this.auditLogRepository = auditLogRepository;
        this.inMemoryAuditStore = inMemoryAuditStore;
    }

    public boolean allowRequest(String userId) {
        Bucket bucket = perUserBuckets.computeIfAbsent(userId, k -> createBucket());
        return bucket.tryConsume(1);
    }

    public boolean withinDailyLimit(String userId) {
        int limit = configService.getRateLimitPerDayDefault();
        long count = getDailyActionCount(userId);
        return count < limit;
    }

    public long getRemainingDailyCount(String userId) {
        int limit = configService.getRateLimitPerDayDefault();
        long count = getDailyActionCount(userId);
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

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Count daily actions for the user. In dev mode uses the in-memory audit
     * store; in production queries MongoDB (with graceful fallback).
     */
    private long getDailyActionCount(String userId) {
        Instant since = Instant.now().minus(Duration.ofDays(1));
        // Pure in-memory mode: dev without DB
        if (devMode && !devUseDb) {
            return inMemoryAuditStore.countByUserAndActionsSince(userId, DAILY_LIMIT_ACTIONS_SET, since);
        }
        // MongoDB mode (production or dev+DB) with graceful fallback
        try {
            return auditLogRepository.countByUserIdAndActionInAndCreatedAtAfter(userId, DAILY_LIMIT_ACTIONS, since);
        } catch (Exception e) {
            log.warn("MongoDB unavailable for daily count — using in-memory store: {}", e.getMessage());
            return inMemoryAuditStore.countByUserAndActionsSince(userId, DAILY_LIMIT_ACTIONS_SET, since);
        }
    }

    private Bucket createBucket() {
        int perSecond = Math.max(1, configService.getRateLimitPerSecond());
        Bandwidth limit = Bandwidth.classic(perSecond, Refill.greedy(perSecond, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
