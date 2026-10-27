package com.cafe.backend.service.payment;

import com.cafe.backend.service.order.OrderService;
import com.cafe.backend.dto.notification.OrderNotification;
import com.cafe.backend.dto.order.CustomerOrderDetailsResponse;
import com.cafe.backend.dto.order.OrderItemResponse;
import com.cafe.backend.dto.payment.PaymentVerificationResponse;
import com.cafe.backend.dto.payment.RazorpayOrderRequest;
import com.cafe.backend.dto.payment.RazorpayOrderResponse;
import com.cafe.backend.dto.payment.RazorpayPaymentVerificationRequest;
import com.cafe.backend.entity.Coupon;
import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.Payment;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.PaymentStatus;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.CouponUsageLimitException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.CouponRepository;
import com.cafe.backend.repository.OrderItemRepository;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.cafe.backend.service.notification.OrderNotificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;

    private final OrderNotificationService orderNotificationService;

    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    public PaymentServiceImpl(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CouponRepository couponRepository,
            OrderNotificationService orderNotificationService, OrderItemRepository orderItemRepository,
            OrderService orderService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
        this.orderNotificationService = orderNotificationService;
        this.orderItemRepository = orderItemRepository;
        this.orderService = orderService;
    }

    @Transactional
    @Override
    public RazorpayOrderResponse createRazorpayOrder(
            RazorpayOrderRequest request
    ) {

        Order order = orderRepository.findLockedById(request.getOrderId())
                .orElseThrow(() ->
                        new NotFoundException("ORDER_NOT_FOUND", "Order not found")
                );

        if (!request.getNotificationToken()
                .equals(order.getCustomerNotificationToken())) {

            throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new ConflictException(
                    "PAYMENT_NOT_ALLOWED",
                    "Payment cannot be initiated for this order"
            );
        }

        Payment existingPayment = paymentRepository
                .findByOrder(order)
                .orElse(null);

        /*
         * If a Razorpay order already exists for this cafe order,
         * return the existing Razorpay order.
         *
         * This keeps refresh/retry safe and prevents duplicate
         * Razorpay orders for the same cafe order.
         */
        if (existingPayment != null) {

            if (existingPayment.getStatus() == PaymentStatus.SUCCESS) {
                throw new ConflictException(
                        "PAYMENT_ALREADY_COMPLETED",
                        "Payment already completed for this order"
                );
            }

            RazorpayOrderResponse response =
                    new RazorpayOrderResponse();

            response.setOrderId(order.getId());
            response.setOrderNumber(order.getOrderNumber());
            response.setRazorpayOrderId(
                    existingPayment.getRazorpayOrderId()
            );
            response.setAmount(order.getTotalAmount());
            response.setCurrency("INR");
            response.setKeyId(razorpayKeyId);

            return response;
        }

        /*
         * Coupon reservation protection
         *
         * Lock the coupon row before checking its usage.
         * This serializes concurrent checkout attempts using
         * the same coupon.
         */
        String couponCode = order.getCouponCode();

        if (couponCode != null && !couponCode.isBlank()) {

            couponCode = couponCode.trim().toUpperCase();

            Coupon coupon = couponRepository
                    .findByCodeForUpdate(couponCode)
                    .orElseThrow(() ->
                            new BadRequestException("INVALID_COUPON", "Coupon not found")
                    );

            /*
             * A PAYMENT_PENDING order created recently is treated
             * as an active coupon reservation.
             *
             * After 20 minutes, an abandoned payment attempt is
             * no longer counted as an active reservation.
             */
            LocalDateTime reservationCutoff =
                    LocalDateTime.now().minusMinutes(20);

            long pendingReservations =
                    orderRepository.countActiveCouponReservations(
                            couponCode,
                            OrderStatus.PAYMENT_PENDING,
                            reservationCutoff,
                            order.getId()
                    );


            /*
             * Do not allow a new Razorpay order if all coupon
             * usages are already consumed or reserved.
             */
            if (coupon.getUsedCount() + pendingReservations
                    >= coupon.getUsageLimit()) {

                throw new CouponUsageLimitException(
                        "Coupon usage limit reached. Please try another coupon."
                );
            }
        }

        try {

            RazorpayClient razorpayClient =
                    new RazorpayClient(
                            razorpayKeyId,
                            razorpayKeySecret
                    );

            long amountInPaise = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValueExact();

            JSONObject razorpayOrderRequest =
                    new JSONObject();

            razorpayOrderRequest.put(
                    "amount",
                    amountInPaise
            );

            razorpayOrderRequest.put(
                    "currency",
                    "INR"
            );

            razorpayOrderRequest.put(
                    "receipt",
                    order.getOrderNumber()
            );

            com.razorpay.Order razorpayOrder =
                    razorpayClient.orders.create(
                            razorpayOrderRequest
                    );

            String razorpayOrderId =
                    razorpayOrder.get("id");

            Payment payment = new Payment();

            payment.setOrder(order);
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(PaymentStatus.CREATED);

            paymentRepository.save(payment);

            RazorpayOrderResponse response =
                    new RazorpayOrderResponse();

            response.setOrderId(order.getId());
            response.setOrderNumber(order.getOrderNumber());
            response.setRazorpayOrderId(razorpayOrderId);
            response.setAmount(order.getTotalAmount());
            response.setCurrency("INR");
            response.setKeyId(razorpayKeyId);

            return response;

        } catch (RazorpayException e) {

            throw new RuntimeException(
                    "Failed to create Razorpay order: "
                            + e.getMessage(),
                    e
            );
        }
    }

    @Transactional
    @Override
    public PaymentVerificationResponse verifyPayment(
            RazorpayPaymentVerificationRequest request
    ) {

        Order order = orderRepository.findLockedById(
                        request.getOrderId()
                )
                .orElseThrow(() ->
                        new NotFoundException("ORDER_NOT_FOUND", "Order not found")
                );

        if (!request.getNotificationToken()
                .equals(order.getCustomerNotificationToken())) {

            throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() ->
                        new NotFoundException(
                                "PAYMENT_NOT_FOUND",
                                "Payment record not found"
                        )
                );

        // Prevent already completed payment from being processed again
        if (payment.getStatus() == PaymentStatus.SUCCESS
                || order.getStatus() == OrderStatus.PAID) {

            PaymentVerificationResponse response =
                    new PaymentVerificationResponse();

            response.setSuccess(true);
            response.setOrderId(order.getId());
            response.setOrderNumber(order.getOrderNumber());
            response.setMessage("Payment already verified");

            return response;
        }

        // Verify that the Razorpay order returned by Checkout
        // belongs to the Razorpay order created by our backend
        if (!payment.getRazorpayOrderId()
                .equals(request.getRazorpayOrderId())) {

            throw new BadRequestException(
                    "PAYMENT_VERIFICATION_FAILED",
                    "Razorpay Order ID mismatch"
            );
        }

        Payment existingPaymentForRazorpayId =
                paymentRepository.findByRazorpayPaymentId(
                                request.getRazorpayPaymentId()
                        )
                        .orElse(null);

        if (existingPaymentForRazorpayId != null
                && !existingPaymentForRazorpayId
                .getOrder()
                .getId()
                .equals(order.getId())) {

            throw new ConflictException(
                    "PAYMENT_ALREADY_USED",
                    "Razorpay Payment ID is already associated "
                            + "with another order"
            );
        }

        try {

            RazorpayClient razorpayClient =
                    new RazorpayClient(
                            razorpayKeyId,
                            razorpayKeySecret
                    );

            com.razorpay.Payment razorpayPayment =
                    razorpayClient.payments.fetch(
                            request.getRazorpayPaymentId()
                    );

            String razorpayPaymentStatus =
                    razorpayPayment.get("status");

            if (!"captured".equalsIgnoreCase(
                    razorpayPaymentStatus
            )) {

                throw new BadRequestException(
                        "PAYMENT_NOT_CAPTURED",
                        "Razorpay payment is not captured. "
                                + "Status: "
                                + razorpayPaymentStatus
                );
            }

            long razorpayAmountInPaise =
                    ((Number) razorpayPayment
                            .get("amount"))
                            .longValue();

            long expectedAmountInPaise =
                    order.getTotalAmount()
                            .multiply(
                                    BigDecimal.valueOf(100)
                            )
                            .longValueExact();

            if (razorpayAmountInPaise
                    != expectedAmountInPaise) {

                throw new BadRequestException(
                        "PAYMENT_VERIFICATION_FAILED",
                        "Payment amount mismatch. Expected: "
                                + expectedAmountInPaise
                                + " paise, Received: "
                                + razorpayAmountInPaise
                                + " paise"
                );
            }

            JSONObject options = new JSONObject();

            // Use the Razorpay Order ID stored in our database.
            // Do NOT trust the Checkout value for signature generation.
            options.put(
                    "razorpay_order_id",
                    payment.getRazorpayOrderId()
            );

            options.put(
                    "razorpay_payment_id",
                    request.getRazorpayPaymentId()
            );

            options.put(
                    "razorpay_signature",
                    request.getRazorpaySignature()
            );

            boolean signatureValid =
                    Utils.verifyPaymentSignature(
                            options,
                            razorpayKeySecret
                    );

            if (!signatureValid) {
                throw new BadRequestException(
                        "PAYMENT_VERIFICATION_FAILED",
                        "Invalid Razorpay signature"
                );
            }

            // Store successful payment information
            payment.setRazorpayPaymentId(
                    request.getRazorpayPaymentId()
            );

            payment.setRazorpaySignature(
                    request.getRazorpaySignature()
            );

            // UPI RRN / bank reference (shown on receipts and in Excel)
            payment.setTransactionReference(
                    RazorpayPaymentDetails.extractTransactionReference(
                            razorpayPayment.get("acquirer_data")
                    )
            );

            payment.setStatus(PaymentStatus.SUCCESS);

            paymentRepository.save(payment);

            // Consume coupon only after successful payment
            consumeCoupon(order);

            // Also notifies the customer + the correct dashboard (Admin or Kitchen)
            markOrderAsPaid(order);

            PaymentVerificationResponse response = new PaymentVerificationResponse();
            response.setSuccess(true);
            response.setOrderId(order.getId());
            response.setOrderNumber(order.getOrderNumber());
            response.setMessage("Payment verified successfully");

            response.setOrderDetails(
                    orderService.getCustomerOrderDetails(
                            order.getCustomerNotificationToken()
                    )
            );

            return response;

        } catch (RazorpayException e) {

            throw new RuntimeException(
                    "Payment verification failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    @Override
    @Transactional
    public void markOrderAsPaid(Order order) {

        // Idempotent: frontend verification and the webhook can both call
        // this; only the first one (while still PAYMENT_PENDING) does anything.
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }

        order.setStatus(OrderStatus.PAID);

        // The estimate starts when the cafe actually receives the paid order,
        // not when the customer first opened the checkout.
        Integer minutes = order.getEstimatedPreparationMinutes();
        if (minutes == null || minutes <= 0) {
            minutes = 15;
            order.setEstimatedPreparationMinutes(minutes);
        }
        order.setEstimatedReadyAt(LocalDateTime.now().plusMinutes(minutes));

        orderRepository.save(order);

        // Customer tracking page (WebSocket / Web Push)
        orderNotificationService.sendOrderNotification(
                new OrderNotification(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus(),
                        "Payment received successfully. Your order is confirmed.",
                        order.getOrderType(),
                        order.getTable() != null
                                ? order.getTable().getTableNumber()
                                : null
                )
        );

        // "Order Received!" + bell on Admin (SMALL_CAFE) or Kitchen (LARGE_CAFE / RESTAURANT)
        orderNotificationService.sendNewOrderNotification(order);
    }

    private void consumeCoupon(Order order) {

        // No coupon used on this order
        if (order.getCouponCode() == null
                || order.getCouponCode().isBlank()) {
            return;
        }

        String couponCode = order.getCouponCode()
                .trim()
                .toUpperCase();

        Coupon coupon =
                couponRepository.findByCodeForUpdate(couponCode)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coupon not found"
                                )
                        );

        // Prevent usage beyond the configured limit
        // Money is already captured at this point. Never fail the order
        // here, otherwise the customer pays but the order is never PAID.
        // Limit is enforced earlier (order creation + payment reservation).
        if (coupon.getUsedCount()
                >= coupon.getUsageLimit()) {

            System.out.println(
                    "COUPON LIMIT OVERSHOT (payment already captured): "
                            + couponCode
            );
        }

        // Consume exactly one usage
        coupon.setUsedCount(
                coupon.getUsedCount() + 1
        );

        couponRepository.save(coupon);
    }
}