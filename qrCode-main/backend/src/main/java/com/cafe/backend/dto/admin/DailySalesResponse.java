package com.cafe.backend.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailySalesResponse {

    private LocalDate date;
    private long orders;
    private BigDecimal revenue;

    public DailySalesResponse() {
    }

    public DailySalesResponse(LocalDate date, long orders, BigDecimal revenue) {
        this.date = date;
        this.orders = orders;
        this.revenue = revenue;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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