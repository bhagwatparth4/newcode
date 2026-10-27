package com.cafe.backend.service.notification;

import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.dto.notification.PushSubscriptionRequest;
import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.PushSubscription;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.repository.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PushSubscriptionServiceImpl
        implements PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final OrderRepository orderRepository;

    public PushSubscriptionServiceImpl(
            PushSubscriptionRepository pushSubscriptionRepository,
            OrderRepository orderRepository) {

        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void subscribe(PushSubscriptionRequest request) {

        if (request.getOrderId() == null) {
            throw new BadRequestException("INVALID_SUBSCRIPTION", "Order ID is required");
        }

        if (request.getEndpoint() == null || request.getEndpoint().isBlank()) {
            throw new BadRequestException("INVALID_SUBSCRIPTION", "Push endpoint is required");
        }

        if (request.getP256dh() == null || request.getP256dh().isBlank()) {
            throw new BadRequestException("INVALID_SUBSCRIPTION", "P256DH key is required");
        }

        if (request.getAuth() == null || request.getAuth().isBlank()) {
            throw new BadRequestException("INVALID_SUBSCRIPTION", "Auth key is required");
        }

        if (request.getEndpoint().length() > 2000
                || request.getP256dh().length() > 500
                || request.getAuth().length() > 500) {
            throw new BadRequestException("INVALID_SUBSCRIPTION", "Invalid push subscription");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (request.getNotificationToken() == null
                || !request.getNotificationToken().equals(order.getCustomerNotificationToken())) {
            throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
        }

        PushSubscription subscription =
                pushSubscriptionRepository
                        .findByEndpoint(request.getEndpoint())
                        .orElseGet(PushSubscription::new);

        subscription.setOrder(order);
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dhKey(request.getP256dh());
        subscription.setAuthKey(request.getAuth());

        pushSubscriptionRepository.save(subscription);
    }
}