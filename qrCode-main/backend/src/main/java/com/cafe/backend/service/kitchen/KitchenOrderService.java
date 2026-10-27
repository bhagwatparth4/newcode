package com.cafe.backend.service.kitchen;

import com.cafe.backend.dto.admin.AdminOrderResponse;

import java.util.List;

public interface KitchenOrderService {

    List<AdminOrderResponse> getActiveOrders();

    AdminOrderResponse getOrderById(Long id);

    AdminOrderResponse updateOrderStatus(Long id, String newStatus);
}