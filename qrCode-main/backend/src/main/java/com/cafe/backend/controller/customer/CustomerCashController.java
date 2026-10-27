package com.cafe.backend.controller.customer; // tracking controller jaisa package

import com.cafe.backend.service.order.CashPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders") // CHECK: tracking controller jaisa public prefix
public class CustomerCashController {

    private final CashPaymentService cashPaymentService;

    public CustomerCashController(CashPaymentService cashPaymentService) {
        this.cashPaymentService = cashPaymentService;
    }

    @PatchMapping("/customer/cancel/{token}")
    public ResponseEntity<Void> cancel(@PathVariable String token) {
        cashPaymentService.cancelByCustomer(token);
        return ResponseEntity.noContent().build();
    }
}