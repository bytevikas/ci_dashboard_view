package com.cars24.rcview.service;

import com.cars24.rcview.entity.AuditLog;
import com.cars24.rcview.entity.VehicleCache;
import com.cars24.rcview.repository.AuditLogRepository;
import com.cars24.rcview.repository.VehicleCacheRepository;
import com.cars24.rcview.security.CustomOAuth2User;
import com.cars24.rcview.dto.VehicleSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class VehicleSearchService {

    private final VehicleCacheRepository cacheRepository;
    private final AuditLogRepository auditLogRepository;
    private final VahanApiClient vahanApiClient;
    private final ConfigService configService;
    private final RateLimitService rateLimitService;

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
                        .registrationNumber(normalized)
                        .data(entry.data)
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
                        .fromCache(true)
                        .createdAt(now)
                        .build());
                return VehicleSearchResponse.builder()
                        .success(true)
                        .fromCache(true)
                        .registrationNumber(vc.getRegNoNormalized())
                        .data(vc.getResponseData())
                        .build();
            }
        }

        Optional<JsonNode> apiResponse = vahanApiClient.search(registrationNumber.trim());
        if (apiResponse.isEmpty()) {
            if (!devMode) {
                auditLogRepository.save(AuditLog.builder()
                        .userId(userId)
                        .userEmail(userEmail)
                        .action(AuditLog.AuditAction.SEARCH)
                        .registrationNumber(normalized)
                        .fromCache(false)
                        .details("NO_DATA")
                        .createdAt(now)
                        .build());
            }
            return VehicleSearchResponse.builder()
                    .success(false)
                    .fromCache(false)
                    .registrationNumber(normalized)
                    .errorMessage("No data found for this registration number")
                    .build();
        }

        JsonNode root = apiResponse.get();
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
                    .fromCache(false)
                    .createdAt(now)
                    .build());
        }

        return VehicleSearchResponse.builder()
                .success(true)
                .fromCache(false)
                .registrationNumber(normalized)
                .data(dataMap)
                .rawResponse(root)
                .build();
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
