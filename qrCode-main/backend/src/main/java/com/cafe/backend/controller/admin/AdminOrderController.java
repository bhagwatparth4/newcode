package com.cafe.backend.controller.admin;

import org.springframework.format.annotation.DateTimeFormat;
import com.cafe.backend.dto.admin.AdminOrderResponse;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.service.admin.AdminOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public ResponseEntity<List<AdminOrderResponse>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) OrderType orderType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        return ResponseEntity.ok(
                adminOrderService.getAllOrders(
                        status,
                        orderType,
                        from,
                        to
                )
        );
    }

    // Live board: all PAID / PREPARING / READY orders, oldest first
    @GetMapping("/active")
    public ResponseEntity<List<AdminOrderResponse>> getActiveOrders() {
        return ResponseEntity.ok(adminOrderService.getActiveOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderResponse> getOrderById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                adminOrderService.getOrderById(id)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(
                adminOrderService.updateOrderStatus(id, status)
        );
    }
}