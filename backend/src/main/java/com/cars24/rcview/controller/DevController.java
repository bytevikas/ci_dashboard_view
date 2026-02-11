package com.cars24.rcview.controller;

import com.cars24.rcview.entity.AppUser;
import com.cars24.rcview.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Only active when app.dev-mode=true. Use for local testing without Google SSO or MongoDB.
 */
@RestController
@RequestMapping("/dev")
public class DevController {

    private final JwtService jwtService;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    public DevController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/login")
    public ResponseEntity<?> devLogin() {
        if (!devMode) {
            return ResponseEntity.notFound().build();
        }
        AppUser devUser = AppUser.builder()
                .id("dev")
                .email("dev@test.com")
                .name("Dev User")
                .role(AppUser.Role.ADMIN)
                .ssoEnabled(true)
                .build();
        String token = jwtService.createToken(devUser);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
