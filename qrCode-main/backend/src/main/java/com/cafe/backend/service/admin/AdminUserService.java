package com.cafe.backend.service.admin;

import com.cafe.backend.dto.admin.CreateKitchenUserRequest;
import com.cafe.backend.entity.Admin;

public interface AdminUserService {

    Admin createKitchenUser(CreateKitchenUserRequest request);
}