package com.cafe.backend.service.admin;

import java.util.Comparator;
import com.cafe.backend.service.order.OrderStatusMessages;
import com.cafe.backend.service.order.OrderStatusTransitionPolicy;
import com.cafe.backend.dto.admin.AdminOrderResponse;
import com.cafe.backend.dto.notification.OrderNotification;
import com.cafe.backend.dto.order.OrderItemResponse;
import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.OrderItem;
import com.cafe.backend.entity.Payment;
import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.OrderItemRepository;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cafe.backend.service.notification.OrderNotificationService;
import com.cafe.backend.enums.PaymentMethod;
import com.cafe.backend.enums.OrderPaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    private final OrderNotificationService orderNotificationService;

    private final CafeSettingService cafeSettingService;

    public AdminOrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            OrderNotificationService orderNotificationService, CafeSettingService cafeSettingService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.orderNotificationService = orderNotificationService;
        this.cafeSettingService = cafeSettingService;
    }

    @Override
    public List<AdminOrderResponse> getAllOrders(
            OrderStatus status,
            OrderType orderType,
            LocalDateTime from,
            LocalDateTime to) {

        List<Order> orders;

        if (from != null && to != null && status != null && orderType != null) {

            orders = orderRepository
                    .findByOrderTypeAndStatusAndCreatedAtBetween(
                            orderType, status, from, to
                    );

        } else if (from != null && to != null && status != null) {

            orders = orderRepository
                    .findByStatusAndCreatedAtBetween(status, from, to);

        } else if (from != null && to != null && orderType != null) {

            orders = orderRepository
                    .findByOrderTypeAndCreatedAtBetween(
                            orderType, from, to
                    );

        } else if (from != null && to != null) {

            orders = orderRepository
                    .findByCreatedAtBetween(from, to);

        } else if (status != null || orderType != null) {

            orders = orderRepository.findAll()
                    .stream()
                    .filter(order ->
                            (status == null || order.getStatus() == status)
                                    &&
                                    (orderType == null || order.getOrderType() == orderType)
                    )
                    .toList();

        } else {

            orders = orderRepository.findAll();
        }

        return orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::mapToAdminResponse)
                .toList();
    }

    @Override
    public AdminOrderResponse getOrderById(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        return mapToAdminResponse(order);
    }

    @Override
    @Transactional
    public AdminOrderResponse updateOrderStatus(Long id, String newStatus) {

        // Row lock: two admins / admin + kitchen clicking at the same moment
        // are processed one after the other, never both.
        Order order = orderRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus requestedStatus = parseStatus(newStatus);

        BusinessType businessType = cafeSettingService.getBusinessType();

        if (!OrderStatusTransitionPolicy.isAllowed(
                businessType,
                OrderStatusTransitionPolicy.Actor.ADMIN,
                currentStatus,
                requestedStatus)) {

            throw new ConflictException(
                    "INVALID_STATUS_TRANSITION",
                    "Invalid order status transition: "
                            + currentStatus + " -> " + requestedStatus
            );
        }

        // Cash must be collected before the order can be closed
        if (requestedStatus == OrderStatus.COMPLETED
                && order.getPaymentMethod() == PaymentMethod.CASH
                && order.getPaymentStatus() != OrderPaymentStatus.PAID) {
            throw new ConflictException(
                    "CASH_NOT_COLLECTED",
                    "Collect cash and mark this order as paid before completing it."
            );
        }

        // Online paid orders: no cancel, no refund
        if (requestedStatus == OrderStatus.CANCELLED
                && currentStatus != OrderStatus.PAYMENT_PENDING
                && !(order.getPaymentMethod() == PaymentMethod.CASH
                && order.getPaymentStatus() != OrderPaymentStatus.PAID)) {
            throw new ConflictException(
                    "CANCEL_NOT_ALLOWED",
                    "Only unpaid cash orders can be cancelled."
            );
        }

        order.setStatus(requestedStatus);

        if (requestedStatus == OrderStatus.PREPARING) {
            Integer preparationMinutes =
                    order.getEstimatedPreparationMinutes();

            if (preparationMinutes == null || preparationMinutes <= 0) {
                preparationMinutes = 15;
            }

            order.setEstimatedPreparationMinutes(preparationMinutes);
            order.setEstimatedReadyAt(
                    LocalDateTime.now().plusMinutes(preparationMinutes)
            );
            order.setLateNotificationSent(false);
        }

        Order savedOrder = orderRepository.save(order);

        orderNotificationService.sendOrderNotification(
                new OrderNotification(
                        savedOrder.getId(),
                        savedOrder.getOrderNumber(),
                        savedOrder.getStatus(),
                        OrderStatusMessages.forCustomer(savedOrder),
                        savedOrder.getOrderType(),
                        savedOrder.getTable() != null
                                ? savedOrder.getTable().getTableNumber()
                                : null
                )
        );

        orderNotificationService.sendStaffOrderUpdate(savedOrder);

        return mapToAdminResponse(savedOrder);
    }

    @Override
    public List<AdminOrderResponse> getActiveOrders() {
        // Unseen cancelled cash orders first, so staff can't miss them
        List<Order> list = new java.util.ArrayList<>(
                orderRepository.findByStatusAndPaymentMethodAndCancelSeenIsNull(OrderStatus.CANCELLED, PaymentMethod.CASH));
        list.addAll(orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.READY)));
        return list.stream().map(this::mapToAdminResponse).toList(); // kitchen: this::mapToResponse
    }

    private OrderStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("INVALID_ORDER_STATUS", "Invalid order status");
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("INVALID_ORDER_STATUS", "Invalid order status");
        }
    }

    private AdminOrderResponse mapToAdminResponse(Order order) {

        AdminOrderResponse response = new AdminOrderResponse();

        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderType(order.getOrderType());
        response.setNickname(order.getNickname());
        response.setCookingNote(order.getCookingNote());
        response.setMobileNumber(order.getMobileNumber());
        response.setTotalAmount(order.getTotalAmount());
        response.setSubtotalAmount(order.getSubtotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setCouponCode(order.getCouponCode());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setStatus(order.getStatus());
        response.setEstimatedPreparationMinutes(
                order.getEstimatedPreparationMinutes()
        );
        response.setEstimatedReadyAt(order.getEstimatedReadyAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setOrderPaymentStatus(order.getPaymentStatus());
        response.setPaidAt(order.getPaidAt());
        response.setPaidBy(order.getPaidBy());
        response.setCounterPaymentMode(order.getCounterPaymentMode());
        response.setPlacedBy(order.getPlacedBy());
        response.setRating(order.getRating());
        response.setFeedback(order.getFeedback());
        if (order.getTable() != null) {
            response.setTableNumber(
                    order.getTable().getTableNumber()
            );
        }

        List<OrderItemResponse> items = orderItemRepository
                .findByOrder(order)
                .stream()
                .map(this::mapItemToResponse)
                .toList();

        response.setItems(items);

        Payment payment = paymentRepository
                .findByOrder(order)
                .orElse(null);

        if (payment != null) {
            response.setPaymentStatus(
                    payment.getStatus().name()
            );
            response.setRazorpayOrderId(
                    payment.getRazorpayOrderId()
            );
            response.setRazorpayPaymentId(
                    payment.getRazorpayPaymentId()
            );
            response.setTransactionReference(
                    payment.getTransactionReference()
            );
        }

        return response;
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {

        OrderItemResponse response = new OrderItemResponse();

        response.setMenuItemId(item.getMenuItem().getId());
        response.setItemName(item.getItemName());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setSubtotal(item.getSubtotal());
        response.setAddonsText(item.getAddonsText());   // ← ye add karo
        response.setAddonsText(item.getAddonsText());
        response.setAddonsDetail(item.getAddonsDetail());   // ← nayi
        return response;
    }
}