package com.cafe.backend.service.payment;

import com.cafe.backend.exception.BadRequestException;
import java.math.BigDecimal;

import com.cafe.backend.entity.Coupon;
import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.Payment;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.PaymentStatus;
import com.cafe.backend.repository.CouponRepository;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.repository.PaymentRepository;
import com.cafe.backend.repository.ProcessedWebhookEventRepository;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final CouponRepository couponRepository;

    private final PaymentService paymentService;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public PaymentWebhookServiceImpl(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ProcessedWebhookEventRepository processedWebhookEventRepository,
            CouponRepository couponRepository,
            PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.processedWebhookEventRepository =
                processedWebhookEventRepository;
        this.couponRepository = couponRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    @Override
    public void processWebhook(
            String payload,
            String signature,
            String eventId
    ) {

        if (signature == null || signature.isBlank()) {
            throw new BadRequestException(
                    "WEBHOOK_SIGNATURE_MISSING",
                    "Missing Razorpay webhook signature"
            );
        }

        if (payload == null || payload.isBlank()) {
            throw new BadRequestException(
                    "WEBHOOK_PAYLOAD_EMPTY",
                    "Empty webhook payload"
            );
        }

        if (eventId == null || eventId.isBlank()) {
            throw new BadRequestException(
                    "WEBHOOK_EVENT_ID_MISSING",
                    "Missing Razorpay webhook event ID"
            );
        }

        try {

            // 1. Verify webhook signature first
            boolean signatureValid =
                    Utils.verifyWebhookSignature(
                            payload,
                            signature,
                            webhookSecret
                    );

            if (!signatureValid) {
                throw new BadRequestException(
                        "WEBHOOK_SIGNATURE_INVALID",
                        "Invalid Razorpay webhook signature"
                );
            }

            // 2. Ignore duplicate webhook events
            if (processedWebhookEventRepository
                    .existsByEventId(eventId)) {

                System.out.println(
                        "Duplicate Razorpay webhook ignored: "
                                + eventId
                );

                return;
            }

            JSONObject webhook = new JSONObject(payload);

            String event = webhook.getString("event");

            System.out.println(
                    "Razorpay webhook event: " + event
            );

            // 3. Handle successful captured payment
            if ("payment.captured".equals(event)) {

                JSONObject paymentEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("payment")
                                .getJSONObject("entity");

                String razorpayPaymentId =
                        paymentEntity.getString("id");

                String razorpayOrderId =
                        paymentEntity.getString("order_id");

                BigDecimal razorpayAmount =
                        BigDecimal.valueOf(
                                paymentEntity.getLong("amount")
                        ).divide(BigDecimal.valueOf(100));

                Payment payment =
                        paymentRepository
                                .findByRazorpayOrderId(razorpayOrderId)
                                .orElse(null);

                if (payment == null) {

                    // Not a payment created by this cafe app (for example a
                    // payment link on the same Razorpay account). Acknowledge
                    // it so Razorpay does not keep retrying forever.
                    System.out.println(
                            "Webhook ignored: no cafe payment for Razorpay Order ID "
                                    + razorpayOrderId
                    );

                } else {

                    // Lock the order row. The frontend /verify call takes the
                    // same lock, so the two can never run at the same time.
                    Order order =
                            orderRepository
                                    .findLockedById(
                                            payment.getOrder().getId()
                                    )
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "Order not found for payment"
                                            )
                                    );

                    if (payment.getAmount() == null
                            || payment.getAmount()
                            .compareTo(razorpayAmount) != 0) {

                        // Never mark an order PAID for the wrong amount.
                        // Retrying cannot fix this, so log loudly instead.
                        System.out.println(
                                "ALERT: webhook amount mismatch for order "
                                        + order.getOrderNumber()
                                        + ". Expected: " + payment.getAmount()
                                        + ", Received: " + razorpayAmount
                        );

                    } else if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {

                        // 4. Only a still-unpaid order needs work. If /verify
                        //    already confirmed it, we change nothing (so stored
                        //    signature/reference are never overwritten and the
                        //    coupon is never consumed twice).
                        payment.setRazorpayPaymentId(
                                razorpayPaymentId
                        );

                        String reference =
                                RazorpayPaymentDetails.extractTransactionReference(
                                        paymentEntity.opt("acquirer_data")
                                );

                        if (reference != null) {
                            payment.setTransactionReference(reference);
                        }

                        payment.setStatus(
                                PaymentStatus.SUCCESS
                        );

                        paymentRepository.save(payment);

                        // 5. Consume coupon exactly once
                        consumeCoupon(order);

                        // 6. Mark order as PAID (+ notify customer and dashboard)
                        paymentService.markOrderAsPaid(order);

                    } else if (order.getStatus() == OrderStatus.CANCELLED
                            && payment.getStatus() != PaymentStatus.SUCCESS) {

                        // Money captured for an order that no longer exists -> owner must refund from Razorpay
                        System.out.println("ALERT: payment captured for CANCELLED order "
                                + order.getOrderNumber() + " (" + razorpayPaymentId + "). Refund from Razorpay dashboard.");
                        payment.setRazorpayPaymentId(razorpayPaymentId);
                        payment.setStatus(PaymentStatus.SUCCESS);
                        paymentRepository.save(payment);
                    }
                }
            }

            // 7. Handle failed payment

            // 7. Handle failed payment
            else if ("payment.failed".equals(event)) {

                JSONObject paymentEntity =
                        webhook
                                .getJSONObject("payload")
                                .getJSONObject("payment")
                                .getJSONObject("entity");

                String razorpayOrderId =
                        paymentEntity.getString("order_id");

                Payment payment =
                        paymentRepository
                                .findByRazorpayOrderId(razorpayOrderId)
                                .orElse(null);

                if (payment != null) {

                    Order order = orderRepository
                            .findLockedById(
                                    payment.getOrder().getId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Order not found for failed payment"
                                    )
                            );

                    if (payment.getStatus()
                            != PaymentStatus.SUCCESS) {

                        payment.setStatus(
                                PaymentStatus.FAILED
                        );

                        paymentRepository.save(payment);
                    }
                }
            }

            // 8. Save event ID only after successful processing
            com.cafe.backend.entity.ProcessedWebhookEvent
                    processedEvent =
                    new com.cafe.backend.entity.ProcessedWebhookEvent();

            processedEvent.setEventId(eventId);

            processedEvent.setProcessedAt(
                    java.time.LocalDateTime.now()
            );

            processedWebhookEventRepository.save(
                    processedEvent
            );

        } catch (BadRequestException e) {

            // Invalid signature etc. -> HTTP 400 (do not hide it as a 500)
            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Webhook processing failed: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Consume coupon only after successful payment.
     *
     * Uses a database row lock so concurrent successful
     * payments cannot incorrectly exceed the coupon usage limit.
     */
    private void consumeCoupon(Order order) {

        // No coupon used on this order
        if (order.getCouponCode() == null
                || order.getCouponCode().isBlank()) {

            return;
        }

        String couponCode =
                order.getCouponCode()
                        .trim()
                        .toUpperCase();

        Coupon coupon =
                couponRepository
                        .findByCodeForUpdate(couponCode)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coupon not found"
                                )
                        );

        /*
         * IMPORTANT:
         * If the payment was already marked SUCCESS before this
         * webhook arrived, do not consume the coupon again.
         *
         * The webhook itself is idempotent at the event level,
         * but the same payment can also be processed by the
         * frontend verification endpoint.
         */
        // Anything past PAYMENT_PENDING (PAID, PREPARING, READY, COMPLETED)
        // was already processed, so the coupon was already consumed.
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }

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

        coupon.setUsedCount(
                coupon.getUsedCount() + 1
        );

        couponRepository.save(coupon);
    }
}