package com.cafe.backend.dto.payment;

import com.cafe.backend.dto.order.CustomerOrderDetailsResponse;

public class PaymentVerificationResponse {

    private boolean success;
    private Long orderId;
    private String orderNumber;
    private String message;

    private CustomerOrderDetailsResponse orderDetails;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CustomerOrderDetailsResponse getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(CustomerOrderDetailsResponse orderDetails) {
        this.orderDetails = orderDetails;
    }
}