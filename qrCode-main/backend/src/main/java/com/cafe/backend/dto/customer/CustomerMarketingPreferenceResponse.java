package com.cafe.backend.dto.customer;

public class CustomerMarketingPreferenceResponse {

    private String mobileNumber;
    private boolean smsOptIn;
    private boolean whatsappOptIn;
    private boolean marketingOptOut;

    public CustomerMarketingPreferenceResponse(
            String mobileNumber,
            boolean smsOptIn,
            boolean whatsappOptIn,
            boolean marketingOptOut) {

        this.mobileNumber = mobileNumber;
        this.smsOptIn = smsOptIn;
        this.whatsappOptIn = whatsappOptIn;
        this.marketingOptOut = marketingOptOut;
    }

    public String getMobileNumber() {
        return mobileNumber;
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