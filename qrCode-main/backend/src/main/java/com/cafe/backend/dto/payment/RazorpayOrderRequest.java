package com.cafe.backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RazorpayOrderRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Customer notification token is required")
    @Size(max = 100, message = "Notification token is too long")
    private String notificationToken;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getNotificationToken() {
        return notificationToken;
    }

    public void setNotificationToken(String notificationToken) {
        this.notificationToken = notificationToken;
    }
}