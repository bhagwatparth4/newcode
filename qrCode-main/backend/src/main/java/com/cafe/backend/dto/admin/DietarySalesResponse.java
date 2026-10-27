package com.cafe.backend.dto.admin;

import java.math.BigDecimal;

public class DietarySalesResponse {

    private String dietaryType;
    private long quantitySold;
    private BigDecimal revenue;

    public DietarySalesResponse() {
    }

    public DietarySalesResponse(
            String dietaryType,
            long quantitySold,
            BigDecimal revenue
    ) {
        this.dietaryType = dietaryType;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public String getDietaryType() {
        return dietaryType;
    }

    public void setDietaryType(String dietaryType) {
        this.dietaryType = dietaryType;
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