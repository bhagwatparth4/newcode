package com.cafe.backend.controller.customer;

import com.cafe.backend.dto.customer.CustomerMarketingPreferenceRequest;
import com.cafe.backend.dto.customer.CustomerMarketingPreferenceResponse;
import com.cafe.backend.service.customer.CustomerMarketingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/marketing")
public class CustomerMarketingController {

    private final CustomerMarketingService customerMarketingService;

    public CustomerMarketingController(
            CustomerMarketingService customerMarketingService) {
        this.customerMarketingService = customerMarketingService;
    }

    @PatchMapping("/preferences")
    public ResponseEntity<Void> updatePreferences(
            @RequestBody CustomerMarketingPreferenceRequest request) {

        customerMarketingService.updateMarketingPreferences(request);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences/{mobileNumber}")
    public ResponseEntity<CustomerMarketingPreferenceResponse> getPreferences(
            @PathVariable String mobileNumber) {

        CustomerMarketingPreferenceResponse response =
                customerMarketingService.getMarketingPreferences(mobileNumber);

        return ResponseEntity.ok(response);
    }
}