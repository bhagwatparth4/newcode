package com.cafe.backend.controller.menu; // PaymentOptionsController jaisa package

import com.cafe.backend.entity.Coupon;
import com.cafe.backend.enums.DiscountType;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.repository.CouponRepository;
import com.cafe.backend.repository.MenuItemAddonGroupRepository;
import com.cafe.backend.repository.OrderItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menu") // PaymentOptionsController jaisa hi public prefix
public class MenuExtrasController {

    private final OrderItemRepository orderItemRepository;
    private final CouponRepository couponRepository;

    private final MenuItemAddonGroupRepository groupRepository;

    public MenuExtrasController(OrderItemRepository orderItemRepository, CouponRepository couponRepository, MenuItemAddonGroupRepository groupRepository) {
        this.orderItemRepository = orderItemRepository;
        this.couponRepository = couponRepository;
        this.groupRepository = groupRepository;
    }

    // Top 5 items by quantity sold in the last 30 days (confirmed orders only)
    @GetMapping("/bestsellers")
    public List<Long> bestsellers() {
        return orderItemRepository.findTopSellingItemIds(
                List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.COMPLETED),
                LocalDateTime.now().minusDays(30),
                PageRequest.of(0, 5)
        );
    }

    // Latest coupon, only if it is usable right now; otherwise no banner
    @GetMapping("/offer")
    public ResponseEntity<Map<String, String>> offer() {
        Coupon c = couponRepository.findTopByOrderByIdDesc().orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (c == null || !c.isActive()
                || now.isBefore(c.getValidFrom()) || now.isAfter(c.getValidUntil())
                || c.getUsedCount() >= c.getUsageLimit()) {
            return ResponseEntity.noContent().build();
        }

        String value = c.getDiscountValue().stripTrailingZeros().toPlainString();
        String text = (c.getDiscountType() == DiscountType.PERCENTAGE ? value + "% off" : "₹" + value + " off")
                + " on online payment";
        if (c.getMinimumOrderAmount() != null && c.getMinimumOrderAmount().signum() > 0) {
            text += " · min order ₹" + c.getMinimumOrderAmount().stripTrailingZeros().toPlainString();
        }

        return ResponseEntity.ok(Map.of("code", c.getCode(), "text", text));
    }

    @GetMapping("/addons")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Map<String, Object>> addons() {
        return groupRepository.findAllByOrderByMenuItemIdAscSortOrderAsc().stream()
                .map(com.cafe.backend.entity.MenuItemAddonGroup::toView).toList();
    }
}