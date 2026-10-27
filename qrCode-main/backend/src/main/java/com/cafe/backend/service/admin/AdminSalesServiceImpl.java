package com.cafe.backend.service.admin;

import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.dto.admin.*;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cafe.backend.enums.PaymentStatus;
import com.cafe.backend.repository.PaymentRepository;
import com.cafe.backend.repository.OrderItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminSalesServiceImpl implements AdminSalesService {

    private final OrderRepository orderRepository;

    private final PaymentRepository paymentRepository;

    private final OrderItemRepository orderItemRepository;

    private static final LocalDateTime BEGINNING = LocalDateTime.of(2000, 1, 1, 0, 0);

    private static BigDecimal plus(BigDecimal a, BigDecimal b) {
        return (a == null ? BigDecimal.ZERO : a).add(b == null ? BigDecimal.ZERO : b);
    }

    public AdminSalesServiceImpl(OrderRepository orderRepository, PaymentRepository paymentRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public AdminSalesSummaryResponse getSalesSummary() {

        AdminSalesSummaryResponse response =
                new AdminSalesSummaryResponse();

        LocalDateTime startOfToday =
                LocalDate.now().atStartOfDay();

        LocalDateTime startOfTomorrow =
                LocalDate.now().plusDays(1).atStartOfDay();

        PaymentStatus success = PaymentStatus.SUCCESS;

        response.setTotalOrders(
                orderRepository.count()
        );

        response.setPaidOrders(
                paymentRepository.countByStatus(success)
                        + orderRepository.countCashCollected(BEGINNING, startOfTomorrow)
        );

        response.setCompletedOrders(
                orderRepository.countByStatus(OrderStatus.COMPLETED)
        );

        // Today's orders = orders that were actually paid today
        // (abandoned checkouts in PAYMENT_PENDING are not real orders)
        response.setTodayOrders(
                orderRepository.countByStatusAndCreatedAtBetween(
                        OrderStatus.PAID,
                        startOfToday,
                        startOfTomorrow
                )
                        + orderRepository.countByStatusAndCreatedAtBetween(
                        OrderStatus.PREPARING,
                        startOfToday,
                        startOfTomorrow
                )
                        + orderRepository.countByStatusAndCreatedAtBetween(
                        OrderStatus.READY,
                        startOfToday,
                        startOfTomorrow
                )
                        + orderRepository.countByStatusAndCreatedAtBetween(
                        OrderStatus.COMPLETED,
                        startOfToday,
                        startOfTomorrow
                )
        );

        response.setTodayCompletedOrders(
                orderRepository.countByStatusAndCreatedAtBetween(
                        OrderStatus.COMPLETED,
                        startOfToday,
                        startOfTomorrow
                )
        );

        response.setTotalRevenue(plus(
                paymentRepository.sumAmountByStatus(success),
                orderRepository.sumCashCollected(BEGINNING, startOfTomorrow)
        ));

        response.setTodayRevenue(plus(
                paymentRepository.sumAmountByStatusAndCreatedAtBetween(success, startOfToday, startOfTomorrow),
                orderRepository.sumCashCollected(startOfToday, startOfTomorrow)
        ));

        return response;
    }

    @Override
    public List<DailySalesResponse> getDailySales(
            LocalDate from,
            LocalDate to) {

        validateRange(from, to);

        List<DailySalesResponse> result = new ArrayList<>();

        LocalDate current = from;

        while (!current.isAfter(to)) {

            LocalDateTime startOfDay = current.atStartOfDay();
            LocalDateTime startOfNextDay = current.plusDays(1).atStartOfDay();

            long orders = orderRepository.countByCreatedAtBetween(
                    startOfDay,
                    startOfNextDay.minusNanos(1)
            );

            BigDecimal revenue = paymentRepository.sumAmountByStatusAndCreatedAtRange(
                    PaymentStatus.SUCCESS,
                    startOfDay,
                    startOfNextDay
            );

            revenue = plus(revenue, orderRepository.sumCashCollected(startOfDay, startOfNextDay));

            result.add(
                    new DailySalesResponse(
                            current,
                            orders,
                            revenue
                    )
            );

            current = current.plusDays(1);
        }

        return result;
    }

    @Override
    public List<TopSellingItemResponse> getTopSellingItems(
            LocalDate from,
            LocalDate to) {

        validateRange(from, to);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = orderItemRepository.findTopSellingItems(
                start,
                end
        );

        List<TopSellingItemResponse> result = new ArrayList<>();

        for (Object[] row : rows) {
            String itemName = (String) row[0];

            long quantitySold = ((Number) row[1]).longValue();

            BigDecimal revenue = row[2] instanceof BigDecimal
                    ? (BigDecimal) row[2]
                    : BigDecimal.valueOf(((Number) row[2]).doubleValue());

            result.add(
                    new TopSellingItemResponse(
                            itemName,
                            quantitySold,
                            revenue
                    )
            );
        }

        return result;
    }

    @Override
    public List<DietarySalesResponse> getDietarySales(
            LocalDate from,
            LocalDate to) {

        validateRange(from, to);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = orderItemRepository.findDietarySales(
                start,
                end
        );

        List<DietarySalesResponse> result = new ArrayList<>();

        for (Object[] row : rows) {

            String dietaryType = row[0].toString();

            long quantitySold = ((Number) row[1]).longValue();

            BigDecimal revenue = row[2] instanceof BigDecimal
                    ? (BigDecimal) row[2]
                    : BigDecimal.valueOf(((Number) row[2]).doubleValue());

            result.add(
                    new DietarySalesResponse(
                            dietaryType,
                            quantitySold,
                            revenue
                    )
            );
        }

        return result;
    }

    @Override
    public List<OrderTypeSalesResponse> getOrderTypeSales(
            LocalDate from,
            LocalDate to) {

        validateRange(from, to);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<OrderTypeSalesResponse> result = new ArrayList<>();

        for (OrderType orderType : OrderType.values()) {

            long orders = orderRepository.countByOrderTypeAndCreatedAtBetween(
                    orderType,
                    start,
                    end
            );

            BigDecimal revenue =
                    paymentRepository.sumAmountByOrderTypeAndStatusAndCreatedAtRange(
                            orderType,
                            PaymentStatus.SUCCESS,
                            start,
                            end
                    );

            revenue = plus(revenue, orderRepository.sumCashCollectedByType(orderType, start, end));

            result.add(
                    new OrderTypeSalesResponse(
                            orderType.name(),
                            orders,
                            revenue
                    )
            );
        }

        return result;
    }

    // Protects the database from accidental huge report ranges
    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("INVALID_DATE_RANGE", "Please choose a valid date range");
        }
        if (from.plusDays(366).isBefore(to)) {
            throw new BadRequestException("DATE_RANGE_TOO_LARGE", "Please choose a range of one year or less");
        }
    }
}
