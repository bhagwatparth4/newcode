package com.cafe.backend.controller.payment;

import com.cafe.backend.service.payment.PaymentWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/razorpay")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(
            PaymentWebhookService paymentWebhookService
    ) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false)
            String signature,

            @RequestHeader(value = "x-razorpay-event-id", required = false)
            String eventId,

            @RequestBody String payload
    ) {

        paymentWebhookService.processWebhook(
                payload,
                signature,
                eventId
        );

        return ResponseEntity.ok("Webhook processed");
    }
}