package com.cafe.backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RazorpayPaymentVerificationRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Customer notification token is required")
    @Size(max = 100, message = "Notification token is too long")
    private String notificationToken;

    @NotBlank(message = "Razorpay order ID is required")
    @Size(max = 100, message = "Razorpay order ID is too long")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    @Size(max = 100, message = "Razorpay payment ID is too long")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Size(max = 500, message = "Razorpay signature is too long")
    private String razorpaySignature;

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

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }
}