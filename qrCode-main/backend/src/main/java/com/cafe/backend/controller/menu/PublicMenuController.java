package com.cafe.backend.controller.menu;

import com.cafe.backend.dto.menu.MenuItemResponse;
import com.cafe.backend.service.menu.MenuItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class PublicMenuController {

    private final MenuItemService menuItemService;

    public PublicMenuController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping("/items")
    public ResponseEntity<List<MenuItemResponse>> getPublicMenuItems() {
        return ResponseEntity.ok(menuItemService.getPublicMenuItems());
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<MenuItemResponse> getPublicMenuItemById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                menuItemService.getMenuItemById(id)
        );
    }
}