package com.cars24.rcview.dto;

import com.cars24.rcview.entity.AppUser;

public class UserInfoDto {

    private String id;
    private String email;
    private String name;
    private String pictureUrl;
    private AppUser.Role role;
    private boolean ssoEnabled;

    public UserInfoDto() {
    }

    public UserInfoDto(String id, String email, String name, String pictureUrl, AppUser.Role role, boolean ssoEnabled) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.role = role;
        this.ssoEnabled = ssoEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static UserInfoDto from(AppUser u) {
        return UserInfoDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .pictureUrl(u.getPictureUrl())
                .role(u.getRole())
                .ssoEnabled(u.isSsoEnabled())
                .build();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }
    public AppUser.Role getRole() { return role; }
    public void setRole(AppUser.Role role) { this.role = role; }
    public boolean isSsoEnabled() { return ssoEnabled; }
    public void setSsoEnabled(boolean ssoEnabled) { this.ssoEnabled = ssoEnabled; }

    public static final class Builder {
        private String id;
        private String email;
        private String name;
        private String pictureUrl;
        private AppUser.Role role;
        private boolean ssoEnabled;

        public Builder id(String id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder pictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; return this; }
        public Builder role(AppUser.Role role) { this.role = role; return this; }
        public Builder ssoEnabled(boolean ssoEnabled) { this.ssoEnabled = ssoEnabled; return this; }
        public UserInfoDto build() { return new UserInfoDto(id, email, name, pictureUrl, role, ssoEnabled); }
    }
}
