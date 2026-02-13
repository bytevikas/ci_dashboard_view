package com.cars24.rcview.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "app_config")
public class AppConfig {

    @Id
    private String id;

    /** Cache TTL in days. */
    private int cacheTtlDays;

    /** Max requests per second per user (burst). */
    private int rateLimitPerSecond;

    /** Default max requests per day per user. */
    private int rateLimitPerDayDefault;

    private Instant updatedAt;
    private String updatedBy;

    public AppConfig() {
    }

    public AppConfig(String id, int cacheTtlDays, int rateLimitPerSecond, int rateLimitPerDayDefault, Instant updatedAt, String updatedBy) {
        this.id = id;
        this.cacheTtlDays = cacheTtlDays;
        this.rateLimitPerSecond = rateLimitPerSecond;
        this.rateLimitPerDayDefault = rateLimitPerDayDefault;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getCacheTtlDays() { return cacheTtlDays; }
    public void setCacheTtlDays(int cacheTtlDays) { this.cacheTtlDays = cacheTtlDays; }
    public int getRateLimitPerSecond() { return rateLimitPerSecond; }
    public void setRateLimitPerSecond(int rateLimitPerSecond) { this.rateLimitPerSecond = rateLimitPerSecond; }
    public int getRateLimitPerDayDefault() { return rateLimitPerDayDefault; }
    public void setRateLimitPerDayDefault(int rateLimitPerDayDefault) { this.rateLimitPerDayDefault = rateLimitPerDayDefault; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public static final class Builder {
        private String id;
        private int cacheTtlDays;
        private int rateLimitPerSecond;
        private int rateLimitPerDayDefault;
        private Instant updatedAt;
        private String updatedBy;

        public Builder id(String id) { this.id = id; return this; }
        public Builder cacheTtlDays(int cacheTtlDays) { this.cacheTtlDays = cacheTtlDays; return this; }
        public Builder rateLimitPerSecond(int rateLimitPerSecond) { this.rateLimitPerSecond = rateLimitPerSecond; return this; }
        public Builder rateLimitPerDayDefault(int rateLimitPerDayDefault) { this.rateLimitPerDayDefault = rateLimitPerDayDefault; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder updatedBy(String updatedBy) { this.updatedBy = updatedBy; return this; }
        public AppConfig build() { return new AppConfig(id, cacheTtlDays, rateLimitPerSecond, rateLimitPerDayDefault, updatedAt, updatedBy); }
    }
}
