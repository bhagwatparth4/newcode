package com.cafe.backend.controller.customer;

import com.cafe.backend.dto.coupon.CouponValidationRequest;
import com.cafe.backend.dto.coupon.CouponValidationResponse;
import com.cafe.backend.security.RateLimitService;
import com.cafe.backend.service.admin.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/customer/coupons")
public class CustomerCouponController {

    private final CouponService couponService;
    private final RateLimitService rateLimitService;

    public CustomerCouponController(CouponService couponService, RateLimitService rateLimitService) {
        this.couponService = couponService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestBody CouponValidationRequest request,
            HttpServletRequest httpRequest
    ) {
        // Stops guessing hidden coupon codes by trying many codes quickly
        if (!rateLimitService.isAllowed("rate-limit:coupon:" + httpRequest.getRemoteAddr(), 20, Duration.ofMinutes(5))) {
            return ResponseEntity.status(429).build();
        }

        return ResponseEntity.ok(
                couponService.validateCoupon(request)
        );
    }
}