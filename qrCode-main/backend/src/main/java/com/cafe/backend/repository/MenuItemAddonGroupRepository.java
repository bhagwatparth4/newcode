package com.cafe.backend.repository;

import com.cafe.backend.entity.MenuItemAddonGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemAddonGroupRepository extends JpaRepository<MenuItemAddonGroup, Long> {
    List<MenuItemAddonGroup> findByMenuItemIdOrderBySortOrderAsc(Long menuItemId);
    List<MenuItemAddonGroup> findAllByOrderByMenuItemIdAscSortOrderAsc();
    void deleteByMenuItemId(Long menuItemId);
}