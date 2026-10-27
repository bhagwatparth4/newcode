package com.cafe.backend.dto.notification;

import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;

public class OrderNotification {

    private Long orderId;
    private String orderNumber;
    private OrderStatus orderStatus;
    private String message;
    private OrderType orderType;
    private String tableNumber;

    public OrderNotification() {
    }

    public OrderNotification(
            Long orderId,
            String orderNumber,
            OrderStatus orderStatus,
            String message,
            OrderType orderType,
            String tableNumber
    ) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderStatus = orderStatus;
        this.message = message;
        this.orderType = orderType;
        this.tableNumber = tableNumber;
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

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
}