package com.cafe.backend.entity;

import com.cafe.backend.enums.OrderStatus;
import com.cafe.backend.enums.OrderType;
import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.cafe.backend.entity.Customer;
import com.cafe.backend.enums.PaymentMethod;
import com.cafe.backend.enums.OrderPaymentStatus;
@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_number", columnNames = "order_number")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private CafeTable table;

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "idempotency_key",unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "subtotal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    private Integer estimatedPreparationMinutes;

    private LocalDateTime estimatedReadyAt;

    @Column(name = "late_notification_sent")
    private boolean lateNotificationSent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "cooking_note", length = 50)
    private String cookingNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 10)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 10)
    private OrderPaymentStatus paymentStatus;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_by", length = 100)
    private String paidBy;


    @Column(
            name = "customer_notification_token",
            unique = true,
            length = 100
    )
    private String customerNotificationToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (customerNotificationToken == null) {
            customerNotificationToken = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Column(name = "cancel_seen")
    private Boolean cancelSeen;

    public Boolean getCancelSeen() { return cancelSeen; }


    @Column(name = "rating")
    private Integer rating;

    @Column(name = "feedback", length = 200)
    private String feedback;

    @Column(name = "rated_at")
    private LocalDateTime ratedAt;

    @Column(name = "counter_payment_mode", length = 10)
    private String counterPaymentMode;   // CASH / UPI / CARD (label only)

    @Column(name = "placed_by", length = 100)
    private String placedBy;             // staff who created a counter order

    public String getCounterPaymentMode() { return counterPaymentMode; }
    public void setCounterPaymentMode(String v) { this.counterPaymentMode = v; }
    public String getPlacedBy() { return placedBy; }
    public void setPlacedBy(String v) { this.placedBy = v; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public LocalDateTime getRatedAt() { return ratedAt; }
    public void setRatedAt(LocalDateTime ratedAt) { this.ratedAt = ratedAt; }
    public void setCancelSeen(Boolean cancelSeen) { this.cancelSeen = cancelSeen; }


    public boolean isLateNotificationSent() {
        return lateNotificationSent;
    }


    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }


    public void setLateNotificationSent(boolean lateNotificationSent) {
        this.lateNotificationSent = lateNotificationSent;
    }

    public String getCookingNote() {
        return cookingNote;
    }

    public void setCookingNote(String cookingNote) {
        this.cookingNote = cookingNote;
    }


    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public CafeTable getCafeTable() {
        return table;
    }

    public void setCafeTable(CafeTable cafeTable) {
        this.table = cafeTable;
    }


    public Integer getEstimatedPreparationMinutes() {
        return estimatedPreparationMinutes;
    }

    public void setEstimatedPreparationMinutes(Integer estimatedPreparationMinutes) {
        this.estimatedPreparationMinutes = estimatedPreparationMinutes;
    }

    public LocalDateTime getEstimatedReadyAt() {
        return estimatedReadyAt;
    }

    public void setEstimatedReadyAt(LocalDateTime estimatedReadyAt) {
        this.estimatedReadyAt = estimatedReadyAt;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public CafeTable getTable() {
        return table;
    }

    public void setTable(CafeTable table) {
        this.table = table;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getCustomerNotificationToken() {
        return customerNotificationToken;
    }

    public void setCustomerNotificationToken(
            String customerNotificationToken
    ) {
        this.customerNotificationToken = customerNotificationToken;
    }

    // Old/online orders have null here -> treated as ONLINE
    public PaymentMethod getPaymentMethod() {
        return paymentMethod == null ? PaymentMethod.ONLINE : paymentMethod;
    }
    public void setPaymentMethod(PaymentMethod m) { this.paymentMethod = m; }

    // Online orders: derived from existing status, so online flow is untouched
    public OrderPaymentStatus getPaymentStatus() {
        if (paymentStatus != null) return paymentStatus;
        return (status == OrderStatus.PAYMENT_PENDING || status == OrderStatus.CANCELLED)
                ? OrderPaymentStatus.PENDING : OrderPaymentStatus.PAID;
    }
    public void setPaymentStatus(OrderPaymentStatus s) { this.paymentStatus = s; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getPaidBy() { return paidBy; }
    public void setPaidBy(String paidBy) { this.paidBy = paidBy; }
}