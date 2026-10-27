package com.cafe.backend.service.order;

import com.cafe.backend.dto.order.*;
import com.cafe.backend.entity.*;
import com.cafe.backend.enums.DiscountType;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.CouponUsageLimitException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.repository.*;
import com.cafe.backend.service.admin.CafeSettingService;
import com.cafe.backend.service.notification.OrderNotificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cafe.backend.enums.PaymentMethod;
import com.cafe.backend.enums.OrderPaymentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final CouponRepository couponRepository;

    private final CafeTableRepository cafeTableRepository;
    private final CounterQrRepository counterQrRepository;
    private final MenuItemRepository menuItemRepository;

    private final CafeSettingService cafeSettingService;

    private final OrderNotificationService orderNotificationService;

    private final PaymentRepository paymentRepository;

    private final CustomerRepository customerRepository;

//    @Value("${app.cash-enabled:true}")
//    private boolean cashEnabled;



    public OrderServiceImpl(
            CafeTableRepository cafeTableRepository,
            CounterQrRepository counterQrRepository,
            MenuItemRepository menuItemRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository, CouponRepository couponRepository, CafeSettingService cafeSettingService, OrderNotificationService orderNotificationService, PaymentRepository paymentRepository, CustomerRepository customerRepository) {

        this.cafeTableRepository = cafeTableRepository;
        this.counterQrRepository = counterQrRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.couponRepository = couponRepository;
        this.cafeSettingService = cafeSettingService;
        this.orderNotificationService = orderNotificationService;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
    }


    @Transactional
    @Override
    public OrderResponse createOrder(OrderCreateRequest request) {

        // -----------------------------------
        // 0. Idempotency Check
        // -----------------------------------

        String idempotencyKey = request.getIdempotencyKey();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException(
                    "IDEMPOTENCY_KEY_REQUIRED",
                    "Idempotency key is required"
            );
        }

        Order existingOrder = orderRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingOrder != null) {
            System.out.println(
                    "DUPLICATE ORDER REQUEST → Returning existing order: "
                            + existingOrder.getOrderNumber()
            );

            return getOrderById(existingOrder.getId());
        }

        // Cafe closed by the admin -> no new orders (checked here on the server)
        if (!request.isStaffOrder() && !cafeSettingService.isAcceptingOrders()) {
            throw new ConflictException("CAFE_CLOSED", "Sorry, we're not taking orders right now.");
        }

        // Clean up customer text input once, before it is used anywhere
        request.setNickname(request.getNickname().trim());
        request.setMobileNumber(request.getMobileNumber().trim());

        if (!(request.isStaffOrder() && request.getMobileNumber().isEmpty())
                && PhoneNumberRules.looksFake(request.getMobileNumber())) {
            throw new BadRequestException(
                    "INVALID_MOBILE",
                    "Please enter your real 10-digit mobile number."
            );
        }

        // -----------------------------------
        // 1. Validate QR Token
        // -----------------------------------

        String qrToken = request.getQrToken();

        if (qrToken == null || qrToken.isBlank()) {
            throw new BadRequestException(
                    "QR_TOKEN_REQUIRED",
                    "QR token is required"
            );
        }

        CafeTable cafeTable = cafeTableRepository
                .findByQrToken(qrToken)
                .orElse(null);

        CounterQr counterQr = null;

        if (cafeTable != null) {

            if (!cafeTable.isActive()) {
                throw new ConflictException(
                        "TABLE_QR_INACTIVE",
                        "This table QR is currently inactive."
                );
            }

            System.out.println(
                    "TABLE ORDER → " + cafeTable.getTableNumber()
            );

        } else {

            counterQr = counterQrRepository
                    .findByQrToken(qrToken)
                    .orElse(null);

            if (counterQr != null) {

                if (!counterQr.isActive()) {
                    throw new ConflictException(
                            "COUNTER_QR_INACTIVE",
                            "This counter QR is currently inactive."
                    );
                }

                System.out.println("WALK_IN ORDER → COUNTER");

            } else {

                throw new NotFoundException(
                        "INVALID_QR_TOKEN",
                        "This QR code is invalid or no longer active."
                );
            }
        }

        // -----------------------------------
        // 2. Validate Order Items
        // -----------------------------------

        List<OrderItemRequest> items = request.getItems();

        if (items == null || items.isEmpty()) {
            throw new BadRequestException(
                    "EMPTY_ORDER",
                    "Your order must contain at least one item."
            );
        }

        // -----------------------------------
        // 3. Calculate Total From DB Prices
        // -----------------------------------

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : items) {

            if (itemRequest.getQuantity() <= 0) {
                throw new BadRequestException(
                        "INVALID_QUANTITY",
                        "Item quantity must be greater than zero."
                );
            }

            MenuItem menuItem = menuItemRepository
                    .findById(itemRequest.getMenuItemId())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "MENU_ITEM_NOT_FOUND",
                                    "Menu item not found: "
                                            + itemRequest.getMenuItemId()
                            )
                    );

            // Item must be available
            if (!menuItem.isAvailable()) {
                throw new ConflictException(
                        "MENU_ITEM_UNAVAILABLE",
                        "Sorry, this menu item is currently unavailable: "
                                + menuItem.getName()
                );
            }

            // Use price from DATABASE
            AddonPick pick = pickAddons(menuItem, itemRequest.getAddonIds());
            BigDecimal unitPrice = menuItem.getPrice().add(pick.perUnit());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            totalAmount = totalAmount.add(subtotal);

            System.out.println(
                    menuItem.getName()
                            + " × "
                            + itemRequest.getQuantity()
                            + " = ₹"
                            + subtotal
            );
        }

        System.out.println(
                "CALCULATED TOTAL → ₹" + totalAmount
        );

        if (request.getPaymentMethod() == PaymentMethod.CASH && !cafeSettingService.isCashEnabled() && !request.isStaffOrder()) {
            throw new ConflictException(
                    "CASH_DISABLED",
                    "Cash payment is not available. Please pay online."
            );
        }

        // -----------------------------------
