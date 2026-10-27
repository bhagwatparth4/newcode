package com.cafe.backend.entity;

import com.cafe.backend.enums.MarketingCampaignStatus;
import com.cafe.backend.enums.MarketingChannel;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;


@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "marketing_campaigns")
public class MarketingCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketingChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MarketingCampaignStatus status;

    @Column(nullable = false)
    private int totalRecipients = 0;



    @Column(nullable = false)
    private int sentCount = 0;

    @Column(nullable = false)
    private int failedCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = MarketingCampaignStatus.DRAFT;
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MarketingChannel getChannel() {
        return channel;
    }

    public void setChannel(MarketingChannel channel) {
        this.channel = channel;
    }

    public MarketingCampaignStatus getStatus() {
        return status;
    }

    public void setStatus(MarketingCampaignStatus status) {
        this.status = status;
    }

    public int getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(int totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}