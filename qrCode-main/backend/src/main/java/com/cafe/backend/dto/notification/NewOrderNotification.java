package com.cafe.backend.dto.notification;

import com.cafe.backend.enums.OrderType;

import java.math.BigDecimal;

public class NewOrderNotification {

    private Long orderId;
    private String orderNumber;
    private OrderType orderType;
    private String tableNumber;
    private String nickname;
    private BigDecimal totalAmount;

    public NewOrderNotification(
            Long orderId,
            String orderNumber,
            OrderType orderType,
            String tableNumber,
            String nickname,
            BigDecimal totalAmount
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderType = orderType;
        this.tableNumber = tableNumber;
        this.nickname = nickname;
        this.totalAmount = totalAmount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}