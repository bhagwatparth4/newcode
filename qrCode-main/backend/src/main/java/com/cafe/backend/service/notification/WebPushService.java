package com.cafe.backend.service.notification;

import com.cafe.backend.entity.PushSubscription;

public interface WebPushService {

    void sendNotification(
            PushSubscription subscription,
            String title,
            String message
    );

    // True only when VAPID keys are configured. When false, Web Push is
    // simply skipped and ordering keeps working with WebSocket updates.
    boolean isConfigured();

    // Public VAPID key the browser needs to subscribe (safe to share).
    String getPublicKey();
}