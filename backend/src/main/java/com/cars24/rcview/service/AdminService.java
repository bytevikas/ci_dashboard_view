package com.cars24.rcview.service;

import com.cars24.rcview.entity.AppConfig;
import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.entity.AuditLog;
import com.cars24.rcview.repository.AppUserRepository;
import com.cars24.rcview.repository.AuditLogRepository;
import com.cars24.rcview.security.CustomOAuth2User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final AppUserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ConfigService configService;
    private final MongoTemplate mongoTemplate;
    private final InMemoryAuditStore inMemoryAuditStore;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.super-admin-email:vikas.kumar8@cars24.com}")
    private String superAdminEmail;

    public AdminService(AppUserRepository userRepository, AuditLogRepository auditLogRepository,
                        ConfigService configService, MongoTemplate mongoTemplate,
                        InMemoryAuditStore inMemoryAuditStore) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.configService = configService;
        this.mongoTemplate = mongoTemplate;
        this.inMemoryAuditStore = inMemoryAuditStore;
    }

    // ── Super-admin helpers ──────────────────────────────────────────────

    public boolean isSuperAdmin(String email) {
        return email != null && email.equalsIgnoreCase(superAdminEmail);
    }

    public boolean canModifyUser(String targetUserId) {
        AppUser target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return false;
        return !isSuperAdmin(target.getEmail());
    }

    // ── User management ──────────────────────────────────────────────────

    public List<AppUser> listUsers(String search) {
        List<AppUser> all;
        try {
            all = userRepository.findAll();
        } catch (Exception e) {
            if (devMode) {
                log.info("Dev mode: returning synthetic user list (MongoDB unavailable)");
                all = List.of(devUser());
            } else {
                throw e;
            }
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            return all.stream()
                    .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                            || (u.getName() != null && u.getName().toLowerCase().contains(q)))
                    .toList();
        }
        return all;
    }

    public AppUser addOrEnableUser(String email, String name, AppUser.Role role, boolean ssoEnabled) {
        if (isSuperAdmin(email)) {
            throw new IllegalArgumentException("Cannot modify super admin");
        }
        AppUser.Role safeRole = (role != null) ? role : AppUser.Role.USER;
        return userRepository.findByEmail(email)
                .map(u -> {
                    u.setSsoEnabled(ssoEnabled);
                    u.setName(name != null ? name : u.getName());
                    u.setRole(safeRole);
                    u.setUpdatedAt(Instant.now());
                    return userRepository.save(u);
                })
                .orElseGet(() -> {
                    AppUser newUser = AppUser.builder()
                            .email(email)
                            .name(name)
                            .role(safeRole)
                            .ssoEnabled(ssoEnabled)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    public void removeUser(String userId) {
        if (!canModifyUser(userId)) {
            throw new IllegalArgumentException("Cannot remove this user");
        }
        userRepository.deleteById(userId);
    }

    public AppUser setRole(String userId, AppUser.Role role) {
        if (!canModifyUser(userId)) {
            throw new IllegalArgumentException("Cannot change role for this user");
        }
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    // ── Config ───────────────────────────────────────────────────────────

    public AppConfig getConfig() {
        return configService.getConfig();
    }

    public AppConfig updateConfig(int cacheTtlDays, int rateLimitPerSecond, int rateLimitPerDayDefault) {
        String updatedBy = getCurrentUserEmail();
        return configService.updateConfig(cacheTtlDays, rateLimitPerSecond, rateLimitPerDayDefault, updatedBy);
    }

    // ── Audit logs (generic) ─────────────────────────────────────────────

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        try {
            return auditLogRepository.findAll(pageable);
        } catch (Exception e) {
            if (devMode) {
                log.info("Dev mode: returning in-memory audit logs (MongoDB unavailable)");
                return inMemorySearchLogsPage(pageable);
            }
            throw e;
        }
    }

    // ── Search activity: paginated logs ──────────────────────────────────

    public Page<AuditLog> getSearchLogs(Pageable pageable) {
        try {
            return auditLogRepository.findByAction(AuditLog.AuditAction.SEARCH, pageable);
        } catch (Exception e) {
            if (devMode) {
                log.info("Dev mode: returning in-memory search logs (MongoDB unavailable)");
                return inMemorySearchLogsPage(pageable);
            }
            throw e;
        }
    }

    // ── Search activity: aggregated stats ────────────────────────────────

    public Map<String, Object> getSearchStats() {
        try {
            return getSearchStatsFromMongo();
        } catch (Exception e) {
            if (devMode) {
                log.info("Dev mode: computing search stats from in-memory store (MongoDB unavailable)");
                return inMemoryAuditStore.getSearchStats();
            }
            throw e;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private Map<String, Object> getSearchStatsFromMongo() {
        Instant startOfToday = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();

        long totalSearches = auditLogRepository.countByAction(AuditLog.AuditAction.SEARCH);
        long todaySearches = auditLogRepository.countByActionAndCreatedAtAfter(
                AuditLog.AuditAction.SEARCH, startOfToday);

        long uniqueUsers = countDistinctField("userEmail");
        long uniqueRegNumbers = countDistinctField("registrationNumber");
        List<Map<String, Object>> topSearchers = getTopSearchers(10);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSearches", totalSearches);
        stats.put("todaySearches", todaySearches);
        stats.put("uniqueUsers", uniqueUsers);
        stats.put("uniqueRegNumbers", uniqueRegNumbers);
        stats.put("topSearchers", topSearchers);
        return stats;
    }

    private long countDistinctField(String field) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("action").is(AuditLog.AuditAction.SEARCH)),
                Aggregation.group(field),
                Aggregation.count().as("total")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "audit_logs", Map.class);
        Map uniqueResult = results.getUniqueMappedResult();
        if (uniqueResult != null && uniqueResult.get("total") instanceof Number n) {
            return n.longValue();
        }
        return 0;
    }

    private List<Map<String, Object>> getTopSearchers(int limit) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("action").is(AuditLog.AuditAction.SEARCH)),
                Aggregation.group("userEmail").count().as("count"),
                Aggregation.sort(org.springframework.data.domain.Sort.Direction.DESC, "count"),
                Aggregation.limit(limit),
                Aggregation.project("count").and("_id").as("email")
        );
        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "audit_logs", Map.class);
        List<Map<String, Object>> topSearchers = new ArrayList<>();
        for (Map row : results.getMappedResults()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("email", row.get("email"));
            entry.put("count", row.get("count"));
            topSearchers.add(entry);
        }
        return topSearchers;
    }

    private Page<AuditLog> inMemorySearchLogsPage(Pageable pageable) {
        List<AuditLog> page = inMemoryAuditStore.getSearchLogs(pageable.getPageNumber(), pageable.getPageSize());
        long total = inMemoryAuditStore.totalSearchCount();
        return new PageImpl<>(page, pageable, total);
    }

    private AppUser devUser() {
        return AppUser.builder()
                .id("dev")
                .email("dev@test.com")
                .name("Dev User")
                .role(AppUser.Role.ADMIN)
                .ssoEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User user) {
            return user.getAppUser().getEmail();
        }
        return null;
    }
}
