package com.cafe.backend.service.order;

import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.service.order.OrderStatusTransitionPolicy.Actor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Plain unit test: no database, no Spring context needed.
 * Run with:  mvn -Dtest=OrderStatusTransitionPolicyTest test
 */
class OrderStatusTransitionPolicyTest {

    // ---------- SMALL_CAFE ----------

    @Test
    void smallCafeAdminRunsTheWholeLifecycle() {
        BusinessType small = BusinessType.SMALL_CAFE;
        assertTrue(OrderStatusTransitionPolicy.isAllowed(small, Actor.ADMIN, OrderStatus.PAID, OrderStatus.PREPARING));
        assertTrue(OrderStatusTransitionPolicy.isAllowed(small, Actor.ADMIN, OrderStatus.PREPARING, OrderStatus.READY));
        assertTrue(OrderStatusTransitionPolicy.isAllowed(small, Actor.ADMIN, OrderStatus.READY, OrderStatus.COMPLETED));
    }

    @Test
    void smallCafeHasNoKitchen() {
        BusinessType small = BusinessType.SMALL_CAFE;
        assertFalse(OrderStatusTransitionPolicy.isAllowed(small, Actor.KITCHEN, OrderStatus.PAID, OrderStatus.PREPARING));
        assertFalse(OrderStatusTransitionPolicy.isAllowed(small, Actor.KITCHEN, OrderStatus.PREPARING, OrderStatus.READY));
        assertFalse(OrderStatusTransitionPolicy.isAllowed(small, Actor.KITCHEN, OrderStatus.READY, OrderStatus.COMPLETED));
    }

    // ---------- LARGE_CAFE / RESTAURANT ----------

    @Test
    void kitchenPreparesButNeverCompletes() {
        for (BusinessType type : new BusinessType[]{BusinessType.LARGE_CAFE, BusinessType.RESTAURANT}) {
            assertTrue(OrderStatusTransitionPolicy.isAllowed(type, Actor.KITCHEN, OrderStatus.PAID, OrderStatus.PREPARING));
            assertTrue(OrderStatusTransitionPolicy.isAllowed(type, Actor.KITCHEN, OrderStatus.PREPARING, OrderStatus.READY));
            assertFalse(OrderStatusTransitionPolicy.isAllowed(type, Actor.KITCHEN, OrderStatus.READY, OrderStatus.COMPLETED));
        }
    }

    @Test
    void adminOnlyCompletesWhenKitchenExists() {
        for (BusinessType type : new BusinessType[]{BusinessType.LARGE_CAFE, BusinessType.RESTAURANT}) {
            assertTrue(OrderStatusTransitionPolicy.isAllowed(type, Actor.ADMIN, OrderStatus.READY, OrderStatus.COMPLETED));
            assertFalse(OrderStatusTransitionPolicy.isAllowed(type, Actor.ADMIN, OrderStatus.PAID, OrderStatus.PREPARING));
            assertFalse(OrderStatusTransitionPolicy.isAllowed(type, Actor.ADMIN, OrderStatus.PREPARING, OrderStatus.READY));
        }
    }

    // ---------- Things nobody may do ----------

    @Test
    void noSkippingOrGoingBackwards() {
        for (BusinessType type : BusinessType.values()) {
            for (Actor actor : Actor.values()) {
                assertFalse(OrderStatusTransitionPolicy.isAllowed(type, actor, OrderStatus.PAYMENT_PENDING, OrderStatus.PAID));
                assertFalse(OrderStatusTransitionPolicy.isAllowed(type, actor, OrderStatus.PAID, OrderStatus.READY));
                assertFalse(OrderStatusTransitionPolicy.isAllowed(type, actor, OrderStatus.PAID, OrderStatus.COMPLETED));
                assertFalse(OrderStatusTransitionPolicy.isAllowed(type, actor, OrderStatus.READY, OrderStatus.PREPARING));
                assertFalse(OrderStatusTransitionPolicy.isAllowed(type, actor, OrderStatus.COMPLETED, OrderStatus.READY));
                assertFalse(OrderStatusTransitionPolicy.isAllowed(type, actor, null, OrderStatus.READY));
            }
        }
    }
}
