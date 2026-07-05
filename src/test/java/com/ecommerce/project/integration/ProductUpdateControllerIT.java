package com.ecommerce.project.integration;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CreateProductRequest;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductUpdateControllerIT {

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

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    /// updateProduct()
    @Test
    void updateProductShouldSuccessfullyUpdateProduct() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 6", "Harry Potter and the Half Blood Prince", 10,
                new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct.getProductId();

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.productName").value(productRequest.getProductName()))
                .andExpect(jsonPath("$.image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.description").value(productRequest.getDescription()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(productRequest.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.specialPrice").value(BigDecimal.valueOf(135).doubleValue()));

        assertEquals(1, productRepository.count());
        Product productFromDB = productRepository.findById(productId).orElseThrow();
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice().doubleValue(), productFromDB.getPrice().doubleValue());
        assertEquals(productRequest.getDiscount().doubleValue(), productFromDB.getDiscount().doubleValue());

        List<CartItem> cartItems = cartItemRepository.findAllByProductId(productId);
        CartItem cartItemFromDB = cartItems.getFirst();
        assertEquals(productRequest.getDiscount().doubleValue(), cartItemFromDB.getDiscount().doubleValue());
        assertEquals(BigDecimal.valueOf(135).doubleValue(), cartItemFromDB.getProductPrice().doubleValue());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(new BigDecimal("405")));
    }

    @Test
    void updateProductShouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        CreateProductRequest productRequest = createProductRequest(
                "Harry Potter 6", "Harry Potter and the Half Blood Prince", 10,
                new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = Long.MAX_VALUE;

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateProductShouldReturnBadRequestWhenProductNameAlreadyExists() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "Mahabharata", "default.png",
                "Mahabharata: The Epic", 10, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        productRepository.save(product2);

        CreateProductRequest productRequest = createProductRequest("Mahabharata", "Mahabharata: The Epic",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct1.getProductId();

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product with name: " + productRequest.getProductName() + " already exists."))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateProductShouldAllowUpdatingProductWithSameName() throws Exception {
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

        CreateProductRequest productRequest = createProductRequest("Harry Potter 3", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct.getProductId();

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.productName").value(productRequest.getProductName()))
                .andExpect(jsonPath("$.image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.description").value(productRequest.getDescription()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(productRequest.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.specialPrice").value(BigDecimal.valueOf(135).doubleValue()));

        assertEquals(1, productRepository.count());
        Product productFromDB = productRepository.findById(productId).orElseThrow();
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice().doubleValue(), productFromDB.getPrice().doubleValue());
        assertEquals(productRequest.getDiscount().doubleValue(), productFromDB.getDiscount().doubleValue());
    }

    @Test
    void updateProductShouldReturnBadRequestIfProductRequestIsInvalid() throws Exception {
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

        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                -10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct.getProductId();

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.quantity").value("Product quantity cannot be negative"));
    }

    @Test
    void updateProductShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
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

        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct.getProductId();

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateProductShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = 1L;

        mockMvc.perform(put("/api/admin/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// updateProductSeller()
    @Test
    void updateProductSellerShouldSuccessfullyUpdateOwnProduct() throws Exception {
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

        Cart cart = createCart(savedSeller);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct.getProductId();

        mockMvc.perform(put("/api/seller/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.productName").value(productRequest.getProductName()))
                .andExpect(jsonPath("$.image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.description").value(productRequest.getDescription()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(productRequest.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.specialPrice").value(BigDecimal.valueOf(135).doubleValue()));

        assertEquals(1, productRepository.count());
        Product productFromDB = productRepository.findById(productId).orElseThrow();
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice().doubleValue(), productFromDB.getPrice().doubleValue());
        assertEquals(productRequest.getDiscount().doubleValue(), productFromDB.getDiscount().doubleValue());
        assertEquals(0, productFromDB.getSpecialPrice().compareTo(new BigDecimal("135")));

        List<CartItem> cartItems = cartItemRepository.findAllByProductId(productId);
        CartItem cartItemFromDB = cartItems.getFirst();
        assertEquals(productRequest.getDiscount().doubleValue(), cartItemFromDB.getDiscount().doubleValue());
        assertEquals(BigDecimal.valueOf(135).doubleValue(), cartItemFromDB.getProductPrice().doubleValue());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(new BigDecimal("405")));
    }

    @Test
    void updateProductSellerShouldSuccessfullyUpdateProductIfUserIsAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test Seller", "seller@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct.getProductId();

        mockMvc.perform(put("/api/seller/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.productName").value(productRequest.getProductName()))
                .andExpect(jsonPath("$.image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.description").value(productRequest.getDescription()))
                .andExpect(jsonPath("$.quantity").value(productRequest.getQuantity()))
                .andExpect(jsonPath("$.price").value(productRequest.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(productRequest.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.specialPrice").value(BigDecimal.valueOf(135).doubleValue()));

        assertEquals(1, productRepository.count());
        Product productFromDB = productRepository.findById(productId).orElseThrow();
        assertEquals(productRequest.getProductName(), productFromDB.getProductName());
        assertEquals(productRequest.getDescription(), productFromDB.getDescription());
        assertEquals(productRequest.getQuantity(), productFromDB.getQuantity());
        assertEquals(productRequest.getPrice().doubleValue(), productFromDB.getPrice().doubleValue());
        assertEquals(productRequest.getDiscount().doubleValue(), productFromDB.getDiscount().doubleValue());
        assertEquals(0, productFromDB.getSpecialPrice().compareTo(new BigDecimal("135")));

        List<CartItem> cartItems = cartItemRepository.findAllByProductId(productId);
        CartItem cartItemFromDB = cartItems.getFirst();
        assertEquals(productRequest.getDiscount().doubleValue(), cartItemFromDB.getDiscount().doubleValue());
        assertEquals(BigDecimal.valueOf(135).doubleValue(), cartItemFromDB.getProductPrice().doubleValue());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(new BigDecimal("405")));
    }

    @Test
    void updateProductSellerShouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(savedRole);
        User savedUser = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = Long.MAX_VALUE;

        mockMvc.perform(put("/api/seller/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateProductSellerShouldReturnBadRequestWhenSellerDoesNotOwnProduct() throws Exception {
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

        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct2.getProductId();

        mockMvc.perform(put("/api/seller/products/{productId}", productId)
                        .with(user(userDetails1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product cannot be updated as it does not belong to this seller!"))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals(savedSeller2, productFromDb.getUser());
        assertEquals("Mahabharata", productFromDb.getProductName());
        assertEquals("Mahabharata: The Epic", productFromDb.getDescription());
        assertEquals(0, productFromDb.getPrice().compareTo(new BigDecimal("150")));
    }

    @Test
    void updateProductSellerShouldReturnBadRequestWhenProductNameAlreadyExists() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller1 = createUser("Test Seller 1", "seller1@gmail.com", "password");
        seller1.getRoles().add(savedRole);
        User savedUser1 = userRepository.save(seller1);

        UserDetailsImpl userDetails1 = UserDetailsImpl.build(savedUser1);

        User seller2 = createUser("Test Seller 2", "seller2@gmail.com", "password");
        seller2.getRoles().add(savedRole);
        User savedUser2 = userRepository.save(seller2);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser1, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedUser2, savedCategory, "Mahabharata", "default.png",
                "Mahabharata: The Epic", 10, new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        productRepository.save(product2);

        CreateProductRequest productRequest = createProductRequest("Mahabharata", "Mahabharata: The Epic War", 10,
                new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = savedProduct1.getProductId();

        mockMvc.perform(put("/api/seller/products/{productId}", productId)
                        .with(user(userDetails1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product with name: Mahabharata already exists."))
                .andExpect(jsonPath("$.status").value(false));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals("Harry Potter 3", productFromDb.getProductName());
    }

    @Test
    void updateProductSellerShouldReturnForbiddenIfUserIsNotSeller() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = 1L;

        mockMvc.perform(put("/api/seller/products/{productId}", productId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateProductSellerShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        CreateProductRequest productRequest = createProductRequest("Harry Potter 6", "Harry Potter and the Half Blood Prince",
                10, new BigDecimal("150"), new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(productRequest);
        Long productId = 1L;

        mockMvc.perform(put("/api/seller/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
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

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());
        return cart;
    }

    private CartItem createCartItem(Product product, Cart cart) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(3);
        cartItem.setDiscount(product.getDiscount());
        cartItem.setProductPrice(product.getSpecialPrice());
        return cartItem;
    }
}
