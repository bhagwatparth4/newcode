package com.cafe.backend.dto.qr;

import com.cafe.backend.enums.OrderType;

public class QrContextResponse {

    private OrderType orderType;

    private String tableNumber;

    private boolean valid;

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

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}