package com.cafe.backend.repository;

import java.util.Collection;
import com.cafe.backend.entity.Order;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import com.cafe.backend.enums.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    // Live boards (Admin + Kitchen): only the statuses asked for, oldest first
    List<Order> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findLockedById(Long id);

    List<Order> findByStatusAndEstimatedReadyAtBeforeAndLateNotificationSentFalse(
            OrderStatus status,
            LocalDateTime time
    );

    Optional<Order> findByCustomerNotificationToken(String customerNotificationToken);

    List<Order> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    List<Order> findByStatusAndCreatedAtBetween(
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Order> findByOrderTypeAndCreatedAtBetween(
            OrderType orderType,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Order> findByOrderTypeAndStatusAndCreatedAtBetween(
            OrderType orderType,
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    long countByStatus(OrderStatus status);

    long countByStatusAndCreatedAtBetween(
            OrderStatus status,
            LocalDateTime from,
            LocalDateTime to
    );
    long countByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    long countByOrderTypeAndCreatedAtBetween(
            OrderType orderType,
            LocalDateTime from,
            LocalDateTime to
    );


    @Query("""
    SELECT COUNT(o)
    FROM Order o
    WHERE o.couponCode = :couponCode
      AND o.status = :status
      AND o.createdAt > :cutoff
      AND o.id <> :orderId
""")
    long countActiveCouponReservations(
            @Param("couponCode") String couponCode,
            @Param("status") OrderStatus status,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("orderId") Long orderId
    );


    List<Order> findByStatusAndPaymentMethodAndCancelSeenIsNull(OrderStatus status, PaymentMethod paymentMethod);



    // Cash that has actually been collected (counter UPI/card included, as they are treated as cash)
    @Query("select coalesce(sum(o.totalAmount), 0) from Order o " +
            "where o.paymentMethod = com.cafe.backend.enums.PaymentMethod.CASH " +
            "and o.paymentStatus = com.cafe.backend.enums.OrderPaymentStatus.PAID " +
            "and o.status <> com.cafe.backend.enums.OrderStatus.CANCELLED " +
            "and o.createdAt >= :from and o.createdAt < :to")
    java.math.BigDecimal sumCashCollected(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o " +
            "where o.orderType = :type " +
            "and o.paymentMethod = com.cafe.backend.enums.PaymentMethod.CASH " +
            "and o.paymentStatus = com.cafe.backend.enums.OrderPaymentStatus.PAID " +
            "and o.status <> com.cafe.backend.enums.OrderStatus.CANCELLED " +
            "and o.createdAt >= :from and o.createdAt < :to")
    java.math.BigDecimal sumCashCollectedByType(@Param("type") OrderType type,
                                                @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select count(o) from Order o " +
            "where o.paymentMethod = com.cafe.backend.enums.PaymentMethod.CASH " +
            "and o.paymentStatus = com.cafe.backend.enums.OrderPaymentStatus.PAID " +
            "and o.status <> com.cafe.backend.enums.OrderStatus.CANCELLED " +
            "and o.createdAt >= :from and o.createdAt < :to")
    long countCashCollected(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);


    @Query("select count(o) from Order o where o.mobileNumber = :mobile " +
            "and o.paymentMethod = com.cafe.backend.enums.PaymentMethod.CASH " +
            "and o.paymentStatus = com.cafe.backend.enums.OrderPaymentStatus.PENDING " +
            "and o.status in (com.cafe.backend.enums.OrderStatus.PAID, com.cafe.backend.enums.OrderStatus.PREPARING, com.cafe.backend.enums.OrderStatus.READY)")
    long countOpenUnpaidCashByMobile(@Param("mobile") String mobile);

    @Query("select count(o) from Order o where o.table.id = :tableId " +
            "and o.paymentMethod = com.cafe.backend.enums.PaymentMethod.CASH " +
            "and o.paymentStatus = com.cafe.backend.enums.OrderPaymentStatus.PENDING " +
            "and o.status in (com.cafe.backend.enums.OrderStatus.PAID, com.cafe.backend.enums.OrderStatus.PREPARING, com.cafe.backend.enums.OrderStatus.READY)")
    long countOpenUnpaidCashByTable(@Param("tableId") Long tableId);
}