package com.cafe.backend.service.notification;

import org.springframework.beans.factory.annotation.Value;
import com.cafe.backend.repository.PushSubscriptionRepository;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.entity.PushSubscription;
import com.cafe.backend.dto.notification.NewOrderNotification;
import com.cafe.backend.dto.notification.OrderNotification;
import com.cafe.backend.entity.Notification;
import com.cafe.backend.entity.Order;
import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.repository.NotificationRepository;
import com.cafe.backend.repository.OrderRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.cafe.backend.service.admin.CafeSettingService;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderNotificationServiceImpl
        implements OrderNotificationService {


    private final CafeSettingService cafeSettingService;

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushService webPushService;

    // Shown as the title of browser (Web Push) notifications
    @Value("${app.cafe-name:Rush Hours}")
    private String cafeName;

    public OrderNotificationServiceImpl(
            CafeSettingService cafeSettingService, SimpMessagingTemplate messagingTemplate,
            NotificationRepository notificationRepository,
            OrderRepository orderRepository,
            PushSubscriptionRepository pushSubscriptionRepository,
            WebPushService webPushService
    ) {
        this.cafeSettingService = cafeSettingService;
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.orderRepository = orderRepository;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.webPushService = webPushService;
    }

    @Override
    @Transactional
    public void sendOrderNotification(
            OrderNotification notification
    ) {

        /*
         * 1. Save notification in database
         *    so notification history is preserved.
         */
        Order order = orderRepository.findById(
                notification.getOrderId()
        ).orElseThrow(() ->
                new RuntimeException(
                        "Order not found for notification"
                )
        );

        Notification savedNotification =
                new Notification();

        savedNotification.setOrder(order);

        savedNotification.setType(
                notification.getOrderStatus() != null
                        ? notification.getOrderStatus().name()
                        : "ORDER_UPDATE"
        );

        savedNotification.setTitle(
                "Order Update"
        );

        savedNotification.setMessage(
                notification.getMessage()
        );

        savedNotification.setRead(false);

        /*
         * Notification is being delivered through WebSocket.
         */
        savedNotification.setDeliveredAt(
                LocalDateTime.now()
        );

        notificationRepository.save(
                savedNotification
        );

        /*
         * 2. Send real-time WebSocket notification
         *
         * Existing destination is preserved.
         */
        String destination =
                "/topic/customer/orders/" + order.getCustomerNotificationToken();
        sendAfterCommit(destination, notification);

        /*
         * 3. Web Push (only reaches the customer when their page is closed
         *    and they allowed notifications). Optional: if VAPID keys are
         *    not configured, or the customer said no, this is skipped.
         */
        sendWebPushAfterCommit(order, notification);
    }

    /*
     * Tells Admin (and Kitchen, when the cafe has one) that an order's status
     * changed, so every open dashboard refreshes without polling.
     * This never plays the "new order" bell; that is only for NEW paid orders.
     */
    @Override
    public void sendStaffOrderUpdate(Order order) {

        OrderNotification update = new OrderNotification(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                "Order " + order.getOrderNumber() + " is now " + order.getStatus(),
                order.getOrderType(),
                order.getTable() != null ? order.getTable().getTableNumber() : null
        );

        sendAfterCommit("/topic/admin/order-updates", update);

        if (cafeSettingService.getBusinessType() != BusinessType.SMALL_CAFE) {
            sendAfterCommit("/topic/kitchen/order-updates", update);
        }
    }

    private void sendWebPushAfterCommit(Order order, OrderNotification notification) {

        if (!webPushService.isConfigured()) {
            return;
        }

        // Load subscriptions now (inside the transaction), send after commit
        List<PushSubscription> subscriptions =
                pushSubscriptionRepository.findByOrderId(order.getId());

        if (subscriptions.isEmpty()) {
            return;
        }

        String pushMessage = buildPushMessage(order, notification);

        Runnable send = () -> {
            for (PushSubscription subscription : subscriptions) {
                try {
                    webPushService.sendNotification(subscription, cafeName, pushMessage);
                } catch (Exception e) {
                    // A failed push must never break the order flow
                    System.out.println("Web Push failed for order "
                            + order.getOrderNumber() + ": " + e.getMessage());
                }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            send.run();
                        }
                    }
            );
        } else {
            send.run();
        }
    }

    /*
     * Context-specific text for TABLE vs COUNTER when the order is READY.
     * Every other update reuses the normal notification message.
     */
    private String buildPushMessage(Order order, OrderNotification notification) {

        if (notification.getOrderStatus() == OrderStatus.READY
                && notification.getMessage() != null
                && notification.getMessage().contains("is ready")) {

            if (order.getOrderType() == OrderType.TABLE && order.getTable() != null) {
                return "Your order for Table " + order.getTable().getTableNumber() + " is ready.";
            }

            return "Your order is ready. Please collect it from the counter.";
        }

        return notification.getMessage();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getOrderNotifications(
            Long orderId
    ) {

        return notificationRepository
                .findByOrderIdOrderByCreatedAtDesc(
                        orderId
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications() {

        return notificationRepository
                .findByReadFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {

        Notification notification =
                notificationRepository.findById(
                        notificationId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found"
                        )
                );

        notification.setRead(true);

        notificationRepository.save(
                notification
        );
    }

    @Override
    public void sendNewOrderNotification(Order order) {

        BusinessType businessType = cafeSettingService.getBusinessType();

        NewOrderNotification notification = new NewOrderNotification(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderType(),
                order.getTable() != null ? order.getTable().getTableNumber() : null,
                order.getNickname(),
                order.getTotalAmount()
        );

        // Admin always receives realtime new-order updates
        sendAfterCommit("/topic/admin/orders", notification);

        // Kitchen receives realtime new-order updates only
        // for LARGE_CAFE / RESTAURANT
        if (businessType == BusinessType.LARGE_CAFE
                || businessType == BusinessType.RESTAURANT) {

            sendAfterCommit("/topic/kitchen/orders", notification);
        }
    }

    /*
     * Send the WebSocket message only after the DB transaction commits.
     * Otherwise the dashboard may receive the event and re-fetch the
     * order before the new status is actually saved.
     */
    private void sendAfterCommit(String destination, Object payload) {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            messagingTemplate.convertAndSend(destination, payload);
                        }
                    }
            );

        } else {
            messagingTemplate.convertAndSend(destination, payload);
        }
    }
}