package com.cafe.backend.service.order;

import com.cafe.backend.dto.order.CustomerOrderStatusResponse;
import com.cafe.backend.dto.order.OrderCreateRequest;
import com.cafe.backend.dto.order.OrderResponse;
import com.cafe.backend.dto.order.CustomerOrderDetailsResponse;
public interface OrderService {

    OrderResponse createOrder(OrderCreateRequest request);

    OrderResponse getOrderById(Long id);

//    OrderResponse updateOrderStatus(Long id, OrderStatus status);
    CustomerOrderStatusResponse getCustomerOrderStatus(String notificationToken);

    CustomerOrderDetailsResponse getCustomerOrderDetails(String notificationToken);
}