package com.cafe.backend.service.auth;

import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.service.admin.CafeSettingService;
import com.cafe.backend.dto.admin.AdminLoginRequest;
import com.cafe.backend.dto.admin.AdminLoginResponse;
import com.cafe.backend.entity.Admin;
import com.cafe.backend.repository.AdminRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CafeSettingService cafeSettingService;

    public AdminAuthServiceImpl(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CafeSettingService cafeSettingService
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cafeSettingService = cafeSettingService;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {

        if (request == null
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Admin admin = adminRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check the password BEFORE revealing anything about the account
        if (!passwordEncoder.matches(
                request.getPassword(),
                admin.getPasswordHash()
        )) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!admin.isActive()) {
            throw new BadCredentialsException("This account is inactive");
        }

        BusinessType businessType = cafeSettingService.getBusinessType();

        // SMALL_CAFE has no Kitchen dashboard, so Kitchen accounts cannot log in
        if ("KITCHEN".equals(admin.getRole())
                && businessType == BusinessType.SMALL_CAFE) {
            throw new BadCredentialsException(
                    "Kitchen access is not enabled for this cafe"
            );
        }

        String token = jwtService.generateToken(
                admin.getEmail(),
                admin.getRole()
        );

        AdminLoginResponse response = new AdminLoginResponse();

        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setAdminId(admin.getId());
        response.setName(admin.getName());
        response.setEmail(admin.getEmail());
        response.setRole(admin.getRole());
        response.setBusinessType(businessType.name());

        return response;
    }
}