package com.cafe.backend.dto.customer;

public class CustomerMarketingPreferenceRequest {

    private String mobileNumber;

    private boolean smsOptIn;

    private boolean whatsappOptIn;

    private boolean marketingOptOut;

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

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

    public boolean isMarketingOptOut() {
        return marketingOptOut;
    }

    public void setMarketingOptOut(boolean marketingOptOut) {
        this.marketingOptOut = marketingOptOut;
    }
}