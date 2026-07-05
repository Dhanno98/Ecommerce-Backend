package com.ecommerce.project.integration;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductImageControllerIT {

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

    @Value("${project.image}")
    private String uploadPath;

    /// updateProductImage()
    @Test
    void updateProductImageShouldSuccessfullyUpdateProductImage() throws Exception {
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

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        Long productId = savedProduct.getProductId();

        String uploadedFile = null;

        try {
            mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                            .file(image)
                            .with(user(userDetails))
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.productId").value(productId))
                    .andExpect(jsonPath("$.productName").value(savedProduct.getProductName()))
                    .andExpect(jsonPath("$.image").isNotEmpty())
                    .andExpect(jsonPath("$.image").value(startsWith("http://localhost:8080/images/")))
                    .andExpect(jsonPath("$.image", containsString(".png")))
                    .andExpect(jsonPath("$.description").value(savedProduct.getDescription()))
                    .andExpect(jsonPath("$.quantity").value(savedProduct.getQuantity()))
                    .andExpect(jsonPath("$.price").value(savedProduct.getPrice().doubleValue()))
                    .andExpect(jsonPath("$.discount").value(savedProduct.getDiscount().doubleValue()))
                    .andExpect(jsonPath("$.specialPrice").value(savedProduct.getSpecialPrice().doubleValue()));

            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertNotEquals("default.png", updatedProduct.getImage());
            assertTrue(updatedProduct.getImage().endsWith(".png"));
            assertNotEquals(image.getOriginalFilename(), updatedProduct.getImage());

            assertEquals(productId, updatedProduct.getProductId());
            assertEquals(savedProduct.getProductName(), updatedProduct.getProductName());
            assertEquals(savedProduct.getDescription(), updatedProduct.getDescription());
            assertEquals(savedProduct.getPrice(), updatedProduct.getPrice());
            assertEquals(savedProduct.getDiscount(), updatedProduct.getDiscount());
            assertEquals(savedProduct.getSpecialPrice(), updatedProduct.getSpecialPrice());
            assertEquals(savedProduct.getQuantity(), updatedProduct.getQuantity());

            assertTrue(Files.exists(Paths.get(uploadPath, updatedProduct.getImage())));

            uploadedFile = updatedProduct.getImage();
        } finally {
            if (uploadedFile != null) {
                Files.deleteIfExists(Paths.get(uploadPath, uploadedFile));
            }
        }
    }

    @Test
    void updateProductImageShouldReturnBadRequestIfImageIsEmpty() throws Exception {
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

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "".getBytes());

        Long productId = savedProduct.getProductId();

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Image cannot be empty"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageShouldReturnBadRequestIfImageContentTypeIsInvalid() throws Exception {
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

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/svg", "dummy image data".getBytes());

        Long productId = savedProduct.getProductId();

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid image type. Valid image types: PNG, JPEG, WEBP"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageShouldReturnBadRequestIfImageContentTypeIsNull() throws Exception {
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

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                null, "dummy image data".getBytes());

        Long productId = savedProduct.getProductId();

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid image type. Valid image types: PNG, JPEG, WEBP"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageShouldReturnBadRequestIfImageSizeExceeds5MB() throws Exception {
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

        byte[] largeImage = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", largeImage);

        Long productId = savedProduct.getProductId();

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Image size exceeds 5MB"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageShouldReturnNotFoundIfProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        Long productId = Long.MAX_VALUE;

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateProductImageShouldReturnBadRequestIfOriginalFilenameIsNull() throws Exception {
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

        Long productId = savedProduct.getProductId();

        MockMultipartFile image = new MockMultipartFile("image", null,
                "image/png", "dummy image data".getBytes());

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid file name"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageShouldReturnBadRequestIfOriginalFilenameIsInvalid() throws Exception {
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

        Long productId = savedProduct.getProductId();

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter",
                "image/png", "dummy image data".getBytes());

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid file name"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
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
        Product savedProduct = productRepository.save(product);

        Long productId = savedProduct.getProductId();

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Long productId = 1L;

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        mockMvc.perform(multipart("/api/admin/products/{productId}/image", productId)
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// updateProductImageSeller()
    @Test
    void updateProductImageSellerShouldSuccessfullyUpdateProductImage() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(savedRole);
        User savedSeller = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedSeller, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        Long productId = savedProduct.getProductId();

        String uploadedFile = null;

        try {
            mockMvc.perform(multipart("/api/seller/products/{productId}/image", productId)
                            .file(image)
                            .with(user(userDetails))
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.productId").value(productId))
                    .andExpect(jsonPath("$.productName").value(savedProduct.getProductName()))
                    .andExpect(jsonPath("$.image").isNotEmpty())
                    .andExpect(jsonPath("$.image").value(startsWith("http://localhost:8080/images/")))
                    .andExpect(jsonPath("$.image", containsString(".png")))
                    .andExpect(jsonPath("$.description").value(savedProduct.getDescription()))
                    .andExpect(jsonPath("$.quantity").value(savedProduct.getQuantity()))
                    .andExpect(jsonPath("$.price").value(savedProduct.getPrice().doubleValue()))
                    .andExpect(jsonPath("$.discount").value(savedProduct.getDiscount().doubleValue()))
                    .andExpect(jsonPath("$.specialPrice").value(savedProduct.getSpecialPrice().doubleValue()));

            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertNotEquals("default.png", updatedProduct.getImage());
            assertTrue(updatedProduct.getImage().endsWith(".png"));
            assertNotEquals(image.getOriginalFilename(), updatedProduct.getImage());

            assertEquals(productId, updatedProduct.getProductId());
            assertEquals(savedProduct.getProductName(), updatedProduct.getProductName());
            assertEquals(savedProduct.getDescription(), updatedProduct.getDescription());
            assertEquals(savedProduct.getPrice(), updatedProduct.getPrice());
            assertEquals(savedProduct.getDiscount(), updatedProduct.getDiscount());
            assertEquals(savedProduct.getSpecialPrice(), updatedProduct.getSpecialPrice());
            assertEquals(savedProduct.getQuantity(), updatedProduct.getQuantity());

            assertTrue(Files.exists(Paths.get(uploadPath, updatedProduct.getImage())));

            uploadedFile = updatedProduct.getImage();
        } finally {
            if (uploadedFile != null) {
                Files.deleteIfExists(Paths.get(uploadPath, uploadedFile));
            }
        }
    }

    @Test
    void updateProductImageSellerShouldSuccessfullyUpdateProductImageIfUserIsAdmin() throws Exception {
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

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        Long productId = savedProduct.getProductId();

        String uploadedFile = null;

        try {
            mockMvc.perform(multipart("/api/seller/products/{productId}/image", productId)
                            .file(image)
                            .with(user(userDetails))
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.productId").value(productId))
                    .andExpect(jsonPath("$.productName").value(savedProduct.getProductName()))
                    .andExpect(jsonPath("$.image").isNotEmpty())
                    .andExpect(jsonPath("$.image").value(startsWith("http://localhost:8080/images/")))
                    .andExpect(jsonPath("$.image", containsString(".png")))
                    .andExpect(jsonPath("$.description").value(savedProduct.getDescription()))
                    .andExpect(jsonPath("$.quantity").value(savedProduct.getQuantity()))
                    .andExpect(jsonPath("$.price").value(savedProduct.getPrice().doubleValue()))
                    .andExpect(jsonPath("$.discount").value(savedProduct.getDiscount().doubleValue()))
                    .andExpect(jsonPath("$.specialPrice").value(savedProduct.getSpecialPrice().doubleValue()));

            Product updatedProduct = productRepository.findById(productId).orElseThrow();
            assertNotEquals("default.png", updatedProduct.getImage());
            assertTrue(updatedProduct.getImage().endsWith(".png"));
            assertNotEquals(image.getOriginalFilename(), updatedProduct.getImage());

            assertEquals(productId, updatedProduct.getProductId());
            assertEquals(savedProduct.getProductName(), updatedProduct.getProductName());
            assertEquals(savedProduct.getDescription(), updatedProduct.getDescription());
            assertEquals(savedProduct.getPrice(), updatedProduct.getPrice());
            assertEquals(savedProduct.getDiscount(), updatedProduct.getDiscount());
            assertEquals(savedProduct.getSpecialPrice(), updatedProduct.getSpecialPrice());
            assertEquals(savedProduct.getQuantity(), updatedProduct.getQuantity());

            assertTrue(Files.exists(Paths.get(uploadPath, updatedProduct.getImage())));

            uploadedFile = updatedProduct.getImage();
        } finally {
            if (uploadedFile != null) {
                Files.deleteIfExists(Paths.get(uploadPath, uploadedFile));
            }
        }
    }

    @Test
    void updateProductImageSellerShouldReturnBadRequestWhenSellerDoesNotOwnProduct() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller1 = createUser("Test Seller 1", "seller1@gmail.com", "password");
        seller1.getRoles().add(savedRole);
        User savedSeller1 = userRepository.save(seller1);

        UserDetailsImpl userDetails1 = UserDetailsImpl.build(savedSeller1);

        User seller2 = createUser("Test Seller 2", "seller2@gmail.com", "password");
        seller2.getRoles().add(savedRole);
        User savedSeller2 = userRepository.save(seller2);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedSeller2, savedCategory, "Mahabharata", "default.png",
                "Mahabharata: The Epic", 10, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        Product savedProduct2 = productRepository.save(product);

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        Long productId = savedProduct2.getProductId();

        mockMvc.perform(multipart("/api/seller/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails1))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product image cannot be updated as it does not belong to this seller!"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals(savedSeller2, productFromDb.getUser());
        assertEquals("default.png", productFromDb.getImage());
        assertEquals("Mahabharata", productFromDb.getProductName());
        assertEquals("Mahabharata: The Epic", productFromDb.getDescription());
        assertEquals(0, productFromDb.getPrice().compareTo(new BigDecimal("150")));
    }

    @Test
    void updateProductImageSellerShouldReturnNotFoundIfProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(savedRole);
        User savedSeller = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        Long productId = Long.MAX_VALUE;

        mockMvc.perform(multipart("/api/seller/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateProductImageSellerShouldReturnForbiddenIfUserIsNotSellerOrAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

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

        Long productId = savedProduct.getProductId();

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        mockMvc.perform(multipart("/api/seller/products/{productId}/image", productId)
                        .file(image)
                        .with(user(userDetails))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void updateProductImageSellerShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Long productId = 1L;

        MockMultipartFile image = new MockMultipartFile("image", "harry-potter.png",
                "image/png", "dummy image data".getBytes());

        mockMvc.perform(multipart("/api/seller/products/{productId}/image", productId)
                        .file(image)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
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
