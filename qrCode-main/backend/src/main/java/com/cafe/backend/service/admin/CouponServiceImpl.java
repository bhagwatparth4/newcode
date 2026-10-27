package com.cafe.backend.service.admin;

import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.dto.coupon.CreateCouponRequest;
import com.cafe.backend.entity.Coupon;
import com.cafe.backend.enums.DiscountType;
import com.cafe.backend.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cafe.backend.dto.coupon.CouponValidationRequest;
import com.cafe.backend.dto.coupon.CouponValidationResponse;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    public CouponServiceImpl(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional
    public Coupon createCoupon(CreateCouponRequest request) {

        if (request == null || request.getCode() == null) {
            throw new BadRequestException("COUPON_CODE_REQUIRED", "Coupon code is required");
        }

        String code = request.getCode().trim().toUpperCase();

        if (code.isBlank()) {
            throw new BadRequestException("COUPON_CODE_REQUIRED", "Coupon code is required");
        }

        if (couponRepository.existsByCode(code)) {
            throw new ConflictException("COUPON_EXISTS", "Coupon code already exists");
        }

        validateCouponRequest(request);

        Coupon coupon = new Coupon();

        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setMaximumDiscountAmount(
                request.getMaximumDiscountAmount()
        );
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsedCount(0);
        coupon.setActive(true);

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    @Transactional
    public Coupon updateCoupon(
            Long id,
            CreateCouponRequest request
    ) {

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("COUPON_NOT_FOUND", "Coupon not found")
                );

        if (request == null || request.getCode() == null) {
            throw new BadRequestException("COUPON_CODE_REQUIRED", "Coupon code is required");
        }

        String code = request.getCode().trim().toUpperCase();

        if (!coupon.getCode().equals(code)
                && couponRepository.existsByCode(code)) {
            throw new ConflictException("COUPON_EXISTS", "Coupon code already exists");
        }

        validateCouponRequest(request);

        coupon.setCode(code);
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(
                request.getMinimumOrderAmount()
        );
        coupon.setMaximumDiscountAmount(
                request.getMaximumDiscountAmount()
        );
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setUsageLimit(request.getUsageLimit());

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public Coupon updateCouponStatus(
            Long id,
            boolean active
    ) {

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("COUPON_NOT_FOUND", "Coupon not found")
                );

        coupon.setActive(active);

        return couponRepository.save(coupon);
    }


    @Override
    public CouponValidationResponse validateCoupon(CouponValidationRequest request) {

        CouponValidationResponse response = new CouponValidationResponse();

        if (request == null || request.getCode() == null || request.getCode().isBlank()) {
            response.setValid(false);
            response.setMessage("Coupon code is required");
            return response;
        }

        if (request.getOrderAmount() == null || request.getOrderAmount().compareTo(BigDecimal.ZERO) <= 0) {
            response.setValid(false);
            response.setMessage("Order amount must be greater than zero");
            return response;
        }

        if (request == null || request.getCode() == null) {
            throw new BadRequestException("COUPON_CODE_REQUIRED", "Coupon code is required");
        }

        String code = request.getCode().trim().toUpperCase();

        Coupon coupon = couponRepository.findByCode(code).orElse(null);

        if (coupon == null) {
            response.setValid(false);
            response.setMessage("Invalid coupon code");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();

        if (!coupon.isActive()) {
            response.setValid(false);
            response.setMessage("Coupon is inactive");
            return response;
        }

        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            response.setValid(false);
            response.setMessage("Coupon has expired or is not yet active");
            return response;
        }

        if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
            response.setValid(false);
            response.setMessage("Coupon usage limit reached");
            return response;
        }

        if (coupon.getMinimumOrderAmount() != null
                && request.getOrderAmount().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            response.setValid(false);
            response.setMessage(
                    "Minimum order amount is ₹" + coupon.getMinimumOrderAmount()
            );
            return response;
        }

        BigDecimal discountAmount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {

            discountAmount = request.getOrderAmount()
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (coupon.getMaximumDiscountAmount() != null
                    && discountAmount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                discountAmount = coupon.getMaximumDiscountAmount();
            }

        } else {
            discountAmount = coupon.getDiscountValue();

            if (discountAmount.compareTo(request.getOrderAmount()) > 0) {
                discountAmount = request.getOrderAmount();
            }
        }

        // Same rule as order creation: payment must be at least ₹1,
        // so the preview matches exactly what the customer will pay.
        BigDecimal maxDiscount = request.getOrderAmount()
                .subtract(BigDecimal.ONE)
                .max(BigDecimal.ZERO);

        if (discountAmount.compareTo(maxDiscount) > 0) {
            discountAmount = maxDiscount;
        }

        BigDecimal finalAmount = request.getOrderAmount()
                .subtract(discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        response.setValid(true);
        response.setMessage("Coupon applied successfully");
        response.setCouponCode(code);
        response.setOrderAmount(request.getOrderAmount());
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(finalAmount);

        return response;
    }

    private void validateCouponRequest(
            CreateCouponRequest request
    ) {

        if (request.getDiscountType() == null) {
            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Discount type is required"
            );
        }

        if (request.getDiscountValue() == null
                || request.getDiscountValue()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Discount value must be greater than zero"
            );
        }

        if (request.getDiscountType()
                .name()
                .equals("PERCENTAGE")
                && request.getDiscountValue()
                .compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Percentage discount cannot exceed 100"
            );
        }

        if (request.getValidFrom() == null
                || request.getValidUntil() == null) {

            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Coupon validity dates are required"
            );
        }

        if (!request.getValidUntil()
                .isAfter(request.getValidFrom())) {

            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Valid until must be after valid from"
            );
        }

        if (request.getUsageLimit() == null
                || request.getUsageLimit() <= 0) {

            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Usage limit must be greater than zero"
            );
        }

        if (request.getMinimumOrderAmount() != null
                && request.getMinimumOrderAmount()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Minimum order amount cannot be negative"
            );
        }

        if (request.getMaximumDiscountAmount() != null
                && request.getMaximumDiscountAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new BadRequestException(
                    "INVALID_COUPON_REQUEST",
                    "Maximum discount amount must be greater than zero"
            );
        }
    }
}