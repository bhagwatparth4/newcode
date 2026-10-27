package com.cafe.backend.dto.admin;

import java.math.BigDecimal;

public class OrderTypeSalesResponse {

    private String orderType;
    private long orders;
    private BigDecimal revenue;

    public OrderTypeSalesResponse() {
    }

    public OrderTypeSalesResponse(
            String orderType,
            long orders,
            BigDecimal revenue
    ) {
        this.orderType = orderType;
        this.orders = orders;
        this.revenue = revenue;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public long getOrders() {
        return orders;
    }

    public void setOrders(long orders) {
        this.orders = orders;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}