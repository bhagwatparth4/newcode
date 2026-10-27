package com.cafe.backend.dto.admin;

public class AdminCustomerResponse {

    private Long id;
    private String mobileNumber;
    private String name;

    private boolean smsOptIn;
    private boolean whatsappOptIn;
    private boolean marketingOptOut;

    public AdminCustomerResponse(
            Long id,
            String mobileNumber,
            String name,
            boolean smsOptIn,
            boolean whatsappOptIn,
            boolean marketingOptOut) {

        this.id = id;
        this.mobileNumber = mobileNumber;
        this.name = name;
        this.smsOptIn = smsOptIn;
        this.whatsappOptIn = whatsappOptIn;
        this.marketingOptOut = marketingOptOut;
    }

    public Long getId() {
        return id;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getName() {
        return name;
    }

    public boolean isSmsOptIn() {
        return smsOptIn;
    }

    public boolean isWhatsappOptIn() {
        return whatsappOptIn;
    }

    public boolean isMarketingOptOut() {
        return marketingOptOut;
    }
}