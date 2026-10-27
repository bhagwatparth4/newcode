package com.cafe.backend.controller.order;

import com.cafe.backend.dto.order.OrderCreateRequest;
import com.cafe.backend.dto.order.OrderResponse;
import com.cafe.backend.service.order.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cafe.backend.dto.order.CustomerOrderStatusResponse;
import com.cafe.backend.dto.order.CustomerOrderDetailsResponse;
import jakarta.validation.Valid;
import com.cafe.backend.security.RateLimitService;

import java.time.Duration;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final RateLimitService rateLimitService;
    private final OrderService orderService;

    public OrderController(RateLimitService rateLimitService, OrderService orderService) {
        this.rateLimitService = rateLimitService;
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();

        boolean allowed = rateLimitService.isAllowed(
                "rate-limit:create-order:" + clientIp,
                30, // many customers share the cafe Wi-Fi (same IP)
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            return ResponseEntity.status(429).build();
        }

        // One phone number cannot flood orders, even from many networks
        String mobile = request.getMobileNumber() == null ? "" : request.getMobileNumber().trim();
        if (!rateLimitService.isAllowed("rate-limit:create-order-mobile:" + mobile, 10, Duration.ofMinutes(10))) {
            return ResponseEntity.status(429).build();
        }

        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<OrderResponse> getOrderById(
//            @PathVariable Long id) {
//
//        OrderResponse response = orderService.getOrderById(id);
//
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/customer/status/{notificationToken}")
    public ResponseEntity<CustomerOrderStatusResponse> getCustomerOrderStatus(
            @PathVariable String notificationToken) {

        CustomerOrderStatusResponse response =
                orderService.getCustomerOrderStatus(notificationToken);

        return ResponseEntity.ok(response);
    }



    @GetMapping("/customer/details/{notificationToken}")
    public ResponseEntity<CustomerOrderDetailsResponse> getCustomerOrderDetails(
            @PathVariable String notificationToken) {

        CustomerOrderDetailsResponse response =
                orderService.getCustomerOrderDetails(notificationToken);

        return ResponseEntity.ok(response);
    }

}
