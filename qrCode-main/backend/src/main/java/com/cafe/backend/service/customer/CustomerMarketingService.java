package com.cafe.backend.service.customer;

import com.cafe.backend.dto.customer.CustomerMarketingPreferenceRequest;
import com.cafe.backend.dto.customer.CustomerMarketingPreferenceResponse;

public interface CustomerMarketingService {

    void updateMarketingPreferences(
            CustomerMarketingPreferenceRequest request
    );

    public CustomerMarketingPreferenceResponse getMarketingPreferences(
            String mobileNumber
    );
}