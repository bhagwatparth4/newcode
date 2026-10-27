package com.cafe.backend.service.notification;

import com.cafe.backend.dto.notification.OrderNotification;
import com.cafe.backend.entity.Order;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class LateOrderNotificationScheduler {

    private final OrderRepository orderRepository;
    private final OrderNotificationService orderNotificationService;

    public LateOrderNotificationScheduler(
            OrderRepository orderRepository,
            OrderNotificationService orderNotificationService) {
        this.orderRepository = orderRepository;
        this.orderNotificationService = orderNotificationService;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkLateOrders() {

        LocalDateTime now = LocalDateTime.now();

        List<Order> lateOrders =
                orderRepository
                        .findByStatusAndEstimatedReadyAtBeforeAndLateNotificationSentFalse(
                                OrderStatus.PREPARING,
                                now
                        );

        for (Order order : lateOrders) {

            String message =
                    "Sorry, your order is taking a little longer than expected. "
                            + "We appreciate your patience and will have it ready "
                            + "as soon as possible.";

            OrderNotification notification = new OrderNotification(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    message,
                    order.getOrderType(),
                    order.getTable() != null
                            ? order.getTable().getTableNumber()
                            : null
            );

            orderNotificationService.sendOrderNotification(notification);

            order.setLateNotificationSent(true);
            orderRepository.save(order);
        }
    }
}