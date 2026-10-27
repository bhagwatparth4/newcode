package com.cafe.backend.controller.customer;

import com.cafe.backend.dto.notification.NotificationResponse;
import com.cafe.backend.entity.Notification;
import com.cafe.backend.entity.Order;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.service.notification.OrderNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/notifications")
public class CustomerNotificationController {

    private final OrderNotificationService orderNotificationService;
    private final OrderRepository orderRepository;

    public CustomerNotificationController(
            OrderNotificationService orderNotificationService,
            OrderRepository orderRepository
    ) {
        this.orderNotificationService = orderNotificationService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/order/{notificationToken}")
    public ResponseEntity<List<NotificationResponse>> getOrderNotifications(
            @PathVariable String notificationToken) {

        Order order = orderRepository
                .findByCustomerNotificationToken(notificationToken)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Invalid notification token"));

        List<NotificationResponse> notifications =
                orderNotificationService
                        .getOrderNotifications(order.getId())
                        .stream()
                        .map(notification -> new NotificationResponse(
                                notification.getId(),
                                notification.getType(),
                                notification.getTitle(),
                                notification.getMessage(),
                                notification.isRead(),
                                notification.getCreatedAt(),
                                notification.getDeliveredAt()
                        ))
                        .toList();

        return ResponseEntity.ok(notifications);
    }


    @PatchMapping("/order/{notificationToken}/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String notificationToken,
            @PathVariable Long notificationId) {

        Order order = orderRepository
                .findByCustomerNotificationToken(notificationToken)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Invalid notification token"));

        List<Notification> notifications =
                orderNotificationService.getOrderNotifications(order.getId());

        boolean belongsToOrder = notifications.stream()
                .anyMatch(notification -> notification.getId().equals(notificationId));

        if (!belongsToOrder) {
            throw new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification does not belong to this order");
        }

        orderNotificationService.markAsRead(notificationId);

        return ResponseEntity.noContent().build();
    }
}