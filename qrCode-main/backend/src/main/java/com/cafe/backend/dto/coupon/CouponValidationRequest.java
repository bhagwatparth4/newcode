package com.cafe.backend.dto.coupon;

import java.math.BigDecimal;

public class CouponValidationRequest {

    private String code;
    private BigDecimal orderAmount;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }
}