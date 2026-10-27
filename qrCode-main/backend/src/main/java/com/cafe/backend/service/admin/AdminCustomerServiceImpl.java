package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.AdminCustomerResponse;
import com.cafe.backend.entity.Customer;
import com.cafe.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerRepository customerRepository;

    public AdminCustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<AdminCustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AdminCustomerResponse mapToResponse(Customer customer) {
        return new AdminCustomerResponse(
                customer.getId(),
                customer.getMobileNumber(),
                customer.getName(),
                customer.isSmsOptIn(),
                customer.isWhatsappOptIn(),
                customer.isMarketingOptOut()
        );
    }
}