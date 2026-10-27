package com.cafe.backend.dto.admin;

import com.cafe.backend.dto.order.OrderItemResponse;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.enums.PaymentMethod;
import com.cafe.backend.enums.OrderPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminOrderResponse {

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


    private String paymentStatus;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String transactionReference;

    private String cookingNote;

    public String getCookingNote() {
        return cookingNote;
    }

    // ---- Cash payment ----
    private PaymentMethod paymentMethod;
    private OrderPaymentStatus orderPaymentStatus;
    private LocalDateTime paidAt;
    private String paidBy;

    private Integer rating;
    private String feedback;
    public Integer getRating() { return rating; }

    private String counterPaymentMode;   // CASH / UPI / CARD (label only)

    private String placedBy;             // staff who created a counter order

    public String getCounterPaymentMode() { return counterPaymentMode; }
    public void setCounterPaymentMode(String v) { this.counterPaymentMode = v; }
    public String getPlacedBy() { return placedBy; }
    public void setPlacedBy(String v) { this.placedBy = v; }


    public void setRating(Integer rating) { this.rating = rating; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod m) { this.paymentMethod = m; }
    public OrderPaymentStatus getOrderPaymentStatus() { return orderPaymentStatus; }
    public void setOrderPaymentStatus(OrderPaymentStatus s) { this.orderPaymentStatus = s; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }

    public void setCookingNote(String cookingNote) {
        this.cookingNote = cookingNote;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    // ---- Added for order details & receipts ----
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private String couponCode;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
