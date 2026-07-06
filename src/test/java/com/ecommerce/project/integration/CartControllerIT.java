package com.ecommerce.project.integration;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.CartItemResponseDTO;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CartControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /// addProductToCart()
    @Test
    void addProductToCartShouldSuccessfullyCreateAndAddProductToCartWhenCartDoesNotExist() throws Exception {
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
        int quantity = 3;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").isNumber())
                .andExpect(jsonPath("$.cartItems.length()").value(1))
                .andExpect(jsonPath("$.cartItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.cartItems[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.cartItems[0].image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.cartItems[0].description").value(savedProduct.getDescription()))
                .andExpect(jsonPath("$.cartItems[0].quantity").value(quantity))
                .andExpect(jsonPath("$.cartItems[0].price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.cartItems[0].discount").value(savedProduct.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.cartItems[0].specialPrice").value(savedProduct.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.totalPrice").value(savedProduct.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)).doubleValue()));

        assertEquals(1, cartRepository.count());
        Cart cartFromDB = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNotNull(cartFromDB);
        assertEquals(savedUser.getUserId(), cartFromDB.getUser().getUserId());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(savedProduct.getSpecialPrice().multiply(BigDecimal.valueOf(quantity))));

        assertEquals(1, cartFromDB.getCartItems().size());
        CartItem cartItem = cartFromDB.getCartItems().getFirst();
        assertEquals(savedProduct.getProductId(), cartItem.getProduct().getProductId());
        assertEquals(quantity, cartItem.getQuantity());
        assertEquals(savedProduct.getDiscount(), cartItem.getDiscount());
        assertEquals(savedProduct.getSpecialPrice(), cartItem.getProductPrice());

        Product productFromDB = productRepository.findById(productId).orElseThrow();
        assertEquals(savedProduct.getQuantity(), productFromDB.getQuantity());
    }

    @Test
    void addProductToCartShouldSuccessfullyAddProductToExistingCart() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        Long productId = savedProduct.getProductId();
        int quantity = 3;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").value(savedCart.getCartId()))
                .andExpect(jsonPath("$.cartItems.length()").value(1))
                .andExpect(jsonPath("$.cartItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.cartItems[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.cartItems[0].image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.cartItems[0].description").value(savedProduct.getDescription()))
                .andExpect(jsonPath("$.cartItems[0].quantity").value(quantity))
                .andExpect(jsonPath("$.cartItems[0].price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.cartItems[0].discount").value(savedProduct.getDiscount().doubleValue()))
                .andExpect(jsonPath("$.cartItems[0].specialPrice").value(savedProduct.getSpecialPrice().doubleValue()))
                .andExpect(jsonPath("$.totalPrice").value(savedProduct.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)).doubleValue()));

        assertEquals(1, cartRepository.count());
        Cart cartFromDB = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNotNull(cartFromDB);
        assertEquals(savedCart.getCartId(), cartFromDB.getCartId());
        assertEquals(savedUser.getUserId(), cartFromDB.getUser().getUserId());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(savedProduct.getSpecialPrice().multiply(BigDecimal.valueOf(quantity))));

        assertEquals(1, cartFromDB.getCartItems().size());
        CartItem cartItem = cartFromDB.getCartItems().getFirst();
        assertEquals(savedProduct.getProductId(), cartItem.getProduct().getProductId());
        assertEquals(quantity, cartItem.getQuantity());
        assertEquals(savedProduct.getDiscount(), cartItem.getDiscount());
        assertEquals(savedProduct.getSpecialPrice(), cartItem.getProductPrice());

        Product productFromDB = productRepository.findById(productId).orElseThrow();
        assertEquals(savedProduct.getQuantity(), productFromDB.getQuantity());
    }

    @Test
    void addProductToCartShouldSuccessfullyAddProductToCartIfAnotherProductAlreadyInCart() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

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

        Product product2 = createProduct(savedUser, savedCategory, "Lord of the Rings", "default.png",
                "Lord of the Rings: Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct2 = productRepository.save(product2);

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct1, savedCart);
        CartItem savedCartItem1 = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem1);
        savedCart.setTotalPrice(savedCartItem1.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem1.getQuantity())));
        cartRepository.save(savedCart);

        BigDecimal initialCartTotal = savedCart.getTotalPrice();

        Long productId = savedProduct2.getProductId();
        int quantity = 5;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").value(savedCart.getCartId()))
                .andExpect(jsonPath("$.cartItems.length()").value(2))
                .andExpect(jsonPath("$.cartItems[0].productId").value(savedProduct1.getProductId()))
                .andExpect(jsonPath("$.cartItems[1].productId").value(savedProduct2.getProductId()))
                .andExpect(jsonPath("$.totalPrice").value(initialCartTotal.add(savedProduct2.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)))));

        assertEquals(1, cartRepository.count());
        Cart cartFromDB = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNotNull(cartFromDB);
        assertEquals(savedCart.getCartId(), cartFromDB.getCartId());
        assertEquals(savedUser.getUserId(), cartFromDB.getUser().getUserId());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(initialCartTotal.add(savedProduct2.getSpecialPrice().multiply(BigDecimal.valueOf(quantity)))));
        assertEquals(2, cartFromDB.getCartItems().size());

        assertTrue(cartFromDB.getCartItems().stream()
                        .anyMatch(ci -> ci.getProduct().getProductId().equals(savedProduct1.getProductId())));

        assertTrue(cartFromDB.getCartItems().stream()
                        .anyMatch(ci -> ci.getProduct().getProductId().equals(savedProduct2.getProductId())));
    }

    @Test
    void addProductToCartShouldReturnBadRequestIfRequestedQuantityIsLessThanOne() throws Exception {
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
        int quantity = 0;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Requested quantity must be greater than 0"))
                .andExpect(jsonPath("$.status").value(false));

        Cart cartFromDb = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNull(cartFromDb);
    }

    @Test
    void addProductToCartShouldReturnNotFoundIfProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long productId = Long.MAX_VALUE;
        int quantity = 3;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));

        Cart cartFromDb = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNotNull(cartFromDb);
        assertEquals(0, cartFromDb.getCartItems().size());
        assertEquals(0, cartFromDb.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void addProductToCartShouldReturnBadRequestIfProductAlreadyExistsInCart() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        int quantity = 5;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product Harry Potter 3 already exists in the cart"))
                .andExpect(jsonPath("$.status").value(false));

        Cart cartFromDb = cartRepository.findCartByUserId(savedUser.getUserId());
        assertEquals(1, cartFromDb.getCartItems().size());
        assertEquals(0, cartFromDb.getTotalPrice().compareTo(savedCart.getTotalPrice()));
    }

    @Test
    void addProductToCartShouldReturnBadRequestIfProductStockQuantityIsZero() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 0,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        Long productId = savedProduct.getProductId();
        int quantity = 3;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Harry Potter 3 is out of stock"))
                .andExpect(jsonPath("$.status").value(false));

        Cart cartFromDb = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNotNull(cartFromDb);
        assertEquals(0, cartFromDb.getCartItems().size());
        assertEquals(0, cartFromDb.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void addProductToCartShouldReturnBadRequestWhenRequestedQuantityExceedsAvailableStock() throws Exception {
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
        int quantity = 11;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Please, make an order of the Harry Potter 3 less than or equal to the stock quantity 10."))
                .andExpect(jsonPath("$.status").value(false));

        Cart cartFromDb = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNotNull(cartFromDb);
        assertEquals(0, cartFromDb.getCartItems().size());
        assertEquals(0, cartFromDb.getTotalPrice().compareTo(BigDecimal.ZERO));
    }

    @Test
    void addProductToCartShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        Long productId = savedProduct.getProductId();
        int quantity = 3;

        mockMvc.perform(post("/api/carts/products/{productId}/quantity/{quantity}", productId, quantity))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));

        Cart cartFromDb = cartRepository.findCartByUserId(savedUser.getUserId());
        assertNull(cartFromDb);
    }

    /// getCarts()
    @Test
    void getCartsShouldSuccessfullyReturnAllCartsIfUserIsAdmin() throws Exception {
        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User admin = createUser("Test Admin", "admin@gmail.com", "password");
        admin.getRoles().add(adminRole);
        User savedAdmin = userRepository.save(admin);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedAdmin);

        User user1 = createUser("Test User 1", "user1@gmail.com", "password");
        user1.getRoles().add(userRole);
        User savedUser1 = userRepository.save(user1);

        User user2 = createUser("Test User 2", "user2@gmail.com", "password");
        user2.getRoles().add(userRole);
        User savedUser2 = userRepository.save(user2);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedAdmin, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedAdmin, savedCategory, "Lord of the Rings", "default.png",
                "Lord of the Rings: Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct2 = productRepository.save(product2);

        Cart cart1 = createCart(savedUser1);
        Cart savedCart1 = cartRepository.save(cart1);
        CartItem cartItem1 = createCartItem(savedProduct1, savedCart1);
        cartItem1.setQuantity(3);
        CartItem savedCartItem1 = cartItemRepository.save(cartItem1);

        savedCart1.getCartItems().add(savedCartItem1);
        savedCart1.setTotalPrice(savedCartItem1.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem1.getQuantity())));
        cartRepository.save(savedCart1);

        Cart cart2 = createCart(savedUser2);
        Cart savedCart2 = cartRepository.save(cart2);
        CartItem cartItem2 = createCartItem(savedProduct2, savedCart2);
        cartItem2.setQuantity(5);
        CartItem savedCartItem2 = cartItemRepository.save(cartItem2);

        savedCart2.getCartItems().add(savedCartItem2);
        savedCart2.setTotalPrice(savedCartItem2.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem2.getQuantity())));
        cartRepository.save(savedCart2);

        MvcResult result = mockMvc.perform(get("/api/admin/carts")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        List<CartDTO> carts = objectMapper.readValue(response, new TypeReference<List<CartDTO>>() {});

        CartDTO cartDTO1 = carts.stream()
                .filter(c -> c.getCartId().equals(savedCart1.getCartId()))
                .findFirst()
                .orElseThrow();

        assertEquals(0, cartDTO1.getTotalPrice().compareTo(savedCart1.getTotalPrice()));
        assertEquals(savedCart1.getCartId(), cartDTO1.getCartId());
        assertEquals(1, cartDTO1.getCartItems().size());

        CartItemResponseDTO item1 = cartDTO1.getCartItems().getFirst();
        assertEquals(savedProduct1.getProductId(), item1.getProductId());
        assertEquals(savedProduct1.getProductName(), item1.getProductName());
        assertEquals(3, item1.getQuantity());
        assertEquals(0, item1.getPrice().compareTo(savedProduct1.getPrice()));
        assertEquals(0, item1.getSpecialPrice().compareTo(savedProduct1.getSpecialPrice()));

        CartDTO cartDTO2 = carts.stream()
                .filter(c -> c.getCartId().equals(savedCart2.getCartId()))
                .findFirst()
                .orElseThrow();

        assertEquals(0, cartDTO2.getTotalPrice().compareTo(savedCart2.getTotalPrice()));
        assertEquals(savedCart2.getCartId(), cartDTO2.getCartId());
        assertEquals(1, cartDTO2.getCartItems().size());

        CartItemResponseDTO item2 = cartDTO2.getCartItems().getFirst();
        assertEquals(savedProduct2.getProductId(), item2.getProductId());
        assertEquals(savedProduct2.getProductName(), item2.getProductName());
        assertEquals(5, item2.getQuantity());
        assertEquals(0, item2.getPrice().compareTo(savedProduct2.getPrice()));
        assertEquals(0, item2.getSpecialPrice().compareTo(savedProduct2.getSpecialPrice()));

        assertEquals(2, cartRepository.count());
        assertEquals(2, cartItemRepository.count());
    }

    @Test
    void getCartsShouldReturnBadRequestWhenNoCartExists() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/admin/carts")
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("No cart exist"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getCartsShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/admin/carts")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getCartsShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/carts"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getCartById
    @Test
    void getCartByIdShouldReturnTheCartOfTheLoggedInUserById() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        mockMvc.perform(get("/api/carts/users/cart")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").value(savedCart.getCartId()))
                .andExpect(jsonPath("$.totalPrice").value(savedCart.getTotalPrice().doubleValue()))
                .andExpect(jsonPath("$.cartItems.length()").value(1))
                .andExpect(jsonPath("$.cartItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.cartItems[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.cartItems[0].quantity").value(savedCartItem.getQuantity()))
                .andExpect(jsonPath("$.cartItems[0].price").value(savedProduct.getPrice().doubleValue()))
                .andExpect(jsonPath("$.cartItems[0].specialPrice").value(savedProduct.getSpecialPrice().doubleValue()));
    }

    @Test
    void getCartByIdShouldReturnBadRequestWhenTheLoggedInUserHasNoCartCreatedYet() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/carts/users/cart")
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cart not yet created!"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getCartByIdShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/carts/users/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// updateCartProductQuantity()
    @Test
    void updateCartProductQuantityShouldSuccessfullyIncrementCartItemQuantityByOne() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        cartItem.setQuantity(3);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        String operation = "add";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").value(savedCart.getCartId()))
                .andExpect(jsonPath("$.cartItems.length()").value(1))
                .andExpect(jsonPath("$.cartItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.cartItems[0].quantity").value(4))
                .andExpect(jsonPath("$.totalPrice").value(360));

        CartItem cartItemFromDB = cartItemRepository.findCartItemByProductIdAndCartId(savedCart.getCartId(), productId);
        assertEquals(4, cartItemFromDB.getQuantity());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(new BigDecimal("360")));
    }

    @Test
    void updateCartProductQuantityShouldSuccessfullyDecrementCartItemQuantityByOne() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        cartItem.setQuantity(3);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        String operation = "delete";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").value(savedCart.getCartId()))
                .andExpect(jsonPath("$.cartItems.length()").value(1))
                .andExpect(jsonPath("$.cartItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.cartItems[0].quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(180));

        CartItem cartItemFromDB = cartItemRepository.findCartItemByProductIdAndCartId(savedCart.getCartId(), productId);
        assertEquals(2, cartItemFromDB.getQuantity());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(new BigDecimal("180")));
    }

    @Test
    void updateCartProductQuantityShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        cartItem.setQuantity(3);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        String operation = "add";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void updateCartProductQuantityShouldReturnBadRequestIfOperationIsInvalid() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        cartItem.setQuantity(3);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        String operation = "increment";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid operation. Supported operations: add, delete"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateCartProductQuantityShouldReturnBadRequestWhenCartDoesNotExist() throws Exception {
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
        String operation = "add";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cart not yet created!"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateCartProductQuantityShouldReturnNotFoundIfProductDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Cart cart = createCart(savedUser);
        cartRepository.save(cart);

        Long productId = Long.MAX_VALUE;
        String operation = "add";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateCartProductQuantityShouldReturnBadRequestIfProductNotInCart() throws Exception {
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

        Cart cart = createCart(savedUser);
        cartRepository.save(cart);

        Long productId = savedProduct.getProductId();
        String operation = "add";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Product Harry Potter 3 not available in the cart!"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateCartProductQuantityShouldReturnBadRequestIfProductIsOutOfStock() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 0,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        cartItem.setQuantity(3);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        String operation = "add";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Harry Potter 3 is out of stock"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateCartProductQuantityShouldReturnBadRequestIfProductQuantityAfterIncrementBecomesGreaterThanProductStockQuantity() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        cartItem.setQuantity(10);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        String operation = "add";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Please, make an order of the Harry Potter 3 less than or equal to the stock quantity 10."))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateCartProductQuantityShouldDeleteProductFromCartIfQuantityAfterUpdateIsZero() throws Exception {
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

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        cartItem.setQuantity(1);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        Long productId = savedProduct.getProductId();
        String operation = "delete";

        mockMvc.perform(put("/api/cart/products/{productId}/quantity/{operation}", productId, operation)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cartId").value(savedCart.getCartId()))
                .andExpect(jsonPath("$.cartItems.length()").value(0))
                .andExpect(jsonPath("$.totalPrice").value(0));

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertEquals(0, cartFromDB.getCartItems().size());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(BigDecimal.ZERO));

        CartItem cartItemFromDB = cartItemRepository.findCartItemByProductIdAndCartId(savedCart.getCartId(), productId);
        assertNull(cartItemFromDB);
    }

    /// deleteProductFromCart()
    @Test
    void deleteProductFromCartShouldSuccessfullyDeleteProductFromCart() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

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

        Product product2 = createProduct(savedUser, savedCategory, "Lord Of the Rings", "default.png",
                "Lord Of the Rings: The Return Of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct2 = productRepository.save(product2);

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem1 = createCartItem(savedProduct1, savedCart);
        cartItem1.setQuantity(3);
        CartItem savedCartItem1 = cartItemRepository.save(cartItem1);
        savedCart.getCartItems().add(savedCartItem1);
        BigDecimal price1 = savedCartItem1.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem1.getQuantity()));

        CartItem cartItem2 = createCartItem(savedProduct2, savedCart);
        cartItem2.setQuantity(2);
        CartItem savedCartItem2 = cartItemRepository.save(cartItem2);
        savedCart.getCartItems().add(savedCartItem2);
        BigDecimal price2 = savedCartItem2.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem2.getQuantity()));

        BigDecimal totalPrice = price1.add(price2);
        savedCart.setTotalPrice(totalPrice);
        cartRepository.save(savedCart);

        Long productId = savedProduct1.getProductId();

        mockMvc.perform(delete("/api/cart/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("Product " + savedCartItem1.getProduct().getProductName() + " removed from the cart!"));

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertEquals(1, cartFromDB.getCartItems().size());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(totalPrice.subtract(price1)));

        CartItem remaining = cartFromDB.getCartItems().getFirst();
        assertEquals(savedProduct2.getProductId(), remaining.getProduct().getProductId());
        assertEquals(2, remaining.getQuantity());
    }

    @Test
    void deleteProductFromCartShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Long productId = 1L;

        mockMvc.perform(delete("/api/cart/products/{productId}", productId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deleteProductFromCartShouldReturnBadRequestIfCartDoesNotExist() throws Exception {
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

        mockMvc.perform(delete("/api/cart/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cart not yet created!"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void deleteProductFromCartShouldReturnNotFoundIfProductNotInCart() throws Exception {
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

        Cart cart = createCart(savedUser);
        cartRepository.save(cart);

        Long productId = savedProduct.getProductId();

        mockMvc.perform(delete("/api/cart/products/{productId}", productId)
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("CartItem not found with productId: " + productId))
                .andExpect(jsonPath("$.status").value(false));
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
