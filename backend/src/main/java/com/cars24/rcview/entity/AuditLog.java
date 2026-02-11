package com.cars24.rcview.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
