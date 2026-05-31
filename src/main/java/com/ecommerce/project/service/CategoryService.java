package com.ecommerce.project.service;

import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import jakarta.transaction.Transactional;

public interface CategoryService {
    CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    CategoryDTO createCategory(CategoryDTO categoryDTO);

    CategoryDTO deleteCategory(Long categoryId);

    @Transactional
    CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId);

    CategoryResponse getAllCategoriesForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
}
