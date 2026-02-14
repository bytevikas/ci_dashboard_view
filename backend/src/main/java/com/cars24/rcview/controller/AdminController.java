package com.cars24.rcview.controller;

import com.cars24.rcview.entity.AppConfig;
import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.service.AdminService;
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
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            @RequestParam(value = "search", required = false) String search) {
        try {
            List<AppUser> users = adminService.listUsers(search);
            List<Map<String, Object>> dtos = users.stream()
                    .map(u -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", u.getId() != null ? u.getId() : "");
                        m.put("email", u.getEmail() != null ? u.getEmail() : "");
                        m.put("name", u.getName());
                        m.put("role", u.getRole() != null ? u.getRole().name() : "USER");
                        m.put("ssoEnabled", u.isSsoEnabled());
                        m.put("createdAt", u.getCreatedAt());
                        return m;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> addOrEnableUser(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String name = (String) body.get("name");
        String roleStr = (String) body.get("role");
        boolean ssoEnabled = body.get("ssoEnabled") != null && (Boolean) body.get("ssoEnabled");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        AppUser.Role role = AppUser.Role.USER;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = AppUser.Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // invalid role string → default to USER
            }
        }
        try {
            AppUser user = adminService.addOrEnableUser(email, name, role, ssoEnabled);
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole().name(),
                    "ssoEnabled", user.isSsoEnabled()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> removeUser(@PathVariable String userId) {
        try {
            adminService.removeUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return mongoUnavailable();
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
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        try {
            return ResponseEntity.ok(adminService.getConfig());
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    @PutMapping("/config")
    public ResponseEntity<?> updateConfig(@RequestBody Map<String, Integer> body) {
        int cacheTtlDays = body.getOrDefault("cacheTtlDays", 3);
        int rateLimitPerSecond = body.getOrDefault("rateLimitPerSecond", 5);
        int rateLimitPerDayDefault = body.getOrDefault("rateLimitPerDayDefault", 100);
        try {
            AppConfig config = adminService.updateConfig(cacheTtlDays, rateLimitPerSecond, rateLimitPerDayDefault);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            return ResponseEntity.ok(adminService.getAuditLogs(pageable));
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    @GetMapping("/search-stats")
    public ResponseEntity<?> getSearchStats() {
        try {
            return ResponseEntity.ok(adminService.getSearchStats());
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    @GetMapping("/search-logs")
    public ResponseEntity<?> getSearchLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            return ResponseEntity.ok(adminService.getSearchLogs(pageable));
        } catch (Exception e) {
            return mongoUnavailable();
        }
    }

    private ResponseEntity<?> mongoUnavailable() {
        return ResponseEntity.status(503).body(Map.of(
                "error", "Database is not reachable at the moment. Read-only data from in-memory store may still be available.",
                "errorMessage", "Database is not reachable. Some features may be limited."));
    }
}
