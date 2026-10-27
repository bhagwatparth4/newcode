package com.cafe.backend.repository;

import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.OrderItem;
import com.cafe.backend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

/*
 * Sales reports count every PAID order (PAID, PREPARING, READY, COMPLETED),
 * the same orders that are included in revenue. Unpaid/cancelled are excluded.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    @Query("""
    SELECT oi.itemName, SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice)
    FROM OrderItem oi
    JOIN oi.order o
    WHERE o.createdAt >= :from
      AND o.createdAt < :to
      AND o.status IN (
            com.cafe.backend.enums.OrderStatus.PAID,
            com.cafe.backend.enums.OrderStatus.PREPARING,
            com.cafe.backend.enums.OrderStatus.READY,
            com.cafe.backend.enums.OrderStatus.COMPLETED
      )
    GROUP BY oi.itemName
    ORDER BY SUM(oi.quantity) DESC
""")
    List<Object[]> findTopSellingItems(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );



    @Query("""
    SELECT mi.dietaryType,
           SUM(oi.quantity),
           SUM(oi.quantity * oi.unitPrice)
    FROM OrderItem oi
    JOIN oi.order o
    JOIN oi.menuItem mi
    WHERE o.createdAt >= :from
      AND o.createdAt < :to
      AND o.status IN (
            com.cafe.backend.enums.OrderStatus.PAID,
            com.cafe.backend.enums.OrderStatus.PREPARING,
            com.cafe.backend.enums.OrderStatus.READY,
            com.cafe.backend.enums.OrderStatus.COMPLETED
      )
    GROUP BY mi.dietaryType
    ORDER BY SUM(oi.quantity) DESC
""")
    List<Object[]> findDietarySales(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );



    @Query("select oi.menuItem.id from OrderItem oi " +
            "where oi.order.status in :statuses and oi.order.createdAt >= :from " +
            "group by oi.menuItem.id order by sum(oi.quantity) desc")
    List<Long> findTopSellingItemIds(@Param("statuses") List<OrderStatus> statuses,
                                     @Param("from") LocalDateTime from,
                                     Pageable pageable);
}