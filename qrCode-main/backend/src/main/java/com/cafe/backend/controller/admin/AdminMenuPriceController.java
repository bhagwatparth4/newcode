package com.cafe.backend.controller.admin;

import com.cafe.backend.entity.MenuItem;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.repository.MenuItemAddonGroupRepository;
import com.cafe.backend.repository.MenuItemRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/menu")
public class AdminMenuPriceController {

    private final MenuItemRepository menuItemRepository;
    private final com.cafe.backend.repository.MenuItemAddonGroupRepository groupRepository;


    public AdminMenuPriceController(MenuItemRepository menuItemRepository, MenuItemAddonGroupRepository groupRepository) {
        this.menuItemRepository = menuItemRepository;
        this.groupRepository = groupRepository;
    }

    @PostMapping("/bulk-price")
    @Transactional
    public Map<String, Integer> bulkPrice(@RequestBody Map<String, BigDecimal> body) {

        BigDecimal percent = body.get("percent");
        if (percent == null || percent.signum() == 0
                || percent.compareTo(BigDecimal.valueOf(-90)) < 0
                || percent.compareTo(BigDecimal.valueOf(200)) > 0) {
            throw new BadRequestException("INVALID_PERCENT", "Percent must be between -90 and 200.");
        }

        BigDecimal factor = BigDecimal.ONE.add(percent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        List<MenuItem> items = menuItemRepository.findAll();

        for (MenuItem item : items) {
            BigDecimal newPrice = item.getPrice().multiply(factor).setScale(0, RoundingMode.HALF_UP);
            item.setPrice(newPrice.max(BigDecimal.ONE)); // never below ₹1
        }
        menuItemRepository.saveAll(items); // all or nothing (one transaction)

        return Map.of("updated", items.size());
    }

    @GetMapping("/items/{itemId}/addons")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAddons(@PathVariable Long itemId) {
        return groupRepository.findByMenuItemIdOrderBySortOrderAsc(itemId).stream()
                .map(com.cafe.backend.entity.MenuItemAddonGroup::toView).toList();
    }

    // Body: [{ name, required, maxSelect, options: [{ name, price }] }]. Replaces all groups of the item.
    // Orders keep their own copy of chosen add-ons, so old orders are never affected.
    @PutMapping("/items/{itemId}/addons")
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Integer> saveAddons(@PathVariable Long itemId, @RequestBody List<Map<String, Object>> body) {

        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new BadRequestException("MENU_ITEM_NOT_FOUND", "Menu item not found."));
        if (body.size() > 6) {
            throw new BadRequestException("INVALID_ADDON", "Maximum 6 groups per item.");
        }

        groupRepository.deleteByMenuItemId(itemId);
        groupRepository.flush();

        int order = 0;
        for (Map<String, Object> g : body) {
            String groupName = String.valueOf(g.getOrDefault("name", "")).trim();
            List<Map<String, Object>> options = (List<Map<String, Object>>) g.getOrDefault("options", List.of());
            int maxSelect;
            try {
                maxSelect = Integer.parseInt(String.valueOf(g.getOrDefault("maxSelect", 1)));
            } catch (NumberFormatException e) {
                maxSelect = 1;
            }

            if (groupName.isEmpty() || groupName.length() > 50) {
                throw new BadRequestException("INVALID_ADDON", "Every group needs a name (max 50 characters).");
            }
            if (options.isEmpty() || options.size() > 15) {
                throw new BadRequestException("INVALID_ADDON", "\"" + groupName + "\" needs 1 to 15 options.");
            }
            if (maxSelect < 1 || maxSelect > options.size()) {
                throw new BadRequestException("INVALID_ADDON", "\"" + groupName + "\": max choices must be between 1 and " + options.size() + ".");
            }

            com.cafe.backend.entity.MenuItemAddonGroup group = new com.cafe.backend.entity.MenuItemAddonGroup();
            group.setMenuItem(item);
            group.setName(groupName);
            group.setRequired(Boolean.TRUE.equals(g.get("required")));
            group.setMaxSelect(maxSelect);
            group.setSortOrder(order++);

            for (Map<String, Object> o : options) {
                String name = String.valueOf(o.getOrDefault("name", "")).trim();
                BigDecimal price;
                try {
                    price = new BigDecimal(String.valueOf(o.get("price"))).setScale(2, RoundingMode.HALF_UP);
                } catch (Exception e) {
                    throw new BadRequestException("INVALID_ADDON", "Enter a valid price for " + name);
                }
                if (name.isEmpty() || name.length() > 50 || price.signum() < 0 || price.compareTo(BigDecimal.valueOf(10000)) > 0) {
                    throw new BadRequestException("INVALID_ADDON", "Each option needs a name (max 50) and a price from 0 to 10000.");
                }
                com.cafe.backend.entity.MenuItemAddon addon = new com.cafe.backend.entity.MenuItemAddon();
                addon.setGroup(group);
                addon.setName(name);
                addon.setPrice(price);
                group.getOptions().add(addon);
            }
            groupRepository.save(group);
        }
        return Map.of("saved", body.size());
    }
}