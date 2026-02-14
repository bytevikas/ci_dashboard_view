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

    public VehicleSearchService(VehicleCacheRepository cacheRepository, AuditLogRepository auditLogRepository, VahanApiClient vahanApiClient, ConfigService configService, RateLimitService rateLimitService) {
        this.cacheRepository = cacheRepository;
        this.auditLogRepository = auditLogRepository;
        this.vahanApiClient = vahanApiClient;
        this.configService = configService;
        this.rateLimitService = rateLimitService;
    }

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CachedEntry> devModeCache = new ConcurrentHashMap<>();

    private static class CachedEntry {
        Map<String, Object> data;
        Instant expiresAt;
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

        if (!rateLimitService.allowRequest(userId)) {
            if (!devMode) {
                auditLogRepository.save(AuditLog.builder()
                        .userId(userId)
                        .userEmail(userEmail)
                        .action(AuditLog.AuditAction.SEARCH)
                        .registrationNumber(normalizeRegNo(registrationNumber))
                        .details("RATE_LIMIT_PER_SECOND")
                        .createdAt(Instant.now())
                        .build());
            }
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Too many requests. Please slow down.")
                    .build();
        }

        if (!rateLimitService.searchCooldownPassed(userId)) {
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Please wait a moment before searching again.")
                    .build();
        }

        if (!rateLimitService.withinDailyLimit(userId)) {
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Daily search limit reached. Try again tomorrow.")
                    .build();
        }

        String normalized = normalizeRegNo(registrationNumber);
        if (normalized == null || normalized.isBlank()) {
            return VehicleSearchResponse.builder()
                    .success(false)
                    .errorMessage("Invalid registration number")
                    .build();
        }

        int ttlDays = configService.getCacheTtlDays();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttlDays, ChronoUnit.DAYS);

        if (devMode) {
            CachedEntry entry = devModeCache.get(normalized);
            if (entry != null && entry.expiresAt.isAfter(now)) {
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
                auditLogRepository.save(AuditLog.builder()
                        .userId(userId)
                        .userEmail(userEmail)
                        .action(AuditLog.AuditAction.CACHE_HIT)
                        .registrationNumber(normalized)
                        .createdAt(now)
                        .build());
                return VehicleSearchResponse.builder()
                        .success(true)
                        .fromCache(true)
                        .registrationNumber(maskRegNo(vc.getRegNoNormalized()))
                        .data(maskDataFields(vc.getResponseData()))
                        .build();
            }
        }

        VahanSearchResult apiResult = vahanApiClient.search(registrationNumber.trim());
        if (apiResult.getErrorMessage() != null) {
            return VehicleSearchResponse.builder()
                    .success(false)
                    .fromCache(false)
                    .registrationNumber(normalized)
                    .errorMessage(apiResult.getErrorMessage())
                    .build();
        }
        if (apiResult.getData().isEmpty()) {
            if (!devMode) {
                auditLogRepository.save(AuditLog.builder()
                        .userId(userId)
                        .userEmail(userEmail)
                        .action(AuditLog.AuditAction.SEARCH)
                        .registrationNumber(normalized)
                        .details("NO_DATA")
                        .createdAt(now)
                        .build());
            }
            return VehicleSearchResponse.builder()
                    .success(false)
                    .fromCache(false)
                    .registrationNumber(normalized)
                    .errorMessage("No data found for this registration number. The number may be invalid or not in the Vahan database.")
                    .build();
        }

        JsonNode root = apiResult.getData().get();
        // API returns data at root.data, not root.response.data
        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode() || !dataNode.isObject()) {
            dataNode = root.path("response").path("data");
        }
        Map<String, Object> dataMap = dataNode.isObject()
                ? objectMapper.convertValue(dataNode, Map.class)
                : new HashMap<>();

        if (devMode) {
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
            auditLogRepository.save(AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(AuditLog.AuditAction.API_CALL)
                    .registrationNumber(normalized)
                    .createdAt(now)
                    .build());
        }

        return VehicleSearchResponse.builder()
                .success(true)
                .fromCache(false)
                .registrationNumber(maskRegNo(normalized))
                .data(maskDataFields(dataMap))
                .build();
    }

    /** Keys in the data map that contain registration numbers and should be masked. */
    private static final Set<String> REG_NO_DATA_KEYS = Set.of("regNo", "vehicleNumber");

    /**
     * Masks a registration number, showing only the first 2 and last 2 characters.
     * e.g. "MH12AB1234" → "MH******34"
     */
    static String maskRegNo(String regNo) {
        if (regNo == null) return null;
        String trimmed = regNo.trim();
        if (trimmed.length() <= 4) return trimmed; // too short to mask meaningfully
        return trimmed.substring(0, 2)
                + "*".repeat(trimmed.length() - 4)
                + trimmed.substring(trimmed.length() - 2);
    }

    /** Masks registration-number fields inside the data map. Returns a new map. */
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

        // Audit the unmask action (skip DB write in dev mode)
        if (!devMode) {
            try {
                String userId = getCurrentUserId();
                String userEmail = getCurrentUserEmail();
                auditLogRepository.save(AuditLog.builder()
                        .userId(userId)
                        .userEmail(userEmail)
                        .action(AuditLog.AuditAction.UNMASK_REG_NUMBER)
                        .registrationNumber(normalized)
                        .details("User acknowledged sensitive-data warning and unmasked registration number")
                        .createdAt(Instant.now())
                        .build());
            } catch (Exception e) {
                // Don't let audit failure block the unmask response
                log.warn("Failed to write unmask audit log for {}: {}", normalized, e.getMessage());
            }
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
