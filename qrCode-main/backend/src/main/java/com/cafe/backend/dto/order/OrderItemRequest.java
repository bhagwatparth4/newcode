package com.cafe.backend.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class OrderItemRequest {

    @NotNull(message = "Menu item ID is required")
    @Positive(message = "Invalid menu item ID")
    private Long menuItemId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    @Max(value = 50, message = "Maximum quantity for one item is 50")
    private Integer quantity;

    private java.util.List<Long> addonIds;

    public java.util.List<Long> getAddonIds() { return addonIds; }
    public void setAddonIds(java.util.List<Long> addonIds) { this.addonIds = addonIds; }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}