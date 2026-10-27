package com.cafe.backend.controller.customer;

import java.util.Map;
import com.cafe.backend.service.notification.WebPushService;
import com.cafe.backend.dto.notification.PushSubscriptionRequest;
import com.cafe.backend.service.notification.PushSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cafe.backend.security.RateLimitService;

import java.time.Duration;

@RestController
@RequestMapping("/api/customer/push")
public class PushSubscriptionController {

    private final RateLimitService rateLimitService;
    private final PushSubscriptionService pushSubscriptionService;
    private final WebPushService webPushService;

    public PushSubscriptionController(
            RateLimitService rateLimitService, PushSubscriptionService pushSubscriptionService,
            WebPushService webPushService) {
        this.rateLimitService = rateLimitService;
        this.pushSubscriptionService = pushSubscriptionService;
        this.webPushService = webPushService;
    }

    /*
     * The browser needs the PUBLIC VAPID key to subscribe.
     * It is meant to be public. The private key never leaves the server.
     * Empty string = Web Push not configured (customer page hides the button).
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushService.getPublicKey()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(
            @RequestBody PushSubscriptionRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();

        boolean allowed = rateLimitService.isAllowed(
                "rate-limit:push-subscribe:" + clientIp,
                10,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            return ResponseEntity.status(429).build();
        }

        pushSubscriptionService.subscribe(request);

        return ResponseEntity.noContent().build();
    }
}