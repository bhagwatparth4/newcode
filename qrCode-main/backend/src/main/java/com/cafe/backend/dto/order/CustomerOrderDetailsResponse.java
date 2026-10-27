package com.cafe.backend.dto.order;

import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.enums.PaymentMethod;
import com.cafe.backend.enums.OrderPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CustomerOrderDetailsResponse {

    private Long orderId;
    private String orderNumber;
    private OrderType orderType;
    private String tableNumber;
    private String nickname;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String paymentStatus;
    private String razorpayPaymentId;
    private String transactionReference;
    private Integer estimatedPreparationMinutes;
    private LocalDateTime estimatedReadyAt;
    private LocalDateTime createdAt;

    private String cookingNote;

    private PaymentMethod paymentMethod;
    private OrderPaymentStatus orderPaymentStatus;

    private java.time.LocalDateTime paidAt;

    private Integer rating;
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public java.time.LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(java.time.LocalDateTime paidAt) { this.paidAt = paidAt; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod m) { this.paymentMethod = m; }
    public OrderPaymentStatus getOrderPaymentStatus() { return orderPaymentStatus; }
    public void setOrderPaymentStatus(OrderPaymentStatus s) { this.orderPaymentStatus = s; }

    public String getCookingNote() {
        return cookingNote;
    }

    public void setCookingNote(String cookingNote) {
        this.cookingNote = cookingNote;
    }

    public CustomerOrderDetailsResponse() {
    }

    public CustomerOrderDetailsResponse(
            Long orderId,
            String orderNumber,
            OrderType orderType,
            String tableNumber,
            String nickname,
            List<OrderItemResponse> items,
            BigDecimal totalAmount,
            OrderStatus status,
            String paymentStatus,
            String razorpayPaymentId,
            String transactionReference,
            Integer estimatedPreparationMinutes,
            LocalDateTime estimatedReadyAt,
            LocalDateTime createdAt
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderType = orderType;
        this.tableNumber = tableNumber;
        this.nickname = nickname;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.razorpayPaymentId = razorpayPaymentId;
        this.transactionReference = transactionReference;
        this.estimatedPreparationMinutes = estimatedPreparationMinutes;
        this.estimatedReadyAt = estimatedReadyAt;
        this.createdAt = createdAt;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
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

    // ---- Added for the customer receipt ----
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private String couponCode;
    private String maskedMobileNumber;
    private LocalDateTime updatedAt;

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

    public String getMaskedMobileNumber() {
        return maskedMobileNumber;
    }

    public void setMaskedMobileNumber(String maskedMobileNumber) {
        this.maskedMobileNumber = maskedMobileNumber;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
