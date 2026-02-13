package com.cars24.rcview.service;

import com.cars24.rcview.entity.AppConfig;
import com.cars24.rcview.repository.AppConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final String CONFIG_ID = "global";

    private final AppConfigRepository configRepository;

    /** In-memory cache — avoids a MongoDB round-trip on every request. */
    private volatile AppConfig cachedConfig;

    /** Set after the first DB failure so subsequent calls use defaults instantly. */
    private volatile boolean dbUnavailable;

    public ConfigService(AppConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Value("${app.cache.ttl-days:3}")
    private int defaultCacheTtlDays;

    @Value("${app.rate-limit.per-second:5}")
    private int defaultRateLimitPerSecond;

    @Value("${app.rate-limit.per-day-default:100}")
    private int defaultRateLimitPerDay;

    public AppConfig getConfig() {
        AppConfig c = cachedConfig;
        if (c != null) return c;
        synchronized (this) {
            c = cachedConfig;
            if (c != null) return c;
            if (dbUnavailable) {
                c = defaultConfig();
                cachedConfig = c;
                return c;
            }
            try {
                c = configRepository.findById(CONFIG_ID).orElseGet(this::defaultConfig);
            } catch (Exception e) {
                log.warn("MongoDB unavailable for config — using defaults: {}", e.getMessage());
                dbUnavailable = true;
                c = defaultConfig();
            }
            cachedConfig = c;
            return c;
        }
    }

    private AppConfig defaultConfig() {
        return AppConfig.builder()
                .id(CONFIG_ID)
                .cacheTtlDays(defaultCacheTtlDays)
                .rateLimitPerSecond(defaultRateLimitPerSecond)
                .rateLimitPerDayDefault(defaultRateLimitPerDay)
                .updatedAt(Instant.now())
                .build();
    }

    public int getCacheTtlDays() {
        return getConfig().getCacheTtlDays();
    }

    public int getRateLimitPerSecond() {
        return getConfig().getRateLimitPerSecond();
    }

    public int getRateLimitPerDayDefault() {
        return getConfig().getRateLimitPerDayDefault();
    }

    public AppConfig updateConfig(int cacheTtlDays, int rateLimitPerSecond, int rateLimitPerDayDefault, String updatedBy) {
        AppConfig config = getConfig();
        config.setCacheTtlDays(cacheTtlDays);
        config.setRateLimitPerSecond(rateLimitPerSecond);
        config.setRateLimitPerDayDefault(rateLimitPerDayDefault);
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(updatedBy);
        try {
            AppConfig saved = configRepository.save(config);
            cachedConfig = saved;
            dbUnavailable = false;
            return saved;
        } catch (Exception e) {
            log.warn("MongoDB unavailable – config saved in-memory only: {}", e.getMessage());
            dbUnavailable = true;
            cachedConfig = config;
            return config;
        }
    }
}
