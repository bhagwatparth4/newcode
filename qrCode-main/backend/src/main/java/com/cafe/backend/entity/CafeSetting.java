package com.cafe.backend.entity;

import jakarta.persistence.*;
import com.cafe.backend.enums.BusinessType;

@Entity
@Table(name = "cafe_settings")
public class CafeSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "default_preparation_minutes", nullable = false)
    private Integer defaultPreparationMinutes = 20;

    public Long getId() {
        return id;
    }

    /*
     * NOTE: The business type actually used by the application comes from the
     * BUSINESS_TYPE environment variable (see CafeSettingServiceImpl).
     * This column is kept only so the existing table/data is not changed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 30)
    private BusinessType businessType = BusinessType.SMALL_CAFE;

    @Column(name = "accepting_orders")
    private Boolean acceptingOrders;


    @Column(name = "cash_enabled")
    private Boolean cashEnabled;

    public Boolean getCashEnabled() { return cashEnabled; }
    public void setCashEnabled(Boolean cashEnabled) { this.cashEnabled = cashEnabled; }


    public Integer getDefaultPreparationMinutes() {
        return defaultPreparationMinutes;
    }

    public void setDefaultPreparationMinutes(Integer defaultPreparationMinutes) {
        this.defaultPreparationMinutes = defaultPreparationMinutes;

    }

    public Boolean getAcceptingOrders() {
        return acceptingOrders;
    }

    public void setAcceptingOrders(Boolean acceptingOrders) {
        this.acceptingOrders = acceptingOrders;
    }

    public BusinessType getBusinessType() {
        return businessType;
    }

    public void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }
}