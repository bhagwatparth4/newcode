package com.cafe.backend.exception;

public class CouponUsageLimitException extends RuntimeException {

    public CouponUsageLimitException(String message) {
        super(message);
    }
}