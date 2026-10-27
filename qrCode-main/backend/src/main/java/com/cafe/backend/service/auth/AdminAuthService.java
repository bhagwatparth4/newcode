package com.cafe.backend.service.auth;

import com.cafe.backend.dto.admin.AdminLoginRequest;
import com.cafe.backend.dto.admin.AdminLoginResponse;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request);
}