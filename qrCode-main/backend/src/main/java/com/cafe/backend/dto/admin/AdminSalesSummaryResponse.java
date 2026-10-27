package com.cafe.backend.dto.admin;

public class AdminSalesSummaryResponse {

    private long totalOrders;
    private long paidOrders;
    private long completedOrders;
    private long todayOrders;
    private long todayCompletedOrders;

    private java.math.BigDecimal totalRevenue;
    private java.math.BigDecimal todayRevenue;

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPaidOrders() {
        return paidOrders;
    }

    public void setPaidOrders(long paidOrders) {
        this.paidOrders = paidOrders;
    }

    public long getCompletedOrders() {
        return completedOrders;
    }

    public void setCompletedOrders(long completedOrders) {
        this.completedOrders = completedOrders;
    }

    public long getTodayOrders() {
        return todayOrders;
    }

    public void setTodayOrders(long todayOrders) {
        this.todayOrders = todayOrders;
    }

    public long getTodayCompletedOrders() {
        return todayCompletedOrders;
    }

    public void setTodayCompletedOrders(long todayCompletedOrders) {
        this.todayCompletedOrders = todayCompletedOrders;
    }

    public java.math.BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(java.math.BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public java.math.BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(java.math.BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }
}