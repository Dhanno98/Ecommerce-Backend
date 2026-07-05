package com.ecommerce.project.integration;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ProductDeleteControllerIT {

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

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    /// deleteProduct()
    @Test
    void deleteProductShouldSuccessfullyDeleteProduct() throws Exception {
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

        Long productId = savedProduct.getProductId();

        mockMvc.perform(delete("/api/admin/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.quantity").value(savedProduct.getQuantity()))
                .andExpect(jsonPath("$.price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(savedProduct.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.specialPrice").value(savedProduct.getSpecialPrice().doubleValue()));

        assertFalse(productRepository.existsById(productId));
        assertTrue(cartRepository.existsById(savedCart.getCartId()));

        assertTrue(cartItemRepository.findAllByProductId(productId).isEmpty());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertTrue(cartFromDB.getCartItems().isEmpty());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void deleteProductShouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long productId = Long.MAX_VALUE;

        mockMvc.perform(delete("/api/admin/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void deleteProductShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long productId = 1L;

        mockMvc.perform(delete("/api/admin/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteProductShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Long productId = 1L;

        mockMvc.perform(delete("/api/admin/products/{productId}", productId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    /// deleteProductSeller()
    @Test
    void deleteProductSellerShouldSuccessfullyDeleteProductOwnedBySeller() throws Exception {
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

        Long productId = savedProduct.getProductId();

        mockMvc.perform(delete("/api/seller/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.quantity").value(savedProduct.getQuantity()))
                .andExpect(jsonPath("$.price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(savedProduct.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.specialPrice").value(savedProduct.getSpecialPrice().doubleValue()));

        assertFalse(productRepository.existsById(productId));
        assertTrue(cartRepository.existsById(savedCart.getCartId()));

        assertTrue(cartItemRepository.findAllByProductId(productId).isEmpty());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertTrue(cartFromDB.getCartItems().isEmpty());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void deleteProductSellerShouldSuccessfullyDeleteProductIfUserIsAdmin() throws Exception {
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

        Long productId = savedProduct.getProductId();

        mockMvc.perform(delete("/api/seller/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.quantity").value(savedProduct.getQuantity()))
                .andExpect(jsonPath("$.price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.discount").value(savedProduct.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.specialPrice").value(savedProduct.getSpecialPrice().doubleValue()));

        assertFalse(productRepository.existsById(productId));
        assertTrue(cartRepository.existsById(savedCart.getCartId()));

        assertTrue(cartItemRepository.findAllByProductId(productId).isEmpty());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertTrue(cartFromDB.getCartItems().isEmpty());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void deleteProductSellerShouldReturnBadRequestWhenSellerDoesNotOwnTheProduct() throws Exception {
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

        Long productId = savedProduct2.getProductId();

        mockMvc.perform(delete("/api/seller/products/{productId}", productId)
                        .with(user(userDetails1)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product cannot be deleted as it does not belong to this seller!"))
                .andExpect(jsonPath("$.status").value(false));

        assertTrue(productRepository.existsById(productId));

        Product productFromDb = productRepository.findById(productId).orElseThrow();
        assertEquals(savedSeller2.getUserId(), productFromDb.getUser().getUserId());
        assertEquals("Mahabharata", productFromDb.getProductName());
        assertEquals("default.png", productFromDb.getImage());
    }

    @Test
    void deleteProductSellerShouldReturnNotFoundIfProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller = createUser("Test Seller", "seller@gmail.com", "password");
        seller.getRoles().add(savedRole);
        User savedSeller = userRepository.save(seller);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        Long productId = Long.MAX_VALUE;

        mockMvc.perform(delete("/api/seller/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void deleteProductSellerShouldReturnForbiddenIfUserIsNotSellerOrAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedSeller = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller);

        Long productId = 1L;

        mockMvc.perform(delete("/api/seller/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void deleteProductSellerShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Long productId = 1L;

        mockMvc.perform(delete("/api/seller/products/{productId}", productId))
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
