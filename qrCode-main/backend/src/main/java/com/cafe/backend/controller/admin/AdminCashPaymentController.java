package com.cafe.backend.controller.admin; // apne folder ke hisaab se package theek karna

import com.cafe.backend.service.order.CashPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders") // CHECK: baaki admin controllers jaisa hi prefix
public class AdminCashPaymentController {

    private final CashPaymentService cashPaymentService;

    public AdminCashPaymentController(CashPaymentService cashPaymentService) {
        this.cashPaymentService = cashPaymentService;
    }

    @PatchMapping("/{id}/mark-paid")
    public ResponseEntity<Void> markPaid(@PathVariable Long id, Authentication auth) {
        cashPaymentService.markCashPaid(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel-cash")
    public ResponseEntity<Void> cancelCash(@PathVariable Long id, Authentication auth) {
        cashPaymentService.cancelCashOrder(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel-seen")
    public ResponseEntity<Void> cancelSeen(@PathVariable Long id) {
        cashPaymentService.markCancelSeen(id);
        return ResponseEntity.noContent().build();
    }
}