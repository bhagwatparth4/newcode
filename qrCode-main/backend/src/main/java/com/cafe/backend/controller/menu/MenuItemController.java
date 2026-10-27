package com.cafe.backend.controller.menu;

import com.cafe.backend.dto.menu.MenuItemRequest;
import com.cafe.backend.dto.menu.MenuItemResponse;
import com.cafe.backend.service.menu.MenuItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menu-items")
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @PostMapping
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @RequestBody MenuItemRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(menuItemService.createMenuItem(request));
    }

    @GetMapping
    public ResponseEntity<List<MenuItemResponse>> getAllMenuItems() {

        return ResponseEntity.ok(menuItemService.getAllMenuItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemResponse> getMenuItemById(
            @PathVariable Long id) {

        return ResponseEntity.ok(menuItemService.getMenuItemById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long id,
            @RequestBody MenuItemRequest request) {

        return ResponseEntity.ok(
                menuItemService.updateMenuItem(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable Long id) {

        menuItemService.deleteMenuItem(id);

        return ResponseEntity.noContent().build();
    }
}