package com.cafe.backend.service.payment;

public interface PaymentWebhookService {

    void processWebhook(
            String payload,
            String signature,
            String eventId
    );
}