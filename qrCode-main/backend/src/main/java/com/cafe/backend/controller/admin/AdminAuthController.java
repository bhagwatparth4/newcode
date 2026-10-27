package com.cafe.backend.controller.admin;

import com.cafe.backend.dto.admin.AdminLoginRequest;
import com.cafe.backend.dto.admin.AdminLoginResponse;
import com.cafe.backend.service.auth.AdminAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cafe.backend.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final RateLimitService rateLimitService;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            RateLimitService rateLimitService
    ) {
        this.adminAuthService = adminAuthService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(
            @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest
    ) {

        String clientIp = httpRequest.getRemoteAddr();

        boolean allowed = rateLimitService.isAllowed(
                "rate-limit:admin-login:" + clientIp,
                5,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            return ResponseEntity.status(429).build();
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        boolean emailAllowed = rateLimitService.isAllowed(
                "rate-limit:admin-login-email:" + email,
                10,
                Duration.ofMinutes(15)
        );
        if (!emailAllowed) {
            return ResponseEntity.status(429).build();
        }

        return ResponseEntity.ok(
                adminAuthService.login(request)
        );
    }
}