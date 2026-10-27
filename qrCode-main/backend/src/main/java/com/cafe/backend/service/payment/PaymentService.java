package com.cafe.backend.service.payment;

import com.cafe.backend.dto.payment.PaymentVerificationResponse;
import com.cafe.backend.dto.payment.RazorpayOrderRequest;
import com.cafe.backend.dto.payment.RazorpayOrderResponse;
import com.cafe.backend.dto.payment.RazorpayPaymentVerificationRequest;
import com.cafe.backend.entity.Order;

public interface PaymentService {

    RazorpayOrderResponse createRazorpayOrder(
            RazorpayOrderRequest request
    );

    PaymentVerificationResponse verifyPayment(
            RazorpayPaymentVerificationRequest request
    );

    void markOrderAsPaid(Order order);
}