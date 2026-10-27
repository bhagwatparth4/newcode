package com.cafe.backend.controller.menu;

import com.cafe.backend.service.admin.CafeSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Public: tells the customer page whether the cafe is taking orders right now
@RestController
public class PublicCafeStatusController {

    private final CafeSettingService cafeSettingService;

    public PublicCafeStatusController(CafeSettingService cafeSettingService) {
        this.cafeSettingService = cafeSettingService;
    }

    @GetMapping("/api/menu/status")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("acceptingOrders", cafeSettingService.isAcceptingOrders()));
    }
}