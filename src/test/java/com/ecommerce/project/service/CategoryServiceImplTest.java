package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.PaginationValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @InjectMocks
    CategoryServiceImpl categoryService;

    @Mock
    PaginationValidator paginationValidator;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    ModelMapper modelMapper;

    /// getAllCategories()
    @Test
    void getAllCategoriesShouldSuccessfullyReturnAllCategories() {
        Category category1 = new Category();
        category1.setCategoryId(1L);
        category1.setCategoryName("Books");

        Category category2 = new Category();
        category2.setCategoryId(2L);
        category2.setCategoryName("Electronics");

        List<Category> categories = List.of(category1, category2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> categoryPage = new PageImpl<>(categories, pageable, categories.size());

        CategoryDTO categoryDTO1 = new CategoryDTO();
        categoryDTO1.setCategoryId(category1.getCategoryId());
        categoryDTO1.setCategoryName(category1.getCategoryName());

        CategoryDTO categoryDTO2 = new CategoryDTO();
        categoryDTO2.setCategoryId(category2.getCategoryId());
        categoryDTO2.setCategoryName(category2.getCategoryName());

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(categoryRepository.findAll(any(Pageable.class)))
                .thenReturn(categoryPage);

        when(modelMapper.map(category1, CategoryDTO.class))
                .thenReturn(categoryDTO1);

        when(modelMapper.map(category2, CategoryDTO.class))
                .thenReturn(categoryDTO2);

        CategoryResponse result = categoryService.getAllCategories(0, 10, "categoryId", "asc");

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertEquals(10, result.getPageSize());
        assertTrue(result.isLastPage());

        CategoryDTO returnedDTO1 = result.getContent().getFirst();
        assertEquals(category1.getCategoryId(), returnedDTO1.getCategoryId());
        assertEquals(category1.getCategoryName(), returnedDTO1.getCategoryName());

        CategoryDTO returnedDTO2 = result.getContent().get(1);
        assertEquals(category2.getCategoryId(), returnedDTO2.getCategoryId());
        assertEquals(category2.getCategoryName(), returnedDTO2.getCategoryName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(categoryRepository).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();

        assertEquals(0, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());

        Sort.Order sortOrder = captured.getSort().iterator().next();
        assertEquals("categoryId", sortOrder.getProperty());
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());

        verify(paginationValidator).validate(eq(0), eq(10), eq("categoryId"), eq("asc"), anyList());
        verify(modelMapper).map(category1, CategoryDTO.class);
        verify(modelMapper).map(category2, CategoryDTO.class);
    }

    @Test
    void getAllCategoriesShouldReturnEmptyResponseWhenNoCategoryExist() {
        Page<Category> emptyPage = Page.empty();

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(categoryRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        CategoryResponse result = categoryService.getAllCategories(0, 10, "categoryId", "asc");

        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getTotalElements());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("categoryId"), eq("asc"), anyList());
        verify(categoryRepository).findAll(any(Pageable.class));
        verify(modelMapper, never()).map(any(Category.class), eq(CategoryDTO.class));
    }

    /// createCategory()
    @Test
    void createCategoryShouldCreateCategory() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryName("Books");

        Category category = new Category();
        category.setCategoryName(categoryDTO.getCategoryName().trim().toLowerCase());

        Category savedCategory = new Category();
        savedCategory.setCategoryName(category.getCategoryName());
        savedCategory.setCategoryId(1L);

        CategoryDTO savedCategoryDTO = new CategoryDTO();
        savedCategoryDTO.setCategoryId(savedCategory.getCategoryId());
        savedCategoryDTO.setCategoryName(savedCategory.getCategoryName());

        when(categoryRepository.existsByCategoryName("books"))
                .thenReturn(false);

        when(modelMapper.map(categoryDTO, Category.class))
                .thenReturn(category);

        when(categoryRepository.save(category))
                .thenReturn(savedCategory);

        when(modelMapper.map(savedCategory, CategoryDTO.class))
                .thenReturn(savedCategoryDTO);

        CategoryDTO result = categoryService.createCategory(categoryDTO);

        assertNotNull(result);
        assertEquals(1L, result.getCategoryId());
        assertEquals("books", result.getCategoryName());

        assertEquals("books", categoryDTO.getCategoryName());

        verify(categoryRepository).existsByCategoryName("books");
        verify(modelMapper).map(categoryDTO, Category.class);
        verify(categoryRepository).save(category);
        verify(modelMapper).map(savedCategory, CategoryDTO.class);
    }

    @Test
    void createCategoryShouldThrowApiExceptionIfAnotherCategoryAlreadyExistsWithSameName() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryName("Books");

        when(categoryRepository.existsByCategoryName("books"))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> categoryService.createCategory(categoryDTO)
        );

        assertEquals("Category with the name: '" + categoryDTO.getCategoryName().trim().toLowerCase() + "' already exists !!!", exception.getMessage());

        verify(categoryRepository).existsByCategoryName("books");
        verify(categoryRepository, never()).save(any(Category.class));
        verifyNoInteractions(modelMapper);
    }

    /// deleteCategory()
    @Test
    void deleteCategoryShouldDeleteCategoryIfNoProductExistForThatCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Books");

        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryId(category.getCategoryId());
        categoryDTO.setCategoryName(category.getCategoryName());

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(productRepository.existsByCategory(category))
                .thenReturn(false);

        doNothing().when(categoryRepository).delete(category);

        when(modelMapper.map(category, CategoryDTO.class))
                .thenReturn(categoryDTO);

        CategoryDTO result = categoryService.deleteCategory(1L);

        assertNotNull(result);
        assertEquals(category.getCategoryId(), result.getCategoryId());
        assertEquals(category.getCategoryName(), result.getCategoryName());

        verify(categoryRepository).findById(1L);
        verify(productRepository).existsByCategory(category);
        verify(categoryRepository).delete(category);
        verify(modelMapper).map(category, CategoryDTO.class);
    }

    @Test
    void deleteCategoryShouldThrowResourceNotFoundExceptionIfCategoryDoesNotExist() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(1L)
        );

        assertEquals("Category not found with categoryId: 1", exception.getMessage());

        verify(categoryRepository).findById(1L);
        verifyNoInteractions(productRepository);
        verify(categoryRepository, never()).delete(any(Category.class));
        verifyNoInteractions(modelMapper);
    }

    @Test
    void deleteCategoryShouldThrowApiExceptionIfAProductBelongsToThatCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Books");

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(productRepository.existsByCategory(category))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> categoryService.deleteCategory(1L)
        );

        assertEquals("Cannot delete category with existing products", exception.getMessage());

        verify(categoryRepository).findById(1L);
        verify(productRepository).existsByCategory(category);
        verify(categoryRepository, never()).delete(any(Category.class));
        verifyNoInteractions(modelMapper);
    }

    /// updateCategory()
    @Test
    void updateCategoryShouldSuccessfullyUpdateCategory() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryName("Electronics");

        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("books");

        CategoryDTO updatedCategoryDTO = new CategoryDTO();
        updatedCategoryDTO.setCategoryId(1L);
        updatedCategoryDTO.setCategoryName("electronics");

        when(categoryRepository.existsByCategoryNameAndCategoryIdNot("electronics", 1L))
                .thenReturn(false);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(modelMapper.map(category, CategoryDTO.class))
                .thenReturn(updatedCategoryDTO);

        CategoryDTO result = categoryService.updateCategory(categoryDTO, 1L);

        assertNotNull(result);
        assertEquals(categoryDTO.getCategoryName().trim().toLowerCase(), result.getCategoryName());
        assertEquals("electronics", category.getCategoryName());

        verify(categoryRepository).existsByCategoryNameAndCategoryIdNot("electronics", 1L);
        verify(categoryRepository).findById(1L);
        verify(modelMapper).map(category, CategoryDTO.class);
    }

    @Test
    void updateCategoryShouldThrowApiExceptionIfAnotherCategoryAlreadyHasNewCategoryName() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryName("Electronics");

        when(categoryRepository.existsByCategoryNameAndCategoryIdNot("electronics", 1L))
                .thenReturn(true);

        APIException exception = assertThrows(
                APIException.class,
                () -> categoryService.updateCategory(categoryDTO, 1L)
        );

        assertEquals("Category with the name: '" + categoryDTO.getCategoryName().trim().toLowerCase() + "' already exists !!!", exception.getMessage());

        verify(categoryRepository).existsByCategoryNameAndCategoryIdNot("electronics", 1L);
        verify(categoryRepository, never()).findById(any(Long.class));
        verifyNoInteractions(modelMapper);
    }

    @Test
    void updateCategoryShouldThrowResourceNotFoundExceptionIfCategoryDoesNotExist() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryName("Electronics");

        when(categoryRepository.existsByCategoryNameAndCategoryIdNot("electronics", 1L))
                .thenReturn(false);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.updateCategory(categoryDTO, 1L)
        );

        assertEquals("Category not found with categoryId: 1", exception.getMessage());

        verify(categoryRepository).existsByCategoryNameAndCategoryIdNot("electronics", 1L);
        verify(categoryRepository).findById(1L);
        verifyNoInteractions(modelMapper);
    }
}