// 3.5 Apply Coupon
// -----------------------------------
        // Coupons are only for online payment
        if (request.getPaymentMethod() == PaymentMethod.CASH
                && request.getCouponCode() != null
                && !request.getCouponCode().isBlank()) {
            throw new BadRequestException(
                    "COUPON_ONLINE_ONLY",
                    "Coupons are valid only on online payment."
            );
        }
        BigDecimal subtotalAmount = totalAmount;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String couponCode = request.getCouponCode();

        if (couponCode != null && !couponCode.isBlank()) {

            String normalizedCouponCode = couponCode.trim().toUpperCase();

            Coupon coupon = couponRepository
                    .findByCode(normalizedCouponCode)
                    .orElseThrow(() ->
                            new BadRequestException(
                                    "INVALID_COUPON",
                                    "Invalid coupon code."
                            )
                    );

            LocalDateTime now = LocalDateTime.now();

            if (!coupon.isActive()) {
                throw new BadRequestException(
                        "COUPON_INACTIVE",
                        "This coupon is currently inactive."
                );
            }

            if (now.isBefore(coupon.getValidFrom())
                    || now.isAfter(coupon.getValidUntil())) {
                throw new BadRequestException(
                        "COUPON_NOT_VALID",
                        "This coupon is expired or not yet active."
                );
            }

            if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new CouponUsageLimitException(
                        "Coupon usage limit reached. Please try another coupon."
                );
            }

            if (coupon.getMinimumOrderAmount() != null
                    && subtotalAmount.compareTo(
                    coupon.getMinimumOrderAmount()
            ) < 0) {
                throw new BadRequestException(
                        "MINIMUM_ORDER_NOT_MET",
                        "Minimum order amount is ₹"
                                + coupon.getMinimumOrderAmount()
                );
            }

            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {

                discountAmount = subtotalAmount
                        .multiply(coupon.getDiscountValue())
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

                if (coupon.getMaximumDiscountAmount() != null
                        && discountAmount.compareTo(
                        coupon.getMaximumDiscountAmount()
                ) > 0) {

                    discountAmount =
                            coupon.getMaximumDiscountAmount();
                }

            } else {

                discountAmount = coupon.getDiscountValue();

                if (discountAmount.compareTo(subtotalAmount) > 0) {
                    discountAmount = subtotalAmount;
                }
            }

            // Razorpay needs at least ₹1, so a coupon can never make the order free
            BigDecimal maxDiscount = subtotalAmount
                    .subtract(BigDecimal.ONE)
                    .max(BigDecimal.ZERO);

            if (discountAmount.compareTo(maxDiscount) > 0) {
                discountAmount = maxDiscount;
            }

            totalAmount = subtotalAmount
                    .subtract(discountAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            System.out.println(
                    "COUPON APPLIED → "
                            + normalizedCouponCode
                            + " | Discount ₹"
                            + discountAmount
                            + " | Final ₹"
                            + totalAmount
            );

            couponCode = normalizedCouponCode;
        }

