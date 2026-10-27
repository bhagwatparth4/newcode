package com.cafe.backend.dto.admin;

import com.cafe.backend.enums.MarketingChannel;

public class CreateMarketingCampaignRequest {

    private String name;
    private String message;
    private MarketingChannel channel;

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
}