package com.cafe.backend.controller.admin;

import com.cafe.backend.dto.admin.CreateKitchenUserRequest;
import com.cafe.backend.entity.Admin;
import com.cafe.backend.service.admin.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/kitchen")
    public ResponseEntity<String> createKitchenUser(
            @RequestBody CreateKitchenUserRequest request
    ) {
        Admin kitchenUser =
                adminUserService.createKitchenUser(request);

        return ResponseEntity.ok(
                "Kitchen user created successfully: "
                        + kitchenUser.getEmail()
        );
    }
}