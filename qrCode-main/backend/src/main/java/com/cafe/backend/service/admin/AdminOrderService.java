package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.AdminOrderResponse;
import com.cafe.backend.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalDateTime;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;

public interface AdminOrderService {

    List<AdminOrderResponse> getAllOrders(
            OrderStatus status,
            OrderType orderType,
            LocalDateTime from,
            LocalDateTime to
    );

    AdminOrderResponse getOrderById(Long id);

    AdminOrderResponse updateOrderStatus(Long id, String newStatus);

    // PAID / PREPARING / READY orders for the live board (oldest first)
    List<AdminOrderResponse> getActiveOrders();
}