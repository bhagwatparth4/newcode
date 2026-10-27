package com.cafe.backend.service.menu;

import java.math.BigDecimal;
import org.springframework.dao.DataIntegrityViolationException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.dto.menu.MenuItemRequest;
import com.cafe.backend.dto.menu.MenuItemResponse;
import com.cafe.backend.entity.Category;
import com.cafe.backend.entity.MenuItem;
import com.cafe.backend.repository.CategoryRepository;
import com.cafe.backend.repository.MenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    public MenuItemServiceImpl(
            MenuItemRepository menuItemRepository,
            CategoryRepository categoryRepository) {

        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public MenuItemResponse createMenuItem(MenuItemRequest request) {

        validate(request);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        MenuItem menuItem = new MenuItem();

        menuItem.setCategory(category);
        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setPrice(request.getPrice());
        menuItem.setImageUrl(request.getImageUrl());
        menuItem.setDietaryType(request.getDietaryType());
        menuItem.setAvailable(request.isAvailable());

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);

        return mapToResponse(savedMenuItem);
    }

    @Override
    public MenuItemResponse getMenuItemById(Long id) {

        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MENU_ITEM_NOT_FOUND", "Menu item not found"));

        return mapToResponse(menuItem);
    }

    @Override
    public List<MenuItemResponse> getAllMenuItems() {

        return menuItemRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {

        validate(request);

        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MENU_ITEM_NOT_FOUND", "Menu item not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        menuItem.setCategory(category);
        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setPrice(request.getPrice());
        menuItem.setImageUrl(request.getImageUrl());
        menuItem.setDietaryType(request.getDietaryType());
        menuItem.setAvailable(request.isAvailable());

        MenuItem updatedMenuItem = menuItemRepository.save(menuItem);

        return mapToResponse(updatedMenuItem);
    }

    @Override
    public void deleteMenuItem(Long id) {

        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("MENU_ITEM_NOT_FOUND", "Menu item not found"));

        try {
            menuItemRepository.delete(menuItem);
            menuItemRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // Old orders point to this item. Deleting it would break order history.
            throw new ConflictException(
                    "MENU_ITEM_IN_USE",
                    "This item appears in past orders, so it cannot be deleted. "
                            + "Mark it as unavailable instead."
            );
        }
    }

    // Customer menu: hide items whose category the admin switched off
    @Override
    public List<MenuItemResponse> getPublicMenuItems() {
        return menuItemRepository.findAll()
                .stream()
                .filter(menuItem -> menuItem.getCategory().isActive())
                .map(this::mapToResponse)
                .toList();
    }

    private void validate(MenuItemRequest request) {

        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("NAME_REQUIRED", "Item name is required");
        }

        if (request.getCategoryId() == null) {
            throw new BadRequestException("CATEGORY_REQUIRED", "Category is required");
        }

        if (request.getPrice() == null
                || request.getPrice().compareTo(BigDecimal.ONE) < 0
                || request.getPrice().scale() > 2) {
            throw new BadRequestException(
                    "INVALID_PRICE",
                    "Price must be at least ₹1 with at most 2 decimal places"
            );
        }

        if (request.getDietaryType() == null) {
            throw new BadRequestException("DIETARY_TYPE_REQUIRED", "Choose VEG or NON_VEG");
        }

        request.setName(request.getName().trim());
    }

    private MenuItemResponse mapToResponse(MenuItem menuItem) {

        MenuItemResponse response = new MenuItemResponse();

        response.setId(menuItem.getId());

        response.setCategoryId(menuItem.getCategory().getId());
        response.setCategoryName(menuItem.getCategory().getName());

        response.setName(menuItem.getName());
        response.setDescription(menuItem.getDescription());
        response.setPrice(menuItem.getPrice());
        response.setImageUrl(menuItem.getImageUrl());
        response.setDietaryType(menuItem.getDietaryType());
        response.setAvailable(menuItem.isAvailable());

        response.setCreatedAt(menuItem.getCreatedAt());
        response.setUpdatedAt(menuItem.getUpdatedAt());

        return response;
    }
}