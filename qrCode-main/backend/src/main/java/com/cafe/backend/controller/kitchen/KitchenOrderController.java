package com.cafe.backend.controller.kitchen;

import com.cafe.backend.dto.admin.AdminOrderResponse;
import com.cafe.backend.service.kitchen.KitchenOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kitchen/orders")
public class KitchenOrderController {

    private final KitchenOrderService kitchenOrderService;

    public KitchenOrderController(KitchenOrderService kitchenOrderService) {
        this.kitchenOrderService = kitchenOrderService;
    }

    @GetMapping
    public ResponseEntity<List<AdminOrderResponse>> getActiveOrders() {
        return ResponseEntity.ok(
                kitchenOrderService.getActiveOrders()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderResponse> getOrderById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                kitchenOrderService.getOrderById(id)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(
                kitchenOrderService.updateOrderStatus(id, status)
        );
    }
}