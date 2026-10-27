package com.cafe.backend.service.admin;

import com.cafe.backend.dto.coupon.CreateCouponRequest;
import com.cafe.backend.entity.Coupon;
import com.cafe.backend.dto.coupon.CouponValidationRequest;
import com.cafe.backend.dto.coupon.CouponValidationResponse;
import java.util.List;

public interface CouponService {

    Coupon createCoupon(CreateCouponRequest request);

    List<Coupon> getAllCoupons();

    Coupon updateCoupon(Long id, CreateCouponRequest request);

    Coupon updateCouponStatus(Long id, boolean active);

    CouponValidationResponse validateCoupon(
            CouponValidationRequest request
    );
}