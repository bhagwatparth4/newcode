package com.cafe.backend.service.order;

import com.cafe.backend.dto.notification.OrderNotification;
import com.cafe.backend.entity.Order;
import com.cafe.backend.enums.*;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.service.notification.OrderNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class CashPaymentService {

    private final OrderRepository orderRepository;
    private final OrderNotificationService orderNotificationService;

    public CashPaymentService(OrderRepository orderRepository,
                              OrderNotificationService orderNotificationService) {
        this.orderRepository = orderRepository;
        this.orderNotificationService = orderNotificationService;
    }

    @Transactional
    public void markCashPaid(Long orderId, String staffName) {

        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getPaymentMethod() != PaymentMethod.CASH) {
            throw new ConflictException("NOT_CASH_ORDER", "Only cash orders can be marked as paid.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("ORDER_CANCELLED", "This order is cancelled.");
        }
        // Double click / two admins -> safe, nothing happens twice
        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            return;
        }

        order.setPaymentStatus(OrderPaymentStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        // Counter orders: credit the worker who took the order; QR orders: whoever pressed Mark as paid
        order.setPaidBy(order.getPlacedBy() != null ? order.getPlacedBy() : staffName);
        orderRepository.save(order);
        // Other admin tabs / devices refresh live
        orderNotificationService.sendStaffOrderUpdate(order);
        // Customer page turns orange -> green live
        orderNotificationService.sendOrderNotification(
                new OrderNotification(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        "Cash payment received. Thank you!",
                        order.getOrderType(),
                        order.getTable() != null ? order.getTable().getTableNumber() : null
                )
        );
    }

    @Transactional
    public void cancelCashOrder(Long orderId, String staffName) {

        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return; // already cancelled, double click safe
        }
        if (order.getPaymentMethod() != PaymentMethod.CASH
                || order.getPaymentStatus() == OrderPaymentStatus.PAID
                || order.getStatus() == OrderStatus.COMPLETED) {
            throw new ConflictException(
                    "CANCEL_NOT_ALLOWED",
                    "Only unpaid cash orders can be cancelled."
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        System.out.println("CASH ORDER CANCELLED → " + order.getOrderNumber() + " by " + staffName);

        // Customer page
        orderNotificationService.sendOrderNotification(
                new OrderNotification(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        "Your order has been cancelled by the cafe.",
                        order.getOrderType(),
                        order.getTable() != null ? order.getTable().getTableNumber() : null
                )
        );

        // Admin + kitchen boards get the CANCELLED update live
        orderNotificationService.sendStaffOrderUpdate(order);
    }

    @Transactional
    public void cancelByCustomer(String notificationToken) {

        Order found = orderRepository.findByCustomerNotificationToken(notificationToken)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        // Lock first, so the kitchen can't start it at the same moment
        Order order = orderRepository.findLockedById(found.getId())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new ConflictException(
                    "KITCHEN_STARTED",
                    "The kitchen has already started your order. Please talk to the staff."
            );
        }

        // Same rules + kitchen/customer notifications as admin cancel
        cancelCashOrder(order.getId(), "CUSTOMER");
    }

    @Transactional
    public void markCancelSeen(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        order.setCancelSeen(true);
        orderRepository.save(order);
        orderNotificationService.sendStaffOrderUpdate(order); // other screens remove it too
    }
}