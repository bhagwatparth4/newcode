package com.cafe.backend.service.customer;

import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.dto.customer.CustomerMarketingPreferenceRequest;
import com.cafe.backend.dto.customer.CustomerMarketingPreferenceResponse;
import com.cafe.backend.entity.Customer;
import com.cafe.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerMarketingServiceImpl implements CustomerMarketingService {

    private final CustomerRepository customerRepository;

    public CustomerMarketingServiceImpl(
            CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void updateMarketingPreferences(
            CustomerMarketingPreferenceRequest request) {

        Customer customer = customerRepository
                .findByMobileNumber(request.getMobileNumber())
                .orElseThrow(() ->
                        new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));

        customer.setSmsOptIn(request.isSmsOptIn());
        customer.setWhatsappOptIn(request.isWhatsappOptIn());
        customer.setMarketingOptOut(request.isMarketingOptOut());

        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerMarketingPreferenceResponse getMarketingPreferences(
            String mobileNumber) {

        Customer customer = customerRepository
                .findByMobileNumber(mobileNumber)
                .orElseThrow(() ->
                        new NotFoundException("CUSTOMER_NOT_FOUND", "Customer not found"));

        return new CustomerMarketingPreferenceResponse(
                customer.getMobileNumber(),
                customer.isSmsOptIn(),
                customer.isWhatsappOptIn(),
                customer.isMarketingOptOut()
        );
    }


}