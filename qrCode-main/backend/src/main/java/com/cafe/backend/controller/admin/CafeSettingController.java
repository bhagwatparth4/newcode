package com.cafe.backend.controller.admin;

import com.cafe.backend.service.admin.CafeSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class CafeSettingController {

    private final CafeSettingService cafeSettingService;

    public CafeSettingController(CafeSettingService cafeSettingService) {
        this.cafeSettingService = cafeSettingService;
    }

    @GetMapping("/preparation-time")
    public ResponseEntity<Integer> getPreparationTime() {
        return ResponseEntity.ok(
                cafeSettingService.getDefaultPreparationMinutes()
        );
    }

    @PutMapping("/preparation-time")
    public ResponseEntity<Integer> updatePreparationTime(
            @RequestBody Map<String, Integer> request
    ) {
        Integer minutes = request.get("minutes");

        return ResponseEntity.ok(
                cafeSettingService.updateDefaultPreparationMinutes(minutes)
        );
    }

    /*
     * Read-only. Lets the Admin dashboard know whether this cafe runs
     * with a Kitchen (LARGE_CAFE / RESTAURANT) or Admin only (SMALL_CAFE).
     * There is intentionally NO endpoint to change it.
     */
    @GetMapping("/business-type")
    public ResponseEntity<Map<String, String>> getBusinessType() {
        return ResponseEntity.ok(
                Map.of("businessType", cafeSettingService.getBusinessType().name())
        );
    }


    @PutMapping("/accepting-orders")
    public ResponseEntity<Map<String, Boolean>> setAcceptingOrders(@RequestBody Map<String, Boolean> body) {
        boolean accepting = Boolean.TRUE.equals(body.get("acceptingOrders"));
        return ResponseEntity.ok(Map.of("acceptingOrders", cafeSettingService.setAcceptingOrders(accepting)));
    }

    @GetMapping("/cash-enabled")
    public ResponseEntity<Map<String, Boolean>> getCashEnabled() {
        return ResponseEntity.ok(Map.of("cashEnabled", cafeSettingService.isCashEnabled()));
    }

    @PutMapping("/cash-enabled")
    public ResponseEntity<Map<String, Boolean>> setCashEnabled(@RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("cashEnabled"));
        return ResponseEntity.ok(Map.of("cashEnabled", cafeSettingService.setCashEnabled(enabled)));
    }
}