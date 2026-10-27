package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.AdminCustomerResponse;

import java.util.List;

public interface AdminCustomerService {

    List<AdminCustomerResponse> getAllCustomers();
}