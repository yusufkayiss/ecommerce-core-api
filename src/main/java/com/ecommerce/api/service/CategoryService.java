package com.ecommerce.api.service;

import com.ecommerce.api.dto.CategoryCreateRequest;
import com.ecommerce.api.dto.CategoryResponse;
import com.ecommerce.api.dto.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    CategoryResponse updateCategory(Long id, CategoryUpdateRequest request);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getMainCategories();

    void deleteCategory(Long id);
}