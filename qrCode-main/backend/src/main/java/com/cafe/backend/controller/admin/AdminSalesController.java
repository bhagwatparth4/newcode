package com.cafe.backend.controller.admin;

import org.springframework.format.annotation.DateTimeFormat;
import com.cafe.backend.dto.admin.*;
import com.cafe.backend.service.admin.AdminSalesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/sales")
public class AdminSalesController {

    private final AdminSalesService adminSalesService;

    public AdminSalesController(AdminSalesService adminSalesService) {
        this.adminSalesService = adminSalesService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminSalesSummaryResponse> getSalesSummary() {
        return ResponseEntity.ok(
                adminSalesService.getSalesSummary()
        );
    }

    @GetMapping("/daily")
    public ResponseEntity<List<DailySalesResponse>> getDailySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                adminSalesService.getDailySales(from, to)
        );
    }

    @GetMapping("/top-items")
    public ResponseEntity<List<TopSellingItemResponse>> getTopSellingItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                adminSalesService.getTopSellingItems(from, to)
        );
    }

    @GetMapping("/dietary")
    public ResponseEntity<List<DietarySalesResponse>> getDietarySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                adminSalesService.getDietarySales(from, to)
        );
    }

    @GetMapping("/order-types")
    public ResponseEntity<List<OrderTypeSalesResponse>> getOrderTypeSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                adminSalesService.getOrderTypeSales(from, to)
        );
    }
}