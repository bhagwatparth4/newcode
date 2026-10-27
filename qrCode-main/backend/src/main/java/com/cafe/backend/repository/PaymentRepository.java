package com.cafe.backend.repository;

import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.Payment;
import com.cafe.backend.enums.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import com.cafe.backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    long countByStatus(PaymentStatus status);

    long countByStatusAndCreatedAtBetween(
            PaymentStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status = :status
        """)
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payment p
        WHERE p.status = :status
        AND p.createdAt BETWEEN :from AND :to
        """)
    BigDecimal sumAmountByStatusAndCreatedAtBetween(
            @Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
    SELECT COALESCE(SUM(p.amount), 0)
    FROM Payment p
    WHERE p.status = :status
    AND p.createdAt >= :from
    AND p.createdAt < :to
""")
    BigDecimal sumAmountByStatusAndCreatedAtRange(
            @Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
    SELECT COALESCE(SUM(p.amount), 0)
    FROM Payment p
    JOIN p.order o
    WHERE o.orderType = :orderType
      AND p.status = :status
      AND p.createdAt >= :from
      AND p.createdAt < :to
""")
    BigDecimal sumAmountByOrderTypeAndStatusAndCreatedAtRange(
            @Param("orderType") OrderType orderType,
            @Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}