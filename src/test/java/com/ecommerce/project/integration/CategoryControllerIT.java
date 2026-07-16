package com.ecommerce.project.integration;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    /// getAllCategories()
    @Test
    void getAllCategoriesShouldReturnAllCategories() throws Exception {
        Category category = createCategory("books");
        categoryRepository.save(category);

        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].categoryName").value("books"))
                .andExpect(jsonPath("$.content[0].categoryId").isNumber())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllCategoriesShouldReturnEmptyWhenNoCategoryExists() throws Exception {
        mockMvc.perform(get("/api/public/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllCategoriesShouldReturnBadRequestWhenPaginationParametersAreInvalid() throws Exception {
        Category category = createCategory("books");
        categoryRepository.save(category);

        mockMvc.perform(get("/api/public/categories")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("sortBy", "categoryId")
                        .param("sortOrder", "dec"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid sort order"))
                .andExpect(jsonPath("$.status").value(false));
    }

    /// createCategory()
    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategoryShouldCreateCategory() throws Exception {
        CategoryDTO categoryDTO = createCategoryDTO("Books");

        String json = objectMapper.writeValueAsString(categoryDTO);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categoryId").isNumber())
                .andExpect(jsonPath("$.categoryName").value("books"));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findAll().getFirst();
        assertEquals("books", categoryInDB.getCategoryName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategoryShouldReturnBadRequestIfCategoryNameAlreadyExists() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CategoryDTO categoryDTO = createCategoryDTO("Books");

        String json = objectMapper.writeValueAsString(categoryDTO);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category with the name: 'books' already exists !!!"));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(savedCategory.getCategoryId()).orElseThrow();
        assertEquals("books", categoryInDB.getCategoryName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategoryShouldReturnBadRequestWhenCategoryNameIsBlank() throws Exception {
        CategoryDTO categoryDTO = new CategoryDTO();

        String json = objectMapper.writeValueAsString(categoryDTO);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categoryName").value("must not be blank"));

        assertEquals(0, categoryRepository.count());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCategoryShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        CategoryDTO categoryDTO = createCategoryDTO("Books");

        String json = objectMapper.writeValueAsString(categoryDTO);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(0, categoryRepository.count());
    }

    @Test
    void createCategoryShouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        CategoryDTO categoryDTO = createCategoryDTO("Books");

        String json = objectMapper.writeValueAsString(categoryDTO);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));

        assertEquals(0, categoryRepository.count());
    }

    /// deleteCategory()
    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategoryShouldDeleteCategory() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(delete("/api/admin/categories/{categoryId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categoryId").isNumber())
                .andExpect(jsonPath("$.categoryName").value(savedCategory.getCategoryName()));

        assertEquals(0, categoryRepository.count());
        assertFalse(categoryRepository.existsById(categoryId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategoryShouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        Long categoryId = Long.MAX_VALUE;

        mockMvc.perform(delete("/api/admin/categories/{categoryId}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category not found with categoryId: " + Long.MAX_VALUE))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategoryShouldReturnBadRequestWhenCategoryHasProducts() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);
        savedCategory.setProducts(new ArrayList<>());

        Product product = createProduct(savedCategory);
        savedCategory.getProducts().add(product);
        productRepository.save(product);

        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(delete("/api/admin/categories/{categoryId}", categoryId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cannot delete category with existing products"))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(savedCategory.getCategoryId()).orElseThrow();
        assertEquals(savedCategory.getCategoryName(), categoryInDB.getCategoryName());
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteCategoryShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(delete("/api/admin/categories/{categoryId}", categoryId))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(savedCategory.getCategoryId()).orElseThrow();
        assertEquals(savedCategory.getCategoryName(), categoryInDB.getCategoryName());
    }

    @Test
    void deleteCategoryShouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(delete("/api/admin/categories/{categoryId}", categoryId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(savedCategory.getCategoryId()).orElseThrow();
        assertEquals(savedCategory.getCategoryName(), categoryInDB.getCategoryName());
    }

    /// updateCategory()
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategoryShouldUpdateCategory() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CategoryDTO categoryDTO = createCategoryDTO("Electronics");

        String json = objectMapper.writeValueAsString(categoryDTO);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(put("/api/admin/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categoryId").isNumber())
                .andExpect(jsonPath("$.categoryName").value("electronics"));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(categoryId).orElseThrow();
        assertEquals("electronics", categoryInDB.getCategoryName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategoryShouldReturnBadRequestWhenCategoryNameToBeUpdatedIsBlank() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CategoryDTO categoryDTO = new CategoryDTO();

        String json = objectMapper.writeValueAsString(categoryDTO);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(put("/api/admin/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categoryName").value("must not be blank"));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(categoryId).orElseThrow();
        assertEquals(savedCategory.getCategoryName(), categoryInDB.getCategoryName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategoryShouldReturnBadRequestWhenAnotherCategoryHasTheSameCategoryName() throws Exception {
        Category category1 = createCategory("books");
        Category savedCategory = categoryRepository.save(category1);

        Category category2 = createCategory("electronics");
        categoryRepository.save(category2);

        CategoryDTO categoryDTO = createCategoryDTO("Electronics");

        String json = objectMapper.writeValueAsString(categoryDTO);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(put("/api/admin/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category with the name: 'electronics' already exists !!!"))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(2, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(categoryId).orElseThrow();
        assertEquals(savedCategory.getCategoryName(), categoryInDB.getCategoryName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategoryShouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
        CategoryDTO categoryDTO = createCategoryDTO("Electronics");

        String json = objectMapper.writeValueAsString(categoryDTO);
        Long categoryId = Long.MAX_VALUE;

        mockMvc.perform(put("/api/admin/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category not found with categoryId: " + Long.MAX_VALUE))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateCategoryShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CategoryDTO categoryDTO = createCategoryDTO("Electronics");

        String json = objectMapper.writeValueAsString(categoryDTO);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(put("/api/admin/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(categoryId).orElseThrow();
        assertEquals(savedCategory.getCategoryName(), categoryInDB.getCategoryName());
    }

    @Test
    void updateCategoryShouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CategoryDTO categoryDTO = createCategoryDTO("Electronics");

        String json = objectMapper.writeValueAsString(categoryDTO);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(put("/api/admin/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));

        assertEquals(1, categoryRepository.count());
        Category categoryInDB = categoryRepository.findById(categoryId).orElseThrow();
        assertEquals(savedCategory.getCategoryName(), categoryInDB.getCategoryName());
    }

    /// getAllCategoriesForAdmin()
    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCategoriesForAdminShouldReturnAllCategories() throws Exception {
        Category category = createCategory("books");
        categoryRepository.save(category);

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].categoryName").value("books"))
                .andExpect(jsonPath("$.content[0].categoryId").isNumber())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCategoriesForAdminShouldReturnEmptyWhenNoCategoryExists() throws Exception {
        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCategoriesForAdminShouldReturnBadRequestWhenPaginationParametersAreInvalid() throws Exception {
        Category category = createCategory("books");
        categoryRepository.save(category);

        mockMvc.perform(get("/api/admin/categories")
                        .param("pageNumber", "1")
                        .param("pageSize", "5")
                        .param("sortBy", "categoryId")
                        .param("sortOrder", "dec"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid sort order"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllCategoriesForAdminShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        Category category = createCategory("books");
        categoryRepository.save(category);

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(1, categoryRepository.count());
    }

    @Test
    void getAllCategoriesForAdminShouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        Category category = createCategory("books");
        categoryRepository.save(category);

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));

        assertEquals(1, categoryRepository.count());
    }

    /// getAllCategoriesUnpaginated()
    @Test
    void getAllCategoriesUnpaginatedShouldReturnAllCategoriesWithoutPagination() throws Exception {
        Category category1 = createCategory("books");
        Category savedCategory1 = categoryRepository.save(category1);

        Category category2 = createCategory("electronics");
        Category savedCategory2 = categoryRepository.save(category2);

        mockMvc.perform(get("/api/public/categories/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].categoryId").value(savedCategory1.getCategoryId()))
                .andExpect(jsonPath("$[0].categoryName").value(savedCategory1.getCategoryName()))
                .andExpect(jsonPath("$[1].categoryId").value(savedCategory2.getCategoryId()))
                .andExpect(jsonPath("$[1].categoryName").value(savedCategory2.getCategoryName()));
    }

    @Test
    void getAllCategoriesUnpaginatedShouldReturnEmptyCategoriesWithoutPagination() throws Exception {
        mockMvc.perform(get("/api/public/categories/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isEmpty());
    }

    /// HELPERS
    private Category createCategory(String categoryName) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        return category;
    }

    private CategoryDTO createCategoryDTO(String categoryName) {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryName(categoryName);
        return categoryDTO;
    }

    private Product createProduct(Category category) {
        Product product = new Product();
        product.setProductName("Harry Potter 3");
        product.setDescription("Harry Potter and the Prizoner of Azkaban");
        product.setCategory(category);
        product.setImage("default.png");
        product.setQuantity(10);
        product.setPrice(new BigDecimal("50"));
        product.setDiscount(new BigDecimal("10"));
        product.setSpecialPrice(new BigDecimal("45"));
        return product;
    }
}
