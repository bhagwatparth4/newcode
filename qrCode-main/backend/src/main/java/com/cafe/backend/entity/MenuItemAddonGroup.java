package com.cafe.backend.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// A question on an item, e.g. "Size" (pick 1, required) or "Extra toppings" (up to 3)
@Entity
@Table(name = "menu_item_addon_groups")
public class MenuItemAddonGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "max_select", nullable = false)
    private int maxSelect; // 1 = pick one

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MenuItemAddon> options = new ArrayList<>();

    // JSON shape used by both admin and customer APIs
    public Map<String, Object> toView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", id);
        view.put("menuItemId", menuItem.getId());
        view.put("name", name);
        view.put("required", required);
        view.put("maxSelect", maxSelect);
        view.put("options", options.stream()
                .map(o -> Map.<String, Object>of("id", o.getId(), "name", o.getName(), "price", o.getPrice()))
                .toList());
        return view;
    }

    public Long getId() { return id; }
    public MenuItem getMenuItem() { return menuItem; }
    public void setMenuItem(MenuItem menuItem) { this.menuItem = menuItem; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public int getMaxSelect() { return maxSelect; }
    public void setMaxSelect(int maxSelect) { this.maxSelect = maxSelect; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<MenuItemAddon> getOptions() { return options; }
}