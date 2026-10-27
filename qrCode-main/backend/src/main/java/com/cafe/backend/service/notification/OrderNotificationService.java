package com.cafe.backend.service.notification;

import com.cafe.backend.dto.notification.OrderNotification;
import com.cafe.backend.entity.Notification;
import com.cafe.backend.entity.Order;

import java.util.List;

public interface OrderNotificationService {

    void sendOrderNotification(OrderNotification notification);

    List<Notification> getOrderNotifications(Long orderId);

    List<Notification> getUnreadNotifications();

    void markAsRead(Long notificationId);

    public void sendNewOrderNotification(Order order);

    // Realtime refresh signal for Admin/Kitchen dashboards after a status change
    void sendStaffOrderUpdate(Order order);
}