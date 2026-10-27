package com.cafe.backend.dto.order;

import com.cafe.backend.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CustomerOrderStatusResponse {

    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private String orderType;
    private String tableNumber;
    private String nickname;
    private BigDecimal totalAmount;

    private Integer estimatedPreparationMinutes;
    private LocalDateTime estimatedReadyAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerOrderStatusResponse() {
    }

    public CustomerOrderStatusResponse(
            Long orderId,
            String orderNumber,
            OrderStatus status,
            String orderType,
            String tableNumber,
            String nickname,
            BigDecimal totalAmount,
            Integer estimatedPreparationMinutes,
            LocalDateTime estimatedReadyAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.status = status;
        this.orderType = orderType;
        this.tableNumber = tableNumber;
        this.nickname = nickname;
        this.totalAmount = totalAmount;
        this.estimatedPreparationMinutes = estimatedPreparationMinutes;
        this.estimatedReadyAt = estimatedReadyAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}