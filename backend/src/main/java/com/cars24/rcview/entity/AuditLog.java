package com.cars24.rcview.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String userId;
    private String userEmail;

    @Indexed
    private AuditAction action;

    private String registrationNumber;
    private boolean fromCache;
    private String details;

    private Map<String, Object> metadata;

    @Indexed
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(String id, String userId, String userEmail, AuditAction action, String registrationNumber, boolean fromCache, String details, Map<String, Object> metadata, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.action = action;
        this.registrationNumber = registrationNumber;
        this.fromCache = fromCache;
        this.details = details;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum AuditAction {
        SEARCH,
        API_CALL,
        CACHE_HIT,
        USER_LOGIN,
        USER_ADDED,
        USER_REMOVED,
        USER_ROLE_CHANGED,
        CONFIG_UPDATED
    }

    public static final class Builder {
        private String id;
        private String userId;
        private String userEmail;
        private AuditAction action;
        private String registrationNumber;
        private boolean fromCache;
        private String details;
        private Map<String, Object> metadata;
        private Instant createdAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder userEmail(String userEmail) { this.userEmail = userEmail; return this; }
        public Builder action(AuditAction action) { this.action = action; return this; }
        public Builder registrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; return this; }
        public Builder fromCache(boolean fromCache) { this.fromCache = fromCache; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public AuditLog build() { return new AuditLog(id, userId, userEmail, action, registrationNumber, fromCache, details, metadata, createdAt); }
    }
}
