package com.cafe.backend.service.order;

import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.enums.OrderStatus;

/*
 * ONE place that decides who may move an order from one status to another.
 * Both AdminOrderServiceImpl and KitchenOrderServiceImpl call this, so the
 * rules cannot drift apart. It is plain Java (no Spring) so it is easy to
 * read and easy to unit test (see OrderStatusTransitionPolicyTest).
 *
 * SMALL_CAFE (Admin only):
 *   ADMIN   : PAID -> PREPARING -> READY -> COMPLETED
 *   KITCHEN : nothing (kitchen does not exist)
 *
 * LARGE_CAFE / RESTAURANT (Admin + Kitchen):
 *   KITCHEN : PAID -> PREPARING -> READY
 *   ADMIN   : READY -> COMPLETED
 *
 * Kitchen can NEVER mark an order COMPLETED.
 */
public final class OrderStatusTransitionPolicy {

    public enum Actor {
        ADMIN,
        KITCHEN
    }

    private OrderStatusTransitionPolicy() {
    }

    public static boolean isAllowed(
            BusinessType businessType,
            Actor actor,
            OrderStatus from,
            OrderStatus to
    ) {
        if (businessType == null || actor == null || from == null || to == null) {
            return false;
        }

        boolean hasKitchen = businessType != BusinessType.SMALL_CAFE;

        if (actor == Actor.KITCHEN) {
            if (!hasKitchen) {
                return false;
            }
            return isPreparationStep(from, to);
        }

        // actor == ADMIN
        boolean completion = from == OrderStatus.READY && to == OrderStatus.COMPLETED;

        if (hasKitchen) {
            // Kitchen prepares, Admin only hands over / completes
            return completion;
        }

        // Small cafe: Admin runs the whole lifecycle
        return isPreparationStep(from, to) || completion;
    }

    private static boolean isPreparationStep(OrderStatus from, OrderStatus to) {
        return (from == OrderStatus.PAID && to == OrderStatus.PREPARING)
                || (from == OrderStatus.PREPARING && to == OrderStatus.READY);
    }
}
