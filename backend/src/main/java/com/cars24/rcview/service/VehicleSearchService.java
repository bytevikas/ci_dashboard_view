package com.cars24.rcview.service;

import com.cars24.rcview.dto.VehicleSearchResponse;
import com.cars24.rcview.entity.AuditLog;
import com.cars24.rcview.entity.VehicleCache;
import com.cars24.rcview.repository.AuditLogRepository;
import com.cars24.rcview.repository.VehicleCacheRepository;
import com.cars24.rcview.security.CustomOAuth2User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VehicleSearchService {

    private static final Logger log = LoggerFactory.getLogger(VehicleSearchService.class);

    private final VehicleCacheRepository cacheRepository;
    private final AuditLogRepository auditLogRepository;
    private final VahanApiClient vahanApiClient;
    private final ConfigService configService;
    private final RateLimitService rateLimitService;
    private final InMemoryAuditStore inMemoryAuditStore;

    public VehicleSearchService(VehicleCacheRepository cacheRepository, AuditLogRepository auditLogRepository,
                                VahanApiClient vahanApiClient, ConfigService configService,
                                RateLimitService rateLimitService, InMemoryAuditStore inMemoryAuditStore) {
        this.cacheRepository = cacheRepository;
        this.auditLogRepository = auditLogRepository;
        this.vahanApiClient = vahanApiClient;
        this.configService = configService;
        this.rateLimitService = rateLimitService;
        this.inMemoryAuditStore = inMemoryAuditStore;
    }

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.dev-use-db:false}")
    private boolean devUseDb;

    /** When true, skip MongoDB and use in-memory maps instead. */
    private boolean useInMemory() { return devMode && !devUseDb; }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CachedEntry> devModeCache = new ConcurrentHashMap<>();

    private static class CachedEntry {
        Map<String, Object> data;
        Instant expiresAt;
    }

    // ── Canonical search audit logger ────────────────────────────────────
    // Called on EVERY code path so the admin dashboard has full visibility.
    // Always writes to in-memory store (for dev-mode admin dashboard).
    // Also attempts MongoDB write; failures are silently logged.
    private void logSearch(String userId, String userEmail, String regNo, String outcome, boolean fromCache) {
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .userEmail(userEmail)
                .action(AuditLog.AuditAction.SEARCH)
                .registrationNumber(regNo)
                .details(outcome)
                .fromCache(fromCache)
                .createdAt(Instant.now())
                .build();

        // Always store in memory so admin dashboard works even without MongoDB
        inMemoryAuditStore.save(entry);

        // Also persist to MongoDB (best-effort)
        try {
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist search log for {}: {}", regNo, e.getMessage());
        }
    }

    public VehicleSearchResponse search(String registrationNumber) {
        String userId = getCurrentUserId();
        String userEmail = getCurrentUserEmail();
        if (userId == null) {
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Unauthorized")
                    .build();
        }

        String normalized = normalizeRegNo(registrationNumber);

        if (!rateLimitService.allowRequest(userId)) {
            logSearch(userId, userEmail, normalized, "RATE_LIMITED", false);
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Too many requests. Please slow down.")
                    .build();
        }

        if (!rateLimitService.searchCooldownPassed(userId)) {
            logSearch(userId, userEmail, normalized, "COOLDOWN", false);
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Please wait a moment before searching again.")
                    .build();
        }

        if (!rateLimitService.withinDailyLimit(userId)) {
            logSearch(userId, userEmail, normalized, "DAILY_LIMIT_REACHED", false);
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Daily search limit reached. Try again tomorrow.")
                    .build();
        }

        if (normalized == null || normalized.isBlank()) {
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Invalid registration number")
                    .build();
        }

        int ttlDays = configService.getCacheTtlDays();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttlDays, ChronoUnit.DAYS);

        // ── Cache lookup ─────────────────────────────────────────────────
        if (useInMemory()) {
            CachedEntry entry = devModeCache.get(normalized);
            if (entry != null && entry.expiresAt.isAfter(now)) {
                logSearch(userId, userEmail, normalized, "CACHE_HIT", true);
                return VehicleSearchResponse.builder()
                        .success(true)
                        .fromCache(true)
                        .registrationNumber(maskRegNo(normalized))
                        .data(maskDataFields(entry.data))
                        .build();
            }
        } else {
            var cached = cacheRepository.findByRegNoNormalizedAndExpiresAtAfter(normalized, now);
            if (cached.isPresent()) {
                VehicleCache vc = cached.get();
                // Keep existing CACHE_HIT action log for rate-limit counting
                try {
                    auditLogRepository.save(AuditLog.builder()
                            .userId(userId).userEmail(userEmail)
                            .action(AuditLog.AuditAction.CACHE_HIT)
                            .registrationNumber(normalized)
                            .createdAt(now).build());
                } catch (Exception e) {
                    log.warn("Failed to write CACHE_HIT audit: {}", e.getMessage());
                }
                logSearch(userId, userEmail, normalized, "CACHE_HIT", true);
                return VehicleSearchResponse.builder()
                        .success(true)
                        .fromCache(true)
                        .registrationNumber(maskRegNo(vc.getRegNoNormalized()))
                        .data(maskDataFields(vc.getResponseData()))
                        .build();
            }
        }

        // ── External API call ────────────────────────────────────────────
        VahanSearchResult apiResult = vahanApiClient.search(registrationNumber.trim());
        if (apiResult.getErrorMessage() != null) {
            logSearch(userId, userEmail, normalized, "API_ERROR", false);
            return VehicleSearchResponse.builder()
                    .success(false)
                    .fromCache(false)
                    .registrationNumber(normalized)
                    .errorMessage(apiResult.getErrorMessage())
                    .build();
        }
        if (apiResult.getData().isEmpty()) {
            logSearch(userId, userEmail, normalized, "NO_DATA", false);
            return VehicleSearchResponse.builder()
                    .success(false)
                    .fromCache(false)
                    .registrationNumber(normalized)
                    .errorMessage("No data found for this registration number. The number may be invalid or not in the Vahan database.")
                    .build();
        }

        JsonNode root = apiResult.getData().get();
        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode() || !dataNode.isObject()) {
            dataNode = root.path("response").path("data");
        }
        Map<String, Object> dataMap = dataNode.isObject()
                ? objectMapper.convertValue(dataNode, Map.class)
                : new HashMap<>();

        // ── Persist to cache ─────────────────────────────────────────────
        if (useInMemory()) {
            CachedEntry entry = new CachedEntry();
            entry.data = dataMap;
            entry.expiresAt = expiresAt;
            devModeCache.put(normalized, entry);
        } else {
            VehicleCache toSave = VehicleCache.builder()
                    .regNoNormalized(normalized)
                    .responseData(dataMap)
                    .cachedAt(now)
                    .expiresAt(expiresAt)
                    .build();
            cacheRepository.save(toSave);
            // Keep existing API_CALL action log for rate-limit counting
            try {
                auditLogRepository.save(AuditLog.builder()
                        .userId(userId).userEmail(userEmail)
                        .action(AuditLog.AuditAction.API_CALL)
                        .registrationNumber(normalized)
                        .createdAt(now).build());
            } catch (Exception e) {
                log.warn("Failed to write API_CALL audit: {}", e.getMessage());
            }
        }

        logSearch(userId, userEmail, normalized, "SUCCESS", false);

        return VehicleSearchResponse.builder()
                .success(true)
                .fromCache(false)
                .registrationNumber(maskRegNo(normalized))
                .data(maskDataFields(dataMap))
                .build();
    }

    // ── Masking helpers ──────────────────────────────────────────────────

    private static final Set<String> REG_NO_DATA_KEYS = Set.of("regNo", "vehicleNumber");

    static String maskRegNo(String regNo) {
        if (regNo == null) return null;
        String trimmed = regNo.trim();
        if (trimmed.length() <= 4) return trimmed;
        return trimmed.substring(0, 2)
                + "*".repeat(trimmed.length() - 4)
                + trimmed.substring(trimmed.length() - 2);
    }

    private Map<String, Object> maskDataFields(Map<String, Object> data) {
        if (data == null) return null;
        Map<String, Object> masked = new HashMap<>(data);
        for (String key : REG_NO_DATA_KEYS) {
            Object val = masked.get(key);
            if (val instanceof String s && !s.isBlank()) {
                masked.put(key, maskRegNo(s));
            }
        }
        return masked;
    }

    /**
     * Returns the full (unmasked) registration number for the given normalized reg-no.
     * Writes an audit log entry (skipped in dev mode). Never throws — returns null on failure.
     */
    public String unmask(String registrationNumber) {
        String normalized = normalizeRegNo(registrationNumber);
        if (normalized == null || normalized.isBlank()) return null;

        String userId = getCurrentUserId();
        String userEmail = getCurrentUserEmail();
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .userEmail(userEmail)
                .action(AuditLog.AuditAction.UNMASK_REG_NUMBER)
                .registrationNumber(normalized)
                .details("User acknowledged sensitive-data warning and unmasked registration number")
                .createdAt(Instant.now())
                .build();

        // Always store in memory
        inMemoryAuditStore.save(entry);

        // Persist to MongoDB (best-effort)
        try {
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write unmask audit log for {}: {}", normalized, e.getMessage());
        }

        return normalized;
    }

    private String normalizeRegNo(String regNo) {
        if (regNo == null) return null;
        return regNo.trim().toUpperCase().replaceAll("\\s+", "");
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User user) {
            return user.getAppUser().getId();
        }
        return null;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User user) {
            return user.getAppUser().getEmail();
        }
        return null;
    }
}
