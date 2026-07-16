package com.ecommerce.project.integration;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashSet;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductGetControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    /// getAllProducts()
    @Test
    void getAllProductsShouldReturnAllProducts() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        mockMvc.perform(get("/api/public/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].productId").isNumber())
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.content[0].image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.content[0].description").value(savedProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].quantity").value(savedProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].discount").value(savedProduct.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.content[0].specialPrice").value(savedProduct.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllProductsShouldReturnEmptyWhenNoProductsExist() throws Exception {
        mockMvc.perform(get("/api/public/products"))
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
    void getAllProductsShouldFilterByKeyword() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "The Lord of the Rings", "default.png",
                "The Lord of the Rings: The Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product2);

        mockMvc.perform(get("/api/public/products")
                        .param("keyword", "har"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].productId").isNumber())
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[0].image").value("http://localhost:8080/images/" + savedProduct1.getImage()))
                .andExpect(jsonPath("$.content[0].description").value(savedProduct1.getDescription()))
                .andExpect(jsonPath("$.content[0].quantity").value(savedProduct1.getQuantity()))
                .andExpect(jsonPath("$.content[0].price").value(savedProduct1.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].discount").value(savedProduct1.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.content[0].specialPrice").value(savedProduct1.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, productRepository.count());
    }

    @Test
    void getAllProductsShouldFilterByCategory() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category1 = createCategory("books");
        Category savedCategory1 = categoryRepository.save(category1);

        Category category2 = createCategory("electronics");
        Category savedCategory2 = categoryRepository.save(category2);

        Product product1 = createProduct(savedUser, savedCategory1, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory2, "iPhone 16", "default.png",
                "Brand new iPhone 16", 10, new BigDecimal("500"), new BigDecimal("10"), new BigDecimal("450"));
        productRepository.save(product2);

        mockMvc.perform(get("/api/public/products")
                        .param("category", "books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].productId").isNumber())
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[0].image").value("http://localhost:8080/images/" + savedProduct1.getImage()))
                .andExpect(jsonPath("$.content[0].description").value(savedProduct1.getDescription()))
                .andExpect(jsonPath("$.content[0].quantity").value(savedProduct1.getQuantity()))
                .andExpect(jsonPath("$.content[0].price").value(savedProduct1.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].discount").value(savedProduct1.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.content[0].specialPrice").value(savedProduct1.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, categoryRepository.count());
        assertEquals(2, productRepository.count());
    }

    @Test
    void getAllProductsShouldFilterByKeywordAndCategory() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category1 = createCategory("books");
        Category savedCategory1 = categoryRepository.save(category1);

        Category category2 = createCategory("electronics");
        Category savedCategory2 = categoryRepository.save(category2);

        Product product1 = createProduct(savedUser, savedCategory1, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory2, "iPhone 16", "default.png",
                "Brand new iPhone 16", 10, new BigDecimal("500"), new BigDecimal("10"), new BigDecimal("450"));
        productRepository.save(product2);

        Product product3 = createProduct(savedUser, savedCategory1, "The Lord of the Rings", "default.png",
                "The Lord of the Rings: The Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product3);

        mockMvc.perform(get("/api/public/products")
                        .param("keyword", "har")
                        .param("category", "books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].productId").isNumber())
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[0].image").value("http://localhost:8080/images/" + savedProduct1.getImage()))
                .andExpect(jsonPath("$.content[0].description").value(savedProduct1.getDescription()))
                .andExpect(jsonPath("$.content[0].quantity").value(savedProduct1.getQuantity()))
                .andExpect(jsonPath("$.content[0].price").value(savedProduct1.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].discount").value(savedProduct1.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.content[0].specialPrice").value(savedProduct1.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, categoryRepository.count());
        assertEquals(3, productRepository.count());
    }

    @Test
    void getAllProductsShouldReturnEmptyWhenNoProductsMatchFilters() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category1 = createCategory("books");
        Category savedCategory1 = categoryRepository.save(category1);

        Category category2 = createCategory("electronics");
        Category savedCategory2 = categoryRepository.save(category2);

        Product product1 = createProduct(savedUser, savedCategory1, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory2, "iPhone 16", "default.png",
                "Brand new iPhone 16", 10, new BigDecimal("500"), new BigDecimal("10"), new BigDecimal("450"));
        productRepository.save(product2);

        Product product3 = createProduct(savedUser, savedCategory1, "The Lord of the Rings", "default.png",
                "The Lord of the Rings: The Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product3);

        mockMvc.perform(get("/api/public/products")
                        .param("keyword", "cri")
                        .param("category", "toys"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, categoryRepository.count());
        assertEquals(3, productRepository.count());
    }

    @Test
    void getAllProductsShouldReturnBadRequestWhenPaginationParametersAreInvalid() throws Exception {
        mockMvc.perform(get("/api/public/products")
                        .param("pageNumber", "0")
                        .param("pageSize", "101")
                        .param("sortBy", "productId")
                        .param("sortOrder", "asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getAllProductsShouldSupportPagination() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "iPhone 16", "default.png",
                "Brand new iPhone 16", 10, new BigDecimal("500"), new BigDecimal("10"), new BigDecimal("450"));
        productRepository.save(product2);

        Product product3 = createProduct(savedUser, savedCategory, "The Lord of the Rings", "default.png",
                "The Lord of the Rings: The Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct3 = productRepository.save(product3);

        mockMvc.perform(get("/api/public/products")
                        .param("pageNumber", "1")
                        .param("pageSize", "2")
                        .param("sortBy", "productId")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").value(savedProduct3.getProductId()))
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct3.getProductName()))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    /// getProductsByCategory()
    @Test
    void getProductsByCategoryShouldReturnProductsByCategory() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category1 = createCategory("books");
        Category savedCategory1 = categoryRepository.save(category1);

        Category category2 = createCategory("electronics");
        Category savedCategory2 = categoryRepository.save(category2);

        Product product1 = createProduct(savedUser, savedCategory1, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory2, "iPhone 16", "default.png",
                "Brand new iPhone 16", 10, new BigDecimal("500"), new BigDecimal("10"), new BigDecimal("450"));
        productRepository.save(product2);

        Product product3 = createProduct(savedUser, savedCategory2, "Samsung Galaxy S26", "default.png",
                "Brand new Samsung Galaxy S26", 10, new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product3);

        Long categoryId = savedCategory1.getCategoryId();

        mockMvc.perform(get("/api/public/categories/{categoryId}/products", categoryId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").isNumber())
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[0].image").value("http://localhost:8080/images/" + savedProduct1.getImage()))
                .andExpect(jsonPath("$.content[0].description").value(savedProduct1.getDescription()))
                .andExpect(jsonPath("$.content[0].quantity").value(savedProduct1.getQuantity()))
                .andExpect(jsonPath("$.content[0].price").value(savedProduct1.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].discount").value(savedProduct1.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.content[0].specialPrice").value(savedProduct1.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, categoryRepository.count());
        assertEquals(3, productRepository.count());
    }

    @Test
    void getProductsByCategoryShouldReturnEmptyWhenCategoryHasNoProducts() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(get("/api/public/categories/{categoryId}/products", categoryId))
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
    void getProductsByCategoryShouldReturnNotFoundIfCategoryDoesNotExist() throws Exception {
        Long categoryId = Long.MAX_VALUE;

        mockMvc.perform(get("/api/public/categories/{categoryId}/products", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category not found with categoryId: " + Long.MAX_VALUE))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getProductsByCategoryShouldReturnBadRequestWhenPaginationParametersAreInvalid() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product);

        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(get("/api/public/categories/{categoryId}/products", categoryId)
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
    void getProductsByCategoryShouldSupportPagination() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "The Lord of the Rings", "default.png",
                "The Lord of the Rings: The Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product2);

        Product product3 = createProduct(savedUser, savedCategory, "Mahabharata", "default.png",
                "Mahabharata: The epic", 10, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        Product savedProduct3 = productRepository.save(product3);

        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(get("/api/public/categories/{categoryId}/products", categoryId)
                        .param("pageNumber", "1")
                        .param("pageSize", "2")
                        .param("sortBy", "productId")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").value(savedProduct3.getProductId()))
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct3.getProductName()))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    /// getProductsByKeyword()
    @Test
    void getProductsByKeywordShouldReturnProductsByKeyword() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "The Lord of the Rings", "default.png",
                "The Lord of the Rings: The Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product2);

        mockMvc.perform(get("/api/public/products/keyword")
                        .param("keyword", "har"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").isNumber())
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[0].image").value("http://localhost:8080/images/" + savedProduct1.getImage()))
                .andExpect(jsonPath("$.content[0].description").value(savedProduct1.getDescription()))
                .andExpect(jsonPath("$.content[0].quantity").value(savedProduct1.getQuantity()))
                .andExpect(jsonPath("$.content[0].price").value(savedProduct1.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].discount").value(savedProduct1.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.content[0].specialPrice").value(savedProduct1.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, productRepository.count());
    }

    @Test
    void getProductsByKeywordShouldReturnEmptyWhenNoProductNameMatchesKeyword() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "The Lord of the Rings", "default.png",
                "The Lord of the Rings: The Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product2);

        mockMvc.perform(get("/api/public/products/keyword")
                        .param("keyword", "mah"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(2, productRepository.count());
    }

    @Test
    void getProductsByKeywordShouldReturnBadRequestWhenPaginationParametersAreInvalid() throws Exception {
        mockMvc.perform(get("/api/public/products/keyword")
                        .param("keyword", "har")
                        .param("pageNumber", "1")
                        .param("pageSize", "0")
                        .param("sortBy", "categoryId")
                        .param("sortOrder", "desc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Page size must be between 1 and 100"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getProductsByKeywordShouldSupportPagination() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "Harry Potter 4", "default.png",
                "Harry Potter and the Goblet Of Fire", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product2);

        Product product3 = createProduct(savedUser, savedCategory, "Harry Potter 5", "default.png",
                "Harry Potter and the Order of the Phoenix", 10,
                new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        Product savedProduct3 = productRepository.save(product3);

        mockMvc.perform(get("/api/public/products/keyword")
                        .param("keyword", "har")
                        .param("pageNumber", "1")
                        .param("pageSize", "2")
                        .param("sortBy", "productId")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").value(savedProduct3.getProductId()))
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct3.getProductName()))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.lastPage").value(true));

        assertEquals(3, productRepository.count());
    }

    /// getAllProductsForAdmin()
    @Test
    void getAllProductsForAdminShouldSuccessfullyReturnAllProductsForAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        mockMvc.perform(get("/api/admin/products")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].productId").isNumber())
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.content[0].image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.content[0].description").value(savedProduct.getDescription()))
                .andExpect(jsonPath("$.content[0].quantity").value(savedProduct.getQuantity()))
                .andExpect(jsonPath("$.content[0].price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].discount").value(savedProduct.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.content[0].specialPrice").value(savedProduct.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllProductsForAdminShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product);

        mockMvc.perform(get("/api/admin/products")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAllProductsForAdminShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product);

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getAllProductsForSeller()
    @Test
    void getAllProductsForSellerShouldReturnOnlyLoggedInSellersProducts() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller1 = createUser("Test Seller 1", "seller1@gmail.com", "password");
        seller1.getRoles().add(savedRole);
        User savedSeller1 = userRepository.save(seller1);

        UserDetailsImpl userDetails1 = UserDetailsImpl.build(savedSeller1);

        User seller2 = createUser("Test User 2", "seller2@gmail.com", "password");
        seller2.getRoles().add(savedRole);
        User savedSeller2 = userRepository.save(seller2);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedSeller1, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedSeller2, savedCategory, "Mahabharata", "default.png",
                "Mahabharata: The Epic", 10, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        Product savedProduct2 = productRepository.save(product2);

        Product product3 = createProduct(savedSeller1, savedCategory, "Harry Potter 6", "default.png",
                "Harry Potter and the Half Blood Prince", 15,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct3 = productRepository.save(product3);

        mockMvc.perform(get("/api/seller/products")
                        .with(user(userDetails1)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[1].productName").value(savedProduct3.getProductName()))
                .andExpect(jsonPath("$.content[*].productName", not(hasItem(savedProduct2.getProductName()))))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllProductsForSellerShouldReturnEmptyListWhenSellerHasNoProducts() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller1 = createUser("Test Seller 1", "seller1@gmail.com", "password");
        seller1.getRoles().add(savedRole);
        User savedSeller1 = userRepository.save(seller1);

        UserDetailsImpl userDetails1 = UserDetailsImpl.build(savedSeller1);

        User seller2 = createUser("Test User 2", "seller2@gmail.com", "password");
        seller2.getRoles().add(savedRole);
        User savedSeller2 = userRepository.save(seller2);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedSeller2, savedCategory, "Mahabharata", "default.png",
                "Mahabharata: The Epic", 10, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        productRepository.save(product);

        mockMvc.perform(get("/api/seller/products")
                        .with(user(userDetails1)))
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
    void getAllProductsForSellerShouldReturnForbiddenIfUserIsNotSeller() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/seller/products")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAllProductsForSellerShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/seller/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// HELPERS
    private User createUser(String userName, String email, String password) {
        User user = new User();
        user.setUserName(userName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(new HashSet<>());
        return user;
    }

    private Category createCategory(String categoryName) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        return category;
    }

    private Product createProduct(User user, Category category, String productName, String image, String description, Integer quantity, BigDecimal price, BigDecimal discount, BigDecimal specialPrice) {
        Product product = new Product();
        product.setProductName(productName);
        product.setImage(image);
        product.setDescription(description);
        product.setQuantity(quantity);
        product.setPrice(price);
        product.setDiscount(discount);
        product.setSpecialPrice(specialPrice);
        product.setCategory(category);
        product.setUser(user);
        return product;
    }
}
