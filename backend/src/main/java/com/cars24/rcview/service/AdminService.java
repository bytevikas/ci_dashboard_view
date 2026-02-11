package com.cars24.rcview.service;

import com.cars24.rcview.entity.AppConfig;
import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.entity.AuditLog;
import com.cars24.rcview.repository.AppUserRepository;
import com.cars24.rcview.repository.AuditLogRepository;
import com.cars24.rcview.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AppUserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ConfigService configService;

    @Value("${app.super-admin-email:vikas.kumar8@cars24.com}")
    private String superAdminEmail;

    public boolean isSuperAdmin(String email) {
        return email != null && email.equalsIgnoreCase(superAdminEmail);
    }

    public boolean canModifyUser(String targetUserId) {
        AppUser target = userRepository.findById(targetUserId).orElse(null);
        if (target == null) return false;
        return !isSuperAdmin(target.getEmail());
    }

    public List<AppUser> listUsers(String search) {
        if (search != null && !search.isBlank()) {
            return userRepository.findAll().stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(search.toLowerCase())
                            || (u.getName() != null && u.getName().toLowerCase().contains(search.toLowerCase())))
                    .toList();
        }
        return userRepository.findAll();
    }

    public AppUser addOrEnableUser(String email, String name, boolean ssoEnabled) {
        if (isSuperAdmin(email)) {
            throw new IllegalArgumentException("Cannot modify super admin");
        }
        return userRepository.findByEmail(email)
                .map(u -> {
                    u.setSsoEnabled(ssoEnabled);
                    u.setName(name != null ? name : u.getName());
                    u.setUpdatedAt(Instant.now());
                    return userRepository.save(u);
                })
                .orElseGet(() -> {
                    AppUser newUser = AppUser.builder()
                            .email(email)
                            .name(name)
                            .role(AppUser.Role.USER)
                            .ssoEnabled(ssoEnabled)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return userRepository.save(newUser);
                });
    }

    public void removeUser(String userId) {
        if (!canModifyUser(userId)) {
            throw new IllegalArgumentException("Cannot remove this user");
        }
        userRepository.deleteById(userId);
    }

    public AppUser setRole(String userId, AppUser.Role role) {
        if (!canModifyUser(userId)) {
            throw new IllegalArgumentException("Cannot change role for this user");
        }
        AppUser user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    public AppConfig getConfig() {
        return configService.getConfig();
    }

    public AppConfig updateConfig(int cacheTtlDays, int rateLimitPerSecond, int rateLimitPerDayDefault) {
        String updatedBy = getCurrentUserEmail();
        return configService.updateConfig(cacheTtlDays, rateLimitPerSecond, rateLimitPerDayDefault, updatedBy);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomOAuth2User user) {
            return user.getAppUser().getEmail();
        }
        return null;
    }
}
