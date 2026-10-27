package com.cafe.backend.controller.payment;

import com.cafe.backend.dto.payment.PaymentVerificationResponse;
import com.cafe.backend.dto.payment.RazorpayOrderRequest;
import com.cafe.backend.dto.payment.RazorpayOrderResponse;
import com.cafe.backend.dto.payment.RazorpayPaymentVerificationRequest;
import com.cafe.backend.service.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.cafe.backend.security.RateLimitService;

import java.time.Duration;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final RateLimitService rateLimitService;
    private final PaymentService paymentService;

    public PaymentController(RateLimitService rateLimitService, PaymentService paymentService) {
        this.rateLimitService = rateLimitService;
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/order")
    public ResponseEntity<RazorpayOrderResponse> createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = httpRequest.getRemoteAddr();

        boolean allowed = rateLimitService.isAllowed(
                "rate-limit:razorpay-order:" + clientIp,
                30, // many customers share the cafe Wi-Fi (same IP)
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            return ResponseEntity.status(429).build();
        }

        RazorpayOrderResponse response =
                paymentService.createRazorpayOrder(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @Valid @RequestBody RazorpayPaymentVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = httpRequest.getRemoteAddr();

        boolean allowed = rateLimitService.isAllowed(
                "rate-limit:razorpay-verify:" + clientIp,
                30, // many customers share the cafe Wi-Fi (same IP)
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            return ResponseEntity.status(429).build();
        }

        PaymentVerificationResponse response =
                paymentService.verifyPayment(request);

        return ResponseEntity.ok(response);
    }

}