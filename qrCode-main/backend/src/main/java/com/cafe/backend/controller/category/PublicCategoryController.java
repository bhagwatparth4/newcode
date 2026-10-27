package com.cafe.backend.controller.category;

import com.cafe.backend.dto.category.CategoryResponse;
import com.cafe.backend.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu/categories")
public class PublicCategoryController {

    private final CategoryRepository categoryRepository;

    public PublicCategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {

        List<CategoryResponse> categories =
                categoryRepository.findByActiveTrue()
                        .stream()
                        .map(category -> {
                            CategoryResponse response = new CategoryResponse();

                            response.setId(category.getId());
                            response.setName(category.getName());
                            response.setDescription(category.getDescription());
                            response.setImageUrl(category.getImageUrl());
                            response.setActive(category.isActive());
                            response.setCreatedAt(category.getCreatedAt());
                            response.setUpdatedAt(category.getUpdatedAt());

                            return response;
                        })
                        .toList();

        return ResponseEntity.ok(categories);
    }
}