// -----------------------------------
// 4. Create Order
// -----------------------------------

        Customer customer = request.getMobileNumber().isEmpty() ? null : customerRepository
                .findByMobileNumber(request.getMobileNumber())
                .orElseGet(() -> {
                    Customer newCustomer = new Customer();

                    newCustomer.setMobileNumber(request.getMobileNumber());
                    newCustomer.setName(request.getNickname());

                    newCustomer.setSmsOptIn(request.isSmsOptIn());
                    newCustomer.setWhatsappOptIn(request.isWhatsappOptIn());

                    return customerRepository.save(newCustomer);
                });


        Order order = new Order();


        String orderNumber = "ORD-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

        order.setOrderNumber(orderNumber);

        order.setCustomer(customer);

        if (cafeTable != null) {
            order.setOrderType(OrderType.TABLE);
            order.setCafeTable(cafeTable);
        } else {
            order.setOrderType(OrderType.WALK_IN);
            order.setCafeTable(null);
        }

        order.setNickname(request.getNickname());
        if (request.getCookingNote() != null && !request.getCookingNote().isBlank()) {
            order.setCookingNote(request.getCookingNote().trim());
        }
        order.setMobileNumber(request.getMobileNumber());
        order.setIdempotencyKey(request.getIdempotencyKey());//----------------------------------
        order.setSubtotalAmount(subtotalAmount);
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponCode);
        order.setTotalAmount(totalAmount);

        order.setStatus(OrderStatus.PAYMENT_PENDING);

        boolean isCash = request.getPaymentMethod() == PaymentMethod.CASH;

        if (isCash) {

            // Stop fake cash orders: limits only for customers, never for staff counter orders
            if (!request.isStaffOrder()) {
                if (orderRepository.countOpenUnpaidCashByMobile(request.getMobileNumber()) >= 3) {
                    throw new ConflictException("TOO_MANY_UNPAID",
                            "You already have unpaid orders. Please pay at the counter first.");
                }
                if (cafeTable != null && orderRepository.countOpenUnpaidCashByTable(cafeTable.getId()) >= 5) {
                    throw new ConflictException("TOO_MANY_UNPAID",
                            "This table has unpaid orders. Please ask the staff.");
                }
            }


            order.setPaymentMethod(PaymentMethod.CASH);
            order.setPaymentStatus(OrderPaymentStatus.PENDING);
            // Confirmed without Razorpay -> kitchen/admin treat it like a paid order
            order.setStatus(OrderStatus.PAID);
        }

// Initial preparation estimate
        Integer preparationMinutes =
                cafeSettingService.getDefaultPreparationMinutes();

        order.setEstimatedPreparationMinutes(preparationMinutes);
        order.setEstimatedReadyAt(
                LocalDateTime.now().plusMinutes(preparationMinutes)
        );

        Order savedOrder;

        try {
            savedOrder = orderRepository.save(order);
        } catch (DataIntegrityViolationException e) {

            // Same idempotency key was saved by a parallel request.
            // This transaction is already marked rollback-only, so we
            // cannot return the other order from here. The client simply
            // retries and the idempotency check at the top returns it.
            throw new ConflictException(
                    "ORDER_CREATION_CONFLICT",
                    "Your order is already being processed. Please retry."
            );
        }

