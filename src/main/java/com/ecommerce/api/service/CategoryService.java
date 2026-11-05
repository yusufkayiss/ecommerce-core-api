package com.ecommerce.api.service;

import com.ecommerce.api.dto.request.CategoryCreateRequest;
import com.ecommerce.api.dto.response.CategoryResponse;
import com.ecommerce.api.dto.request.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    CategoryResponse updateCategory(Long id, CategoryUpdateRequest request);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getMainCategories();

    void deleteCategory(Long id);
}