package com.cafe.backend.entity;

import com.cafe.backend.enums.MarketingChannel;
import com.cafe.backend.enums.MarketingDeliveryStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "marketing_campaign_recipients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_campaign_customer_channel",
                        columnNames = {"campaign_id", "customer_id", "channel"}
                )
        }
)
public class MarketingCampaignRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private MarketingCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketingChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketingDeliveryStatus status = MarketingDeliveryStatus.PENDING;

    @Column(length = 100)
    private String providerMessageId;

    @Column(length = 500)
    private String failureReason;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime failedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = MarketingDeliveryStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public MarketingCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(MarketingCampaign campaign) {
        this.campaign = campaign;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public MarketingChannel getChannel() {
        return channel;
    }

    public void setChannel(MarketingChannel channel) {
        this.channel = channel;
    }

    public MarketingDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(MarketingDeliveryStatus status) {
        this.status = status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}