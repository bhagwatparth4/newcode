package com.cafe.backend.controller.customer; // CustomerCashController jaisa package

import com.cafe.backend.entity.Order;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/orders") // same public prefix as customer cancel
public class FeedbackController {

    private final OrderRepository orderRepository;

    public FeedbackController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // One rating per order, only after it is completed
    @PatchMapping("/customer/feedback/{token}")
    @Transactional
    public Map<String, String> feedback(@PathVariable String token, @RequestBody Map<String, Object> body) {

        Order order = orderRepository.findByCustomerNotificationToken(token)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new ConflictException("NOT_COMPLETED", "You can rate your order once it is served.");
        }
        if (order.getRating() != null) {
            throw new ConflictException("ALREADY_RATED", "Thanks! You have already rated this order.");
        }

        int rating;
        try {
            rating = Integer.parseInt(String.valueOf(body.get("rating")));
        } catch (NumberFormatException e) {
            throw new BadRequestException("INVALID_RATING", "Please choose 1 to 5 stars.");
        }
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("INVALID_RATING", "Please choose 1 to 5 stars.");
        }

        String comment = body.get("comment") == null ? "" : String.valueOf(body.get("comment")).trim();
        if (comment.length() > 200) {
            comment = comment.substring(0, 200);
        }

        order.setRating(rating);
        order.setFeedback(comment.isEmpty() ? null : comment);
        order.setRatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return Map.of("status", "SAVED");
    }
}