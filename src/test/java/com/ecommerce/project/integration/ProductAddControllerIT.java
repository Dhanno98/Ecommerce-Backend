package com.ecommerce.project.integration;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CreateProductRequest;
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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductAddControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    /// addProduct()
    @Test
    void addProductShouldAddProduct() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 3", "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productName").value(productRequest.getProductName()))
                .andExpect(jsonPath("$.description").value(productRequest.getDescription()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(productRequest.getDiscount().doubleValue()));

        assertEquals(1, productRepository.count());

        Product productFromDB = productRepository.findAll().getFirst();
        assertNotNull(productFromDB);
        assertEquals(savedUser, productFromDB.getUser());
        assertEquals(savedCategory, productFromDB.getCategory());
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice(), productFromDB.getPrice());
        assertEquals(productRequest.getDiscount(), productFromDB.getDiscount());
        assertEquals(new BigDecimal("90.00"), productFromDB.getSpecialPrice());
        assertEquals("default.png", productFromDB.getImage());
    }

    @Test
    void addProductShouldReturnBadRequestIfProductRequestIsInvalid() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 3", "Harry Potter and the Prisoner of Azkaban", -10,
                new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.quantity").value("Product quantity cannot be negative"));

        assertEquals(0, productRepository.count());
    }

    @Test
    void addProductShouldReturnNotFoundIfCategoryDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 3", "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = Long.MAX_VALUE;

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category not found with categoryId: " + categoryId))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(0, productRepository.count());
    }

    @Test
    void addProductShouldReturnBadRequestIfProductAlreadyExistsWithThatProductName() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "harry-potter.png",
                "Harry Potter and the Half Blood Prince", 20,
                new BigDecimal("100"), new BigDecimal("5"), new BigDecimal("95"));
        productRepository.save(product);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 3", "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product with name: " + productRequest.getProductName() + " already exists."))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(1, productRepository.count());
        Product productFromDB = productRepository.findAll().getFirst();
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertNotEquals(productRequest.getDescription(), productFromDB.getDescription());
    }

    @Test
    void addProductShouldReturnForbiddenWhenUserIsNotAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 3", "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(0, productRepository.count());
    }

    @Test
    void addProductShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 3", "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));

        assertEquals(0, productRepository.count());
    }

    /// addProductSeller()
    @Test
    void addProductSellerShouldAddProductIfUserIsSeller() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(savedRole);
        User savedSeller = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 3", "Harry Potter and the Prisoner of Azkaban",
                10, new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/seller/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productName").value(productRequest.getProductName()))
                .andExpect(jsonPath("$.description").value(productRequest.getDescription()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(productRequest.getDiscount().doubleValue()));

        assertEquals(1, productRepository.count());

        Product productFromDB = productRepository.findAll().getFirst();
        assertNotNull(productFromDB);
        assertEquals(savedSeller, productFromDB.getUser());
        assertEquals(savedCategory, productFromDB.getCategory());
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice(), productFromDB.getPrice());
        assertEquals(productRequest.getDiscount(), productFromDB.getDiscount());
        assertEquals(new BigDecimal("90.00"), productFromDB.getSpecialPrice());
        assertEquals("default.png", productFromDB.getImage());
    }

    @Test
    void addProductSellerShouldAddProductIfUserIsAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 3", "Harry Potter and the Prisoner of Azkaban",
                10, new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/seller/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productName").value(productRequest.getProductName()))
                .andExpect(jsonPath("$.description").value(productRequest.getDescription()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(productRequest.getDiscount().doubleValue()));

        assertEquals(1, productRepository.count());

        Product productFromDB = productRepository.findAll().getFirst();
        assertNotNull(productFromDB);
        assertEquals(savedUser, productFromDB.getUser());
        assertEquals(savedCategory, productFromDB.getCategory());
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice(), productFromDB.getPrice());
        assertEquals(productRequest.getDiscount(), productFromDB.getDiscount());
        assertEquals(new BigDecimal("90.00"), productFromDB.getSpecialPrice());
        assertEquals("default.png", productFromDB.getImage());
    }

    @Test
    void addProductSellerShouldReturnNotFoundIfCategoryDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(savedRole);
        User savedSeller = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 3", "Harry Potter and the Prisoner of Azkaban",
                10, new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = Long.MAX_VALUE;

        mockMvc.perform(post("/api/seller/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category not found with categoryId: " + categoryId))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(0, productRepository.count());
    }

    @Test
    void addProductSellerShouldReturnBadRequestIfProductAlreadyExistsWithThatProductName() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(savedRole);
        User savedSeller = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedSeller, savedCategory, "Harry Potter 3", "harry-potter.png",
                "Harry Potter and the Half Blood Prince", 20,
                new BigDecimal("100"), new BigDecimal("5"), new BigDecimal("95"));
        productRepository.save(product);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 3", "Harry Potter and the Prisoner of Azkaban",
                10, new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/seller/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product with name: " + productRequest.getProductName() + " already exists."))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(1, productRepository.count());
        Product productFromDB = productRepository.findAll().getFirst();
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertNotEquals(productRequest.getDescription(), productFromDB.getDescription());
    }

    @Test
    void addProductSellerShouldReturnForbiddenWhenUserIsNeitherSellerNotAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 3", "Harry Potter and the Prisoner of Azkaban",
                10, new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/seller/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        assertEquals(0, productRepository.count());
    }

    @Test
    void addProductSellerShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 3", "Harry Potter and the Prisoner of Azkaban",
                10, new BigDecimal("100"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long categoryId = savedCategory.getCategoryId();

        mockMvc.perform(post("/api/seller/categories/{categoryId}/product", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));

        assertEquals(0, productRepository.count());
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

    private CreateProductRequest createProductRequest(String productName, String description, Integer quantity, BigDecimal price, BigDecimal discount) {
        CreateProductRequest productRequest = new CreateProductRequest();
        productRequest.setProductName(productName);
        productRequest.setDescription(description);
        productRequest.setQuantity(quantity);
        productRequest.setPrice(price);
        productRequest.setDiscount(discount);
        return productRequest;
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
