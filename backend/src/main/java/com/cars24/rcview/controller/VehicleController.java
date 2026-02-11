package com.cars24.rcview.controller;

import com.cars24.rcview.dto.VehicleSearchResponse;
import com.cars24.rcview.service.RateLimitService;
import com.cars24.rcview.service.UserService;
import com.cars24.rcview.service.VehicleSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleSearchService vehicleSearchService;
    private final UserService userService;
    private final RateLimitService rateLimitService;

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

    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> rateLimitInfo() {
        var user = userService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        long remaining = rateLimitService.getRemainingDailyCount(user.getId());
        return ResponseEntity.ok(Map.of("remainingSearchesToday", remaining));
    }
}
