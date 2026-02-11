package com.cars24.rcview.controller;

import com.cars24.rcview.entity.AppConfig;
import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.entity.AuditLog;
import com.cars24.rcview.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String search) {
        List<AppUser> users = adminService.listUsers(search);
        List<Map<String, Object>> dtos = users.stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("email", u.getEmail());
                    m.put("name", u.getName());
                    m.put("role", u.getRole().name());
                    m.put("ssoEnabled", u.isSsoEnabled());
                    m.put("createdAt", u.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/users")
    public ResponseEntity<?> addOrEnableUser(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String name = (String) body.get("name");
        boolean ssoEnabled = body.get("ssoEnabled") != null && (Boolean) body.get("ssoEnabled");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        try {
            AppUser user = adminService.addOrEnableUser(email, name, ssoEnabled);
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole().name(),
                    "ssoEnabled", user.isSsoEnabled()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> removeUser(@PathVariable String userId) {
        try {
            adminService.removeUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<?> setRole(@PathVariable String userId, @RequestBody Map<String, String> body) {
        String roleStr = body != null ? body.get("role") : null;
        if (roleStr == null || roleStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "role is required"));
        }
        AppUser.Role role;
        try {
            role = AppUser.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: USER, ADMIN, SUPER_ADMIN"));
        }
        try {
            AppUser user = adminService.setRole(userId, role);
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "role", user.getRole().name()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<AppConfig> getConfig() {
        return ResponseEntity.ok(adminService.getConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<AppConfig> updateConfig(@RequestBody Map<String, Integer> body) {
        int cacheTtlDays = body.getOrDefault("cacheTtlDays", 3);
        int rateLimitPerSecond = body.getOrDefault("rateLimitPerSecond", 5);
        int rateLimitPerDayDefault = body.getOrDefault("rateLimitPerDayDefault", 100);
        AppConfig config = adminService.updateConfig(cacheTtlDays, rateLimitPerSecond, rateLimitPerDayDefault);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(adminService.getAuditLogs(pageable));
    }
}
