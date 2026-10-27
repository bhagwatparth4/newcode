package com.cafe.backend.dto.order;

import java.math.BigDecimal;

public class OrderItemResponse {

    private Long menuItemId;

    private String itemName;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;

    private String addonsText;

    public String getAddonsText() { return addonsText; }


    private String addonsDetail;   // "Large|120.00;Extra cheese|40.00"

    public String getAddonsDetail() { return addonsDetail; }
    public void setAddonsDetail(String addonsDetail) { this.addonsDetail = addonsDetail; }
    public void setAddonsText(String addonsText) { this.addonsText = addonsText; }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}