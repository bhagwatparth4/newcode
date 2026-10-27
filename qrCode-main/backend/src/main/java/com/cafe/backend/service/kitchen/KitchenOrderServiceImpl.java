package com.cafe.backend.service.kitchen;

import com.cafe.backend.dto.admin.AdminOrderResponse;
import com.cafe.backend.dto.notification.OrderNotification;
import com.cafe.backend.dto.order.OrderItemResponse;
import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.OrderItem;
import com.cafe.backend.entity.Payment;
import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.repository.PaymentRepository;
import com.cafe.backend.service.admin.AdminOrderServiceImpl;
import com.cafe.backend.service.order.OrderStatusMessages;
import com.cafe.backend.service.order.OrderStatusTransitionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cafe.backend.service.notification.OrderNotificationService;
import com.cafe.backend.enums.PaymentMethod;
import java.time.LocalDateTime;
import java.util.List;

import com.cafe.backend.service.admin.CafeSettingService;
import com.cafe.backend.repository.OrderItemRepository;

@Service
@Transactional(readOnly = true)
public class KitchenOrderServiceImpl implements KitchenOrderService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CafeSettingService cafeSettingService;
    private final OrderNotificationService orderNotificationService;
    private final OrderItemRepository orderItemRepository;

    public KitchenOrderServiceImpl(AdminOrderServiceImpl adminOrderService, PaymentRepository paymentRepository, OrderRepository orderRepository, CafeSettingService cafeSettingService, OrderNotificationService orderNotificationService, OrderItemRepository orderItemRepository) {
        this.paymentRepository = paymentRepository;

        this.orderRepository = orderRepository;
        this.cafeSettingService = cafeSettingService;
        this.orderNotificationService = orderNotificationService;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public List<AdminOrderResponse> getActiveOrders() {

        ensureKitchenEnabled();

        // New (PAID) and in-progress (PREPARING) orders, oldest first
        // Unseen cancelled cash orders first, so staff can't miss them
        List<Order> list = new java.util.ArrayList<>(
                orderRepository.findByStatusAndPaymentMethodAndCancelSeenIsNull(OrderStatus.CANCELLED, PaymentMethod.CASH));
        list.addAll(orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(OrderStatus.PAID, OrderStatus.PREPARING /* admin: , OrderStatus.READY */)));
        return list.stream().map(this::mapToAdminResponse).toList(); // kitchen: this::mapToResponse
    }

    @Override
    public AdminOrderResponse getOrderById(Long id) {

        ensureKitchenEnabled();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        return mapToResponse(order);
    }

    @Override
    @Transactional
    public AdminOrderResponse updateOrderStatus(Long id, String newStatus) {

        BusinessType businessType = ensureKitchenEnabled();

        // Row lock so two kitchen tablets cannot move the same order twice
        Order order = orderRepository.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus requestedStatus;

        try {
            requestedStatus = OrderStatus.valueOf(
                    newStatus.trim().toUpperCase()
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("INVALID_ORDER_STATUS", "Invalid order status");
        }

        // Kitchen may only do PAID -> PREPARING -> READY. Never COMPLETED.
        if (!OrderStatusTransitionPolicy.isAllowed(
                businessType,
                OrderStatusTransitionPolicy.Actor.KITCHEN,
                currentStatus,
                requestedStatus)) {

            throw new ConflictException(
                    "INVALID_STATUS_TRANSITION",
                    "Kitchen cannot perform transition: "
                            + currentStatus + " -> " + requestedStatus
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

        // Admin board (and other kitchen tablets) refresh in realtime
        orderNotificationService.sendStaffOrderUpdate(savedOrder);

        return mapToResponse(savedOrder);
    }

    private BusinessType ensureKitchenEnabled() {

        BusinessType businessType = cafeSettingService.getBusinessType();

        if (businessType == BusinessType.SMALL_CAFE) {
            throw new ConflictException(
                    "KITCHEN_DISABLED",
                    "Kitchen dashboard is not enabled for SMALL_CAFE"
            );
        }

        return businessType;
    }

    /*
     * Kitchen gets only what it needs to cook: no customer mobile number,
     * no payment IDs. (Admin uses AdminOrderServiceImpl for full details.)
     */
    private AdminOrderResponse mapToResponse(Order order) {

        AdminOrderResponse response = new AdminOrderResponse();

        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setOrderType(order.getOrderType());
        response.setNickname(order.getNickname());
        response.setCookingNote(order.getCookingNote());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setEstimatedPreparationMinutes(
                order.getEstimatedPreparationMinutes()
        );
        response.setEstimatedReadyAt(order.getEstimatedReadyAt());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getTable() != null) {
            response.setTableNumber(
                    order.getTable().getTableNumber()
            );
        }

        List<OrderItemResponse> items = orderItemRepository
                .findByOrder(order)
                .stream()
                .map(item -> {
                    OrderItemResponse itemResponse =
                            new OrderItemResponse();

                    itemResponse.setMenuItemId(
                            item.getMenuItem().getId()
                    );
                    itemResponse.setItemName(
                            item.getItemName()
                    );
                    itemResponse.setQuantity(
                            item.getQuantity()
                    );
                    itemResponse.setUnitPrice(
                            item.getUnitPrice()
                    );
                    itemResponse.setSubtotal(
                            item.getSubtotal()
                    );
                    itemResponse.setAddonsText(item.getAddonsText());   // ← ye add karo

                    itemResponse.setAddonsText(item.getAddonsText());
                    itemResponse.setAddonsDetail(item.getAddonsDetail());   // ← nayi

                    return itemResponse;
                })
                .toList();

        response.setItems(items);

        return response;
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

        return response;
    }
}
