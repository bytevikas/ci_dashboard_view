package com.cars24.rcview.security;

import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtService {

    private static final long USER_CACHE_TTL_MS = 60_000; // 1 min – avoid MongoDB on every request

    private final AppUserRepository userRepository;

    private final ConcurrentHashMap<String, CachedUser> userCache = new ConcurrentHashMap<>();

    private static final class CachedUser {
        final AppUser user;
        final long expiresAt;

        CachedUser(AppUser user, long ttlMs) {
            this.user = user;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    public JwtService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Value("${app.jwt.secret:rcview-default-secret-key-min-256-bits-for-hs256-please-change-in-production-xyz}")
    private String secret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    public String createToken(AppUser user) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public AppUser resolveUser(String token) {
        if (token == null || token.isBlank()) return null;
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            String userId = claims.getSubject();
            // In dev mode, skip MongoDB lookup and use token claims directly
            if (devMode && claims.get("email") != null) {
                String roleStr = claims.get("role", String.class);
                AppUser.Role role = roleStr != null ? AppUser.Role.valueOf(roleStr) : AppUser.Role.USER;
                return AppUser.builder()
                        .id(userId)
                        .email(String.valueOf(claims.get("email")))
                        .name("Dev User")
                        .role(role)
                        .ssoEnabled(true)
                        .build();
            }
            // Production: short-lived cache to avoid MongoDB on every request
            CachedUser cached = userCache.get(token);
            if (cached != null) {
                if (cached.isExpired()) {
                    userCache.remove(token);
                } else {
                    return cached.user;
                }
            }
            var dbUser = userRepository.findById(userId).orElse(null);
            if (dbUser != null) {
                userCache.put(token, new CachedUser(dbUser, USER_CACHE_TTL_MS));
                return dbUser;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
