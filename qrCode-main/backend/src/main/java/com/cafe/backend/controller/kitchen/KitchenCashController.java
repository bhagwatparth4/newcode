package com.cafe.backend.controller.kitchen;


import com.cafe.backend.service.order.CashPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kitchen/orders")
public class KitchenCashController {

    private final CashPaymentService cashPaymentService;

    public KitchenCashController(CashPaymentService cashPaymentService) {
        this.cashPaymentService = cashPaymentService;
    }

    @PatchMapping("/{id}/cancel-seen")
    public ResponseEntity<Void> cancelSeen(@PathVariable Long id) {
        cashPaymentService.markCancelSeen(id);
        return ResponseEntity.noContent().build();
    }



}
