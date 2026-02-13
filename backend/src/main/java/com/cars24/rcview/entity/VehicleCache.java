package com.cars24.rcview.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "vehicle_cache")
public class VehicleCache {

    @Id
    private String id;

    @Indexed
    private String regNoNormalized;

    private Map<String, Object> responseData;

    @Indexed
    private Instant cachedAt;

    private Instant expiresAt;

    public VehicleCache() {
    }

    public VehicleCache(String id, String regNoNormalized, Map<String, Object> responseData, Instant cachedAt, Instant expiresAt) {
        this.id = id;
        this.regNoNormalized = regNoNormalized;
        this.responseData = responseData;
        this.cachedAt = cachedAt;
        this.expiresAt = expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRegNoNormalized() { return regNoNormalized; }
    public void setRegNoNormalized(String regNoNormalized) { this.regNoNormalized = regNoNormalized; }
    public Map<String, Object> getResponseData() { return responseData; }
    public void setResponseData(Map<String, Object> responseData) { this.responseData = responseData; }
    public Instant getCachedAt() { return cachedAt; }
    public void setCachedAt(Instant cachedAt) { this.cachedAt = cachedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public static final class Builder {
        private String id;
        private String regNoNormalized;
        private Map<String, Object> responseData;
        private Instant cachedAt;
        private Instant expiresAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder regNoNormalized(String regNoNormalized) { this.regNoNormalized = regNoNormalized; return this; }
        public Builder responseData(Map<String, Object> responseData) { this.responseData = responseData; return this; }
        public Builder cachedAt(Instant cachedAt) { this.cachedAt = cachedAt; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public VehicleCache build() { return new VehicleCache(id, regNoNormalized, responseData, cachedAt, expiresAt); }
    }
}
