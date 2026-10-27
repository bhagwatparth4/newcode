package com.cafe.backend.controller.menu; // menu controller jaisa package

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/menu") // CHECK: menu controller jaisa hi public prefix
public class PaymentOptionsController {
    private final com.cafe.backend.service.admin.CafeSettingService cafeSettingService;

    public PaymentOptionsController(com.cafe.backend.service.admin.CafeSettingService cafeSettingService) {
        this.cafeSettingService = cafeSettingService;
    }
    @GetMapping("/payment-options")
    public Map<String, Object> paymentOptions() {
        return Map.of(
                "cashEnabled", cafeSettingService.isCashEnabled(),
                "businessType", cafeSettingService.getBusinessType().name()
        );
    }
}