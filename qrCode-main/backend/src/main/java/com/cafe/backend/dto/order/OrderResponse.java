package com.cafe.backend.dto.order;

import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.enums.PaymentMethod;
import com.cafe.backend.enums.OrderPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderType orderType;
    private String tableNumber;
    private String nickname;
    private String mobileNumber;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Integer estimatedPreparationMinutes;
    private LocalDateTime estimatedReadyAt;
    private LocalDateTime createdAt;

    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private String couponCode;

    // Secret token the customer uses for payment, tracking and WebSocket updates
    private String customerNotificationToken;

    private PaymentMethod paymentMethod;
    private OrderPaymentStatus orderPaymentStatus;

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod m) { this.paymentMethod = m; }
    public OrderPaymentStatus getOrderPaymentStatus() { return orderPaymentStatus; }
    public void setOrderPaymentStatus(OrderPaymentStatus s) { this.orderPaymentStatus = s; }

    public String getCustomerNotificationToken() {
        return customerNotificationToken;
    }

    public void setCustomerNotificationToken(String customerNotificationToken) {
        this.customerNotificationToken = customerNotificationToken;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Integer getEstimatedPreparationMinutes() {
        return estimatedPreparationMinutes;
    }

    public void setEstimatedPreparationMinutes(Integer estimatedPreparationMinutes) {
        this.estimatedPreparationMinutes = estimatedPreparationMinutes;
    }

    public LocalDateTime getEstimatedReadyAt() {
        return estimatedReadyAt;
    }

    public void setEstimatedReadyAt(LocalDateTime estimatedReadyAt) {
        this.estimatedReadyAt = estimatedReadyAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}