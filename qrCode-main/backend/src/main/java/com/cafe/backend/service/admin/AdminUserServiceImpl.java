package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.CreateKitchenUserRequest;
import com.cafe.backend.entity.Admin;
import com.cafe.backend.enums.BusinessType;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.repository.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final CafeSettingService cafeSettingService;

    public AdminUserServiceImpl(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            CafeSettingService cafeSettingService
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.cafeSettingService = cafeSettingService;
    }

    @Override
    @Transactional
    public Admin createKitchenUser(CreateKitchenUserRequest request) {

        // SMALL_CAFE = Admin only. Kitchen accounts are not allowed at all.
        if (cafeSettingService.getBusinessType() == BusinessType.SMALL_CAFE) {
            throw new ConflictException(
                    "KITCHEN_NOT_ALLOWED",
                    "Kitchen accounts are not available for a Small Cafe."
            );
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("NAME_REQUIRED", "Name is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()
                || !request.getEmail().contains("@")) {
            throw new BadRequestException("EMAIL_REQUIRED", "A valid email is required");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BadRequestException(
                    "WEAK_PASSWORD",
                    "Password must be at least 8 characters"
            );
        }

        String email = request.getEmail().trim().toLowerCase();

        if (adminRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_EXISTS", "Email already exists");
        }

        Admin kitchenUser = new Admin();
        kitchenUser.setName(request.getName().trim());
        kitchenUser.setEmail(email);
        kitchenUser.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        // Role is always decided by the backend, never by the request
        kitchenUser.setRole("KITCHEN");
        kitchenUser.setActive(true);

        return adminRepository.save(kitchenUser);
    }
}
