package com.cafe.backend.dto.order;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import com.cafe.backend.enums.PaymentMethod;

public class OrderCreateRequest {

    @NotBlank(message = "Nickname is required")
    @Size(max = 100, message = "Nickname must not exceed 100 characters")
    private String nickname;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 15, message = "Mobile number must not exceed 15 characters")
    @Pattern(
            regexp = "^\\s*[6-9][0-9]{9}\\s*$",
            message = "Please enter a valid 10-digit mobile number"
    )
    private String mobileNumber;

    public String getCookingNote() {
        return cookingNote;
    }

    public void setCookingNote(String cookingNote) {
        this.cookingNote = cookingNote;
    }

    @Size(max = 50, message = "Cooking request must be 50 characters or less")
    private String cookingNote;

    @NotBlank(message = "QR token is required")
    @Size(max = 100, message = "QR token is too long")
    private String qrToken;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String idempotencyKey;

    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    private String couponCode;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;


    private boolean smsOptIn;

    private boolean whatsappOptIn;

    // null = ONLINE (old frontend keeps working)
    private PaymentMethod paymentMethod;


    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean staffOrder;

    public boolean isStaffOrder() { return staffOrder; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public void setStaffOrder(boolean staffOrder) { this.staffOrder = staffOrder; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isSmsOptIn() {
        return smsOptIn;
    }

    public void setSmsOptIn(boolean smsOptIn) {
        this.smsOptIn = smsOptIn;
    }

    public boolean isWhatsappOptIn() {
        return whatsappOptIn;
    }

    public void setWhatsappOptIn(boolean whatsappOptIn) {
        this.whatsappOptIn = whatsappOptIn;
    }


    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }


    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }


    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
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

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}