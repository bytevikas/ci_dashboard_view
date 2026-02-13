package com.cars24.rcview.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class AppUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String pictureUrl;

    @Indexed
    private Role role;

    private boolean ssoEnabled;

    private Instant createdAt;
    private Instant updatedAt;

    public AppUser() {
    }

    public AppUser(String id, String email, String name, String pictureUrl, Role role, boolean ssoEnabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.role = role;
        this.ssoEnabled = ssoEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isSsoEnabled() { return ssoEnabled; }
    public void setSsoEnabled(boolean ssoEnabled) { this.ssoEnabled = ssoEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum Role {
        USER,
        ADMIN,
        SUPER_ADMIN
    }

    public static final class Builder {
        private String id;
        private String email;
        private String name;
        private String pictureUrl;
        private Role role;
        private boolean ssoEnabled;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder pictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder ssoEnabled(boolean ssoEnabled) { this.ssoEnabled = ssoEnabled; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public AppUser build() { return new AppUser(id, email, name, pictureUrl, role, ssoEnabled, createdAt, updatedAt); }
    }
}
