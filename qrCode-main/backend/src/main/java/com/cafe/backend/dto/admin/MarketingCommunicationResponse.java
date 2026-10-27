package com.cafe.backend.dto.admin;

import com.cafe.backend.enums.MarketingChannel;
import com.cafe.backend.enums.MarketingDeliveryStatus;

import java.time.LocalDateTime;

public class MarketingCommunicationResponse {

    private Long campaignId;
    private String campaignName;
    private MarketingChannel channel;
    private MarketingDeliveryStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
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
}