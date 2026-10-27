package com.cafe.backend.service.admin;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.CellStyle;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import com.cafe.backend.entity.Order;
import com.cafe.backend.entity.OrderItem;
import com.cafe.backend.entity.Payment;
import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import com.cafe.backend.repository.OrderItemRepository;
import com.cafe.backend.repository.OrderRepository;
import com.cafe.backend.repository.PaymentRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.cafe.backend.enums.OrderPaymentStatus;
import com.cafe.backend.enums.PaymentMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminExcelExportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public AdminExcelExportService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
    }

    public byte[] exportOrders(
            OrderStatus status,
            OrderType orderType,
            LocalDateTime from,
            LocalDateTime to) {

        List<Order> orders;

        if (from != null && to != null && status != null && orderType != null) {

            orders = orderRepository
                    .findByOrderTypeAndStatusAndCreatedAtBetween(
                            orderType,
                            status,
                            from,
                            to
                    );

        } else if (from != null && to != null && status != null) {

            orders = orderRepository
                    .findByStatusAndCreatedAtBetween(
                            status,
                            from,
                            to
                    );

        } else if (from != null && to != null && orderType != null) {

            orders = orderRepository
                    .findByOrderTypeAndCreatedAtBetween(
                            orderType,
                            from,
                            to
                    );

        } else if (from != null && to != null) {

            orders = orderRepository
                    .findByCreatedAtBetween(from, to);

        } else if (status != null || orderType != null) {

            orders = orderRepository.findAll()
                    .stream()
                    .filter(order ->
                            (status == null || order.getStatus() == status)
                                    &&
                                    (orderType == null || order.getOrderType() == orderType)
                    )
                    .toList();

        } else {

            orders = orderRepository.findAll();
        }

        // Newest orders first
        orders = orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .toList();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("hh:mm a");

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Orders");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);

            String[] headers = {
                    "Order Number",
                    "Date",
                    "Time",
                    "Order Type",
                    "Table / Counter",
                    "Nickname",
                    "Mobile",
                    "Items",
                    "Subtotal",
                    "Coupon",
                    "Discount",
                    "Total Amount",
                    "Payment Method",
                    "Payment Status",
                    "Paid At",
                    "Cash Taken By",
                    "Counter Pay Mode",
                    "Order Taken By",
                    "Razorpay Payment ID",
                    "Transaction Reference / UPI RRN",
                    "Order Status"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNumber = 1;

            for (Order order : orders) {

                Row row = sheet.createRow(rowNumber++);

                Payment payment = paymentRepository
                        .findByOrder(order)
                        .orElse(null);

                List<OrderItem> items =
                        orderItemRepository.findByOrder(order);

                String itemText = items.stream()
                        .map(item ->
                                item.getItemName()
                                        + " x "
                                        + item.getQuantity()
                                        + " @ Rs."
                                        + item.getUnitPrice()
                        )
                        .collect(Collectors.joining(", "));

                String tableOrCounter =
                        order.getTable() != null
                                ? "Table " + order.getTable().getTableNumber()
                                : "COUNTER";

                int c = 0;
                row.createCell(c++).setCellValue(text(order.getOrderNumber()));
                row.createCell(c++).setCellValue(
                        order.getCreatedAt() != null ? order.getCreatedAt().format(dateFormat) : "");
                row.createCell(c++).setCellValue(
                        order.getCreatedAt() != null ? order.getCreatedAt().format(timeFormat) : "");
                row.createCell(c++).setCellValue(
                        order.getOrderType() != null ? order.getOrderType().name() : "");
                row.createCell(c++).setCellValue(tableOrCounter);
                row.createCell(c++).setCellValue(text(order.getNickname()));
                row.createCell(c++).setCellValue(text(order.getMobileNumber()));
                row.createCell(c++).setCellValue(itemText);
                row.createCell(c++).setCellValue(number(order.getSubtotalAmount()));
                row.createCell(c++).setCellValue(text(order.getCouponCode()));
                row.createCell(c++).setCellValue(number(order.getDiscountAmount()));
                row.createCell(c++).setCellValue(number(order.getTotalAmount()));
                // Works for both cash and online orders
                boolean cash = order.getPaymentMethod() == PaymentMethod.CASH;
                row.createCell(c++).setCellValue(cash ? "CASH" : "ONLINE");
                row.createCell(c++).setCellValue(
                        order.getStatus() == OrderStatus.CANCELLED ? "CANCELLED"
                                : order.getPaymentStatus() == OrderPaymentStatus.PAID ? "PAID" : "PENDING");
                row.createCell(c++).setCellValue(
                        order.getPaidAt() != null ? order.getPaidAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a")) : "");
                row.createCell(c++).setCellValue(text(order.getPaidBy()));
                row.createCell(c++).setCellValue(text(order.getCounterPaymentMode()));
                row.createCell(c++).setCellValue(text(order.getPlacedBy()));
                row.createCell(c++).setCellValue(
                        payment != null ? text(payment.getRazorpayPaymentId()) : "");
                row.createCell(c++).setCellValue(
                        payment != null ? text(payment.getTransactionReference()) : "");
                row.createCell(c).setCellValue(
                        order.getStatus() != null ? order.getStatus().name() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                try {
                    sheet.autoSizeColumn(i);
                } catch (Throwable fontProblem) {
                    // Some minimal cloud servers have no fonts installed;
                    // use a sensible fixed width instead of failing the export.
                    sheet.setColumnWidth(i, 20 * 256);
                }
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    private static String text(String value) {
        return value != null ? value : "";
    }

    private static double number(java.math.BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}
