package com.cafe.backend.dto.admin;

import java.math.BigDecimal;

public class TopSellingItemResponse {

    private String itemName;
    private long quantitySold;
    private BigDecimal revenue;

    public TopSellingItemResponse() {
    }

    public TopSellingItemResponse(
            String itemName,
            long quantitySold,
            BigDecimal revenue
    ) {
        this.itemName = itemName;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public long getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(long quantitySold) {
        this.quantitySold = quantitySold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}