package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.PaginationValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private final ProductRepository productRepository;

    private final ModelMapper modelMapper;

    private final PaginationValidator paginationValidator;

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("categoryId", "categoryName");

    @Override
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Category> categoryPage = categoryRepository.findAll(pageDetails);

        List<Category> categories = categoryPage.getContent();

        List<CategoryDTO> categoryDTOS = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();

        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(categoryPage.getNumber());
        categoryResponse.setPageSize(categoryPage.getSize());
        categoryResponse.setTotalElements(categoryPage.getTotalElements());
        categoryResponse.setTotalPages(categoryPage.getTotalPages());
        categoryResponse.setLastPage(categoryPage.isLast());
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        String normalizedCategoryName = categoryDTO.getCategoryName().trim().toLowerCase();

        log.info("Category creation requested. categoryName={}", normalizedCategoryName);

        if (categoryRepository.existsByCategoryName(normalizedCategoryName)) {
            log.warn("Category creation failed. Category already exists: {}", normalizedCategoryName);
            throw new APIException("Category with the name: '" + normalizedCategoryName + "' already exists !!!");
        }

        categoryDTO.setCategoryName(normalizedCategoryName);
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category savedCategory = categoryRepository.save(category);

        log.info("Category created successfully. categoryId={}, categoryName={}",
                savedCategory.getCategoryId(), savedCategory.getCategoryName());
        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        log.info("Category deletion requested. categoryId={}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        if (productRepository.existsByCategory(category)) {
            log.warn("Category deletion blocked. categoryId={} contains products", categoryId);
            throw new APIException("Cannot delete category with existing products");
        }

        categoryRepository.delete(category);

        log.info("Category deleted successfully. categoryId={}, categoryName={}",
                category.getCategoryId(), category.getCategoryName());
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Transactional
    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
        String categoryName = categoryDTO.getCategoryName().trim().toLowerCase();

        log.info("Category update requested. categoryId={}, newName={}", categoryId, categoryName);

        if (categoryRepository.existsByCategoryNameAndCategoryIdNot(categoryName, categoryId)) {
            log.warn("Category update failed. Name already exists: {}", categoryName);
            throw new APIException("Category with the name: '" + categoryName + "' already exists !!!");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        category.setCategoryName(categoryName);

        log.info("Category updated successfully. categoryId={}, categoryName={}",
                category.getCategoryId(), category.getCategoryName());
        return modelMapper.map(category, CategoryDTO.class);
    }
}
