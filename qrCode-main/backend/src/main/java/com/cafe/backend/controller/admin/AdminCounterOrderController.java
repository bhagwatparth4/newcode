package com.cafe.backend.controller.admin;

import com.cafe.backend.dto.order.OrderCreateRequest;
import com.cafe.backend.dto.order.OrderItemRequest;
import com.cafe.backend.dto.order.OrderResponse;
import com.cafe.backend.entity.CafeTable;
import com.cafe.backend.entity.CounterQr;
import com.cafe.backend.entity.Order;
import com.cafe.backend.enums.PaymentMethod;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.repository.CafeTableRepository;
import com.cafe.backend.repository.CounterQrRepository;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.service.order.CashPaymentService;
import com.cafe.backend.service.order.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// Orders created by the owner at the counter / over the phone. Handled as CASH everywhere;
// payMode (CASH / UPI / CARD) is only a label for the owner.
@RestController
@RequestMapping("/api/admin/orders")
public class AdminCounterOrderController {

    public record CounterOrderRequest(Long tableId, String nickname, String mobile, String cookingNote,
                                      String payMode, boolean paidNow, String idempotencyKey,
                                      List<OrderItemRequest> items, String takenBy) {}

    private final OrderService orderService;
    private final CashPaymentService cashPaymentService;
    private final CafeTableRepository cafeTableRepository;
    private final CounterQrRepository counterQrRepository;
    private final OrderRepository orderRepository;

    public AdminCounterOrderController(OrderService orderService, CashPaymentService cashPaymentService,
                                       CafeTableRepository cafeTableRepository, CounterQrRepository counterQrRepository,
                                       OrderRepository orderRepository) {
        this.orderService = orderService;
        this.cashPaymentService = cashPaymentService;
        this.cafeTableRepository = cafeTableRepository;
        this.counterQrRepository = counterQrRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/counter")
    @Transactional
    public OrderResponse create(@RequestBody CounterOrderRequest body, Authentication auth) {

        String qrToken = body.tableId() != null
                ? cafeTableRepository.findById(body.tableId()).filter(CafeTable::isActive)
                .orElseThrow(() -> new BadRequestException("INVALID_TABLE", "Choose a valid table.")).getQrToken()
                : counterQrRepository.findAll().stream().filter(CounterQr::isActive).findFirst()
                .orElseThrow(() -> new BadRequestException("NO_COUNTER_QR", "Create a counter QR in Tables & QR first.")).getQrToken();

        String payMode = List.of("CASH", "UPI", "CARD").contains(body.payMode()) ? body.payMode() : "CASH";

        // Worker who took the order (typed on the counter screen); falls back to the logged-in admin
        String takenBy = body.takenBy() == null || body.takenBy().isBlank()
                ? auth.getName()
                : body.takenBy().trim().substring(0, Math.min(100, body.takenBy().trim().length()));

        OrderCreateRequest req = new OrderCreateRequest();
        req.setStaffOrder(true);
        req.setQrToken(qrToken);
        req.setNickname(body.nickname() == null || body.nickname().isBlank() ? "Walk-in" : body.nickname().trim());
        req.setMobileNumber(body.mobile() == null ? "" : body.mobile().trim());
        req.setCookingNote(body.cookingNote());
        req.setIdempotencyKey(body.idempotencyKey());
        req.setItems(body.items());
        req.setPaymentMethod(PaymentMethod.CASH);

        OrderResponse response = orderService.createOrder(req);

        Order order = orderRepository.findById(response.getId()).orElseThrow();
        order.setCounterPaymentMode(payMode);
        order.setPlacedBy(takenBy);
        orderRepository.save(order);

        if (body.paidNow()) {
            cashPaymentService.markCashPaid(order.getId(), takenBy);
        }
        return response;
    }


}