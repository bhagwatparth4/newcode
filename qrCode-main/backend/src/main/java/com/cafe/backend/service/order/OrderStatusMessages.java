package com.cafe.backend.service.order;

import com.cafe.backend.entity.Order;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;

/*
 * Customer-facing text for each status change.
 * Shared by the Admin and Kitchen services so customers always
 * see the same wording no matter who pressed the button.
 */
public final class OrderStatusMessages {

    private OrderStatusMessages() {
    }

    public static String forCustomer(Order order) {

        OrderStatus status = order.getStatus();
        String number = order.getOrderNumber();

        if (status == OrderStatus.PREPARING) {
            return "Your order " + number + " has started preparing.";
        }

        if (status == OrderStatus.READY) {
            if (order.getOrderType() == OrderType.TABLE && order.getTable() != null) {
                return "Your order " + number
                        + " is ready and will be served at your table "
                        + order.getTable().getTableNumber() + ".";
            }
            return "Your order " + number
                    + " is ready! Please collect it from the counter.";
        }

        if (status == OrderStatus.COMPLETED) {
            return "Your order " + number
                    + " has been completed. Thank you for visiting us!";
        }

        return "Your order " + number + " was updated.";
    }
}
