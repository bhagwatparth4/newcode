package com.cafe.backend.service.menu;

import com.cafe.backend.dto.menu.MenuItemRequest;
import com.cafe.backend.dto.menu.MenuItemResponse;

import java.util.List;

public interface MenuItemService {

    MenuItemResponse createMenuItem(MenuItemRequest request);

    MenuItemResponse getMenuItemById(Long id);

    List<MenuItemResponse> getAllMenuItems();

    MenuItemResponse updateMenuItem(Long id, MenuItemRequest request);

    void deleteMenuItem(Long id);

    // Items shown to customers (items of inactive categories are hidden)
    List<MenuItemResponse> getPublicMenuItems();
}