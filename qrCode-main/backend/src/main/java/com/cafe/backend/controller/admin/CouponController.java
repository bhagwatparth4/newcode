package com.cafe.backend.controller.admin;

import com.cafe.backend.dto.coupon.CouponValidationRequest;
import com.cafe.backend.dto.coupon.CouponValidationResponse;
import com.cafe.backend.dto.coupon.CreateCouponRequest;
import com.cafe.backend.entity.Coupon;
import com.cafe.backend.service.admin.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    public ResponseEntity<Coupon> createCoupon(
            @RequestBody CreateCouponRequest request
    ) {
        return ResponseEntity.ok(
                couponService.createCoupon(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(
                couponService.getAllCoupons()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Coupon> updateCoupon(
            @PathVariable Long id,
            @RequestBody CreateCouponRequest request
    ) {
        return ResponseEntity.ok(
                couponService.updateCoupon(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Coupon> updateCouponStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                couponService.updateCouponStatus(id, active)
        );
    }


    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestBody CouponValidationRequest request
    ) {
        return ResponseEntity.ok(
                couponService.validateCoupon(request)
        );
    }
}