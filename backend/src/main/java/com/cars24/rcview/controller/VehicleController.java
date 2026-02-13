package com.cars24.rcview.controller;

import com.cars24.rcview.dto.VehicleSearchResponse;
import com.cars24.rcview.dto.UserInfoDto;
import com.cars24.rcview.entity.AppConfig;
import com.cars24.rcview.entity.AuditLog;
import com.cars24.rcview.repository.AuditLogRepository;
import com.cars24.rcview.service.ConfigService;
import com.cars24.rcview.service.RateLimitService;
import com.cars24.rcview.service.UserService;
import com.cars24.rcview.service.VehicleSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/vehicle")
public class VehicleController {

    private final VehicleSearchService vehicleSearchService;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final ConfigService configService;
    private final AuditLogRepository auditLogRepository;

    public VehicleController(VehicleSearchService vehicleSearchService, UserService userService,
                             RateLimitService rateLimitService, ConfigService configService,
                             AuditLogRepository auditLogRepository) {
        this.vehicleSearchService = vehicleSearchService;
        this.userService = userService;
        this.rateLimitService = rateLimitService;
        this.configService = configService;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping("/search")
    public ResponseEntity<VehicleSearchResponse> search(@RequestBody Map<String, String> body) {
        String regNo = body != null ? body.get("registrationNumber") : null;
        if (regNo == null || regNo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(VehicleSearchResponse.builder()
                            .success(false)
                            .errorMessage("registrationNumber is required")
                            .build());
        }
        VehicleSearchResponse result = vehicleSearchService.search(regNo);
        if (!result.isSuccess() && result.getErrorMessage() != null) {
            int status = "Unauthorized".equals(result.getErrorMessage()) ? 401
                    : result.getErrorMessage().contains("limit") ? 429 : 400;
            return ResponseEntity.status(status).body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Unmask a registration number. This is an audited action – the user must
     * have acknowledged the sensitive-data warning on the frontend before calling.
     */
    @PostMapping("/unmask")
    public ResponseEntity<Map<String, String>> unmask(@RequestBody Map<String, String> body) {
        UserInfoDto currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        String regNo = body != null ? body.get("registrationNumber") : null;
        if (regNo == null || regNo.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "registrationNumber is required"));
        }
        String full = vehicleSearchService.unmask(regNo);
        if (full == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid registration number"));
        }
        // Audit log the unmask action
        auditLogRepository.save(AuditLog.builder()
                .userId(currentUser.getId())
                .userEmail(currentUser.getEmail())
                .action(AuditLog.AuditAction.UNMASK_REG_NUMBER)
                .registrationNumber(full)
                .details("User acknowledged sensitive-data warning and unmasked registration number")
                .createdAt(Instant.now())
                .build());
        return ResponseEntity.ok(Map.of("registrationNumber", full));
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> rateLimitInfo() {
        var user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        long remaining = rateLimitService.getRemainingDailyCount(user.getId());
        AppConfig config = configService.getConfig();
        // updatedBy is only set when an admin explicitly saves the config
        boolean adminConfigured = config.getUpdatedBy() != null;
        return ResponseEntity.ok(Map.of(
                "remainingSearchesToday", remaining,
                "dailyLimit", config.getRateLimitPerDayDefault(),
                "adminConfigured", adminConfigured
        ));
    }
}
