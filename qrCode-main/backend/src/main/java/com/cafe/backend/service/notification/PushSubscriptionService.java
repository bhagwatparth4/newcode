package com.cafe.backend.service.notification;

import com.cafe.backend.dto.notification.PushSubscriptionRequest;

public interface PushSubscriptionService {

    void subscribe(PushSubscriptionRequest request);
}