package com.cars24.rcview.security;

import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.entity.AuditLog;
import com.cars24.rcview.repository.AppUserRepository;
import com.cars24.rcview.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Value("${app.super-admin-email:vikas.kumar8@cars24.com}")
    private String superAdminEmail;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);
        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not provided by Google");
        }

        Optional<AppUser> existing = userRepository.findByEmail(email);
        AppUser user;

        if (existing.isPresent()) {
            user = existing.get();
            if (!user.isSsoEnabled()) {
                throw new OAuth2AuthenticationException("SSO login is not enabled for this account. Contact admin.");
            }
            user.setName(oauth2User.getAttribute("name") != null ? oauth2User.getAttribute("name").toString() : null);
            user.setPictureUrl(oauth2User.getAttribute("picture") != null ? oauth2User.getAttribute("picture").toString() : null);
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
        } else {
            // First-time login: only allow if super admin or if we auto-provision (here we don't auto-provision non-super-admin)
            if (!email.equalsIgnoreCase(superAdminEmail)) {
                throw new OAuth2AuthenticationException("Account not found. Contact admin to enable SSO.");
            }
            user = AppUser.builder()
                    .email(email)
                    .name(oauth2User.getAttribute("name") != null ? oauth2User.getAttribute("name").toString() : null)
                    .pictureUrl(oauth2User.getAttribute("picture") != null ? oauth2User.getAttribute("picture").toString() : null)
                    .role(AppUser.Role.SUPER_ADMIN)
                    .ssoEnabled(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            user = userRepository.save(user);
        }

        if (!devMode) {
            auditLogRepository.save(AuditLog.builder()
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .action(AuditLog.AuditAction.USER_LOGIN)
                    .createdAt(Instant.now())
                    .build());
        }

        return new CustomOAuth2User(oauth2User, user);
    }
}
