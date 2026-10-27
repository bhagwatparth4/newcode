package com.cafe.backend.service.category;

import org.springframework.dao.DataIntegrityViolationException;
import com.cafe.backend.exception.ConflictException;
import com.cafe.backend.exception.BadRequestException;
import com.cafe.backend.exception.NotFoundException;
import com.cafe.backend.dto.category.CategoryRequest;
import com.cafe.backend.dto.category.CategoryResponse;
import com.cafe.backend.entity.Category;
import com.cafe.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {

        validate(request);

        Category category = new Category();

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category savedCategory = saveUnique(category);

        return mapToResponse(savedCategory);
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        return mapToResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {

        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Category not found"));

        validate(request);

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        Category updatedCategory = saveUnique(category);

        return mapToResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id) {

        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("CATEGORY_NOT_FOUND", "Category not found");
        }

        try {
            categoryRepository.deleteById(id);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "CATEGORY_IN_USE",
                    "This category still has menu items. Move or delete them first, "
                            + "or hide the category instead."
            );
        }
    }

    private void validate(CategoryRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("NAME_REQUIRED", "Category name is required");
        }
    }

    // Category names are unique in the database; turn that into a clear message
    private Category saveUnique(Category category) {
        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("CATEGORY_EXISTS", "A category with this name already exists");
        }
    }

    private CategoryResponse mapToResponse(Category category) {

        CategoryResponse response = new CategoryResponse();

        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setImageUrl(category.getImageUrl());
        response.setActive(category.isActive());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());

        return response;
    }
}