// -----------------------------------
// 5. Create Order Items
// -----------------------------------

        for (OrderItemRequest itemRequest : items) {

            MenuItem menuItem = menuItemRepository
                    .findById(itemRequest.getMenuItemId())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "MENU_ITEM_NOT_FOUND",
                                    "Menu item not found: "
                                            + itemRequest.getMenuItemId()
                            )
                    );

            AddonPick pick = pickAddons(menuItem, itemRequest.getAddonIds());
            BigDecimal unitPrice = menuItem.getPrice().add(pick.perUnit());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(savedOrder);
            orderItem.setMenuItem(menuItem);

            // Snapshot values
            orderItem.setItemName(menuItem.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setAddonsText(pick.text());
            orderItem.setAddonsDetail(pick.detail());
            orderItem.setSubtotal(subtotal);

            orderItemRepository.save(orderItem);
        }

        System.out.println(
                "ORDER CREATED → " + savedOrder.getOrderNumber()
        );

        if (isCash) {

            // Same bell/notification that online orders get after payment
            orderNotificationService.sendNewOrderNotification(savedOrder);
        }

// -----------------------------------
// 6. Build Order Response
// -----------------------------------

        OrderResponse response = new OrderResponse();

        response.setId(savedOrder.getId());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setCustomerNotificationToken(savedOrder.getCustomerNotificationToken());
        response.setOrderType(savedOrder.getOrderType());

        if (savedOrder.getTable() != null) {
            response.setTableNumber(
                    savedOrder.getTable().getTableNumber()
            );
        } else {
            response.setTableNumber(null);
        }

        response.setNickname(savedOrder.getNickname());
        response.setMobileNumber(savedOrder.getMobileNumber());

        response.setSubtotalAmount(
                savedOrder.getSubtotalAmount()
        );

        response.setDiscountAmount(
                savedOrder.getDiscountAmount()
        );

        response.setCouponCode(
                savedOrder.getCouponCode()
        );

        response.setTotalAmount(
                savedOrder.getTotalAmount()
        );

        response.setStatus(savedOrder.getStatus());
        response.setPaymentMethod(savedOrder.getPaymentMethod());
        response.setOrderPaymentStatus(savedOrder.getPaymentStatus());
        response.setEstimatedPreparationMinutes(
                savedOrder.getEstimatedPreparationMinutes()
        );

        response.setEstimatedReadyAt(
                savedOrder.getEstimatedReadyAt()
        );

        response.setCreatedAt(savedOrder.getCreatedAt());

        List<OrderItemResponse> itemResponses = new ArrayList<>();

        for (OrderItemRequest itemRequest : items) {

            MenuItem menuItem = menuItemRepository
                    .findById(itemRequest.getMenuItemId())
                    .orElseThrow(() ->
                            new NotFoundException(
                                    "MENU_ITEM_NOT_FOUND",
                                    "Menu item not found: "
                                            + itemRequest.getMenuItemId()
                            )
                    );

            AddonPick pick = pickAddons(menuItem, itemRequest.getAddonIds());
            BigDecimal unitPrice = menuItem.getPrice().add(pick.perUnit());
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItemResponse itemResponse = new OrderItemResponse();

            itemResponse.setMenuItemId(menuItem.getId());
            itemResponse.setItemName(menuItem.getName());
            itemResponse.setQuantity(itemRequest.getQuantity());
            itemResponse.setUnitPrice(unitPrice);
            itemResponse.setAddonsText(pick.text());
            itemResponse.setAddonsDetail(pick.detail());


            itemResponse.setSubtotal(subtotal);

            itemResponses.add(itemResponse);
        }

        response.setItems(itemResponses);

        return response;
    }

    @Override
    public OrderResponse getOrderById(Long id) {

        // -----------------------------------
        // 1. Find Order
        // -----------------------------------

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "ORDER_NOT_FOUND",
                                "The order could not be found."
                        )
                );

        // -----------------------------------
        // 2. Build Order Response
        // -----------------------------------

        OrderResponse response = new OrderResponse();

        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerNotificationToken(order.getCustomerNotificationToken());
        response.setOrderType(order.getOrderType());

        if (order.getTable() != null) {
            response.setTableNumber(
                    order.getTable().getTableNumber()
            );
        } else {
            response.setTableNumber(null);
        }

        response.setNickname(order.getNickname());
        response.setMobileNumber(order.getMobileNumber());

        response.setSubtotalAmount(order.getSubtotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setCouponCode(order.getCouponCode());
        response.setTotalAmount(order.getTotalAmount());

        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setOrderPaymentStatus(order.getPaymentStatus());
        response.setEstimatedPreparationMinutes(
                order.getEstimatedPreparationMinutes()
        );

        response.setEstimatedReadyAt(
                order.getEstimatedReadyAt()
        );

        response.setCreatedAt(order.getCreatedAt());

        // -----------------------------------
        // 3. Fetch Order Items
        // -----------------------------------

        // Only this order's items (previously loaded the whole table)
        List<OrderItem> orderItems =
                orderItemRepository.findByOrder(order);

        List<OrderItemResponse> itemResponses =
                new ArrayList<>();

        for (OrderItem orderItem : orderItems) {

            OrderItemResponse itemResponse =
                    new OrderItemResponse();

            itemResponse.setMenuItemId(
                    orderItem.getMenuItem().getId()
            );

            itemResponse.setItemName(
                    orderItem.getItemName()
            );

            itemResponse.setQuantity(
                    orderItem.getQuantity()
            );

            itemResponse.setUnitPrice(
                    orderItem.getUnitPrice()
            );

            itemResponse.setSubtotal(
                    orderItem.getSubtotal()
            );
            itemResponse.setAddonsText(orderItem.getAddonsText());   // ← ye add karo

            itemResponse.setAddonsText(orderItem.getAddonsText());
            itemResponse.setAddonsDetail(orderItem.getAddonsDetail());   // ← nayi

            itemResponses.add(itemResponse);
        }

        response.setItems(itemResponses);

        return response;
    }


    @Override
    @Transactional(readOnly = true)
    public CustomerOrderStatusResponse getCustomerOrderStatus(String notificationToken) {

        Order order = orderRepository
                .findByCustomerNotificationToken(notificationToken)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Invalid notification token"));

        return new CustomerOrderStatusResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getOrderType().name(),
                order.getTable() != null
                        ? order.getTable().getTableNumber()
                        : null,
                order.getNickname(),
                order.getTotalAmount(),
                order.getEstimatedPreparationMinutes(),
                order.getEstimatedReadyAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderDetailsResponse getCustomerOrderDetails(
            String notificationToken) {

        Order order = orderRepository
                .findByCustomerNotificationToken(notificationToken)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Invalid notification token"));

        List<OrderItemResponse> items = new ArrayList<>();

        for (OrderItem item : orderItemRepository.findByOrder(order)) {

            OrderItemResponse itemResponse = new OrderItemResponse();

            itemResponse.setMenuItemId(item.getMenuItem().getId());
            itemResponse.setItemName(item.getItemName());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setUnitPrice(item.getUnitPrice());
            itemResponse.setSubtotal(item.getSubtotal());
            itemResponse.setAddonsText(item.getAddonsText());   // ← ye add karo
            itemResponse.setAddonsText(item.getAddonsText());
            itemResponse.setAddonsDetail(item.getAddonsDetail());   // ← nayi
            items.add(itemResponse);
        }

        Payment payment = paymentRepository
                .findByOrder(order)
                .orElse(null);

        String paymentStatus = null;
        String razorpayPaymentId = null;
        String transactionReference = null;

        if (payment != null) {
            paymentStatus = payment.getStatus().name();
            razorpayPaymentId = payment.getRazorpayPaymentId();
            transactionReference = payment.getTransactionReference();
        }

        CustomerOrderDetailsResponse response = new CustomerOrderDetailsResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderType(),
                order.getTable() != null
                        ? order.getTable().getTableNumber()
                        : null,
                order.getNickname(),
                items,
                order.getTotalAmount(),
                order.getStatus(),
                paymentStatus,
                razorpayPaymentId,
                transactionReference,
                order.getEstimatedPreparationMinutes(),
                order.getEstimatedReadyAt(),
                order.getCreatedAt()
        );

        // Extra fields for the customer receipt
        response.setSubtotalAmount(order.getSubtotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setCouponCode(order.getCouponCode());
        response.setUpdatedAt(order.getUpdatedAt());

        // Tracking links can be shared, so the customer view only shows
        // the last 4 digits of the mobile number (Admin sees the full number).
        response.setMaskedMobileNumber(maskMobile(order.getMobileNumber()));
        response.setCookingNote(order.getCookingNote());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setOrderPaymentStatus(order.getPaymentStatus());
        response.setPaidAt(order.getPaidAt());
        response.setRating(order.getRating());
        return response;
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return null;
        }
        return "******" + mobile.substring(mobile.length() - 4);
    }




    @org.springframework.beans.factory.annotation.Autowired
    private com.cafe.backend.repository.MenuItemAddonGroupRepository addonGroupRepository;

    private record AddonPick(BigDecimal perUnit, String text, String detail) {}

    // Checks the chosen add-ons against the item's rules and returns their price per unit
    private AddonPick pickAddons(MenuItem item, List<Long> ids) {
        java.util.Set<Long> chosen = ids == null ? java.util.Set.of() : new java.util.HashSet<>(ids);
        BigDecimal sum = BigDecimal.ZERO;
        List<String> names = new ArrayList<>();
        List<String> details = new ArrayList<>();
        int matched = 0;

        List<com.cafe.backend.entity.MenuItemAddonGroup> groups =
                addonGroupRepository.findByMenuItemIdOrderBySortOrderAsc(item.getId());

// Old cart / deleted option -> clear message first
        java.util.Set<Long> validIds = new java.util.HashSet<>();
        groups.forEach(g -> g.getOptions().forEach(o -> validIds.add(o.getId())));
        if (!validIds.containsAll(chosen)) {
            throw new ConflictException("ADDONS_CHANGED",
                    "Options for " + item.getName() + " have changed. Please add it to your cart again.");
        }

        for (com.cafe.backend.entity.MenuItemAddonGroup group : groups) {
            List<com.cafe.backend.entity.MenuItemAddon> picked = group.getOptions().stream()
                    .filter(o -> chosen.contains(o.getId())).toList();
            if (group.isRequired() && picked.isEmpty()) {
                throw new BadRequestException("ADDON_REQUIRED",
                        "Please choose " + group.getName() + " for " + item.getName() + ".");
            }
            if (picked.size() > group.getMaxSelect()) {
                throw new BadRequestException("TOO_MANY_ADDONS",
                        "Choose at most " + group.getMaxSelect() + " in " + group.getName() + " for " + item.getName() + ".");
            }
            for (com.cafe.backend.entity.MenuItemAddon o : picked) {
                sum = sum.add(o.getPrice());
                names.add(o.getName());
                details.add(o.getName().replace("|", "/").replace(";", ",") + "|" + o.getPrice().toPlainString());
            }
            matched += picked.size();
        }

        // An ID that is not an option of this item = old cart or tampered request
        if (matched != chosen.size()) {
            throw new ConflictException("ADDONS_CHANGED",
                    "Options for " + item.getName() + " have changed. Please add it to your cart again.");
        }
        return new AddonPick(sum,
                names.isEmpty() ? null : String.join(", ", names),
                details.isEmpty() ? null : String.join(";", details));
    }

}