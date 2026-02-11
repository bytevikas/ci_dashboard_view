package com.cars24.rcview.service;

import com.cars24.rcview.entity.AppConfig;
import com.cars24.rcview.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String CONFIG_ID = "global";

    private final AppConfigRepository configRepository;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.cache.ttl-days:3}")
    private int defaultCacheTtlDays;

    @Value("${app.rate-limit.per-second:5}")
    private int defaultRateLimitPerSecond;

    @Value("${app.rate-limit.per-day-default:100}")
    private int defaultRateLimitPerDay;

    public AppConfig getConfig() {
        if (devMode) return defaultConfig();
        return configRepository.findById(CONFIG_ID)
                .orElseGet(this::defaultConfig);
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
        if (devMode) return config;
        return configRepository.save(config);
    }
}
