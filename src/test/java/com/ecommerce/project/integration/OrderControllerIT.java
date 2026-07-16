package com.ecommerce.project.integration;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.OrderItem;
import com.ecommerce.project.model.OrderStatus;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.PaymentStatus;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.OrderRequestDTO;
import com.ecommerce.project.payload.OrderStatusUpdateDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.OrderItemRepository;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.repositories.PaymentRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.ecommerce.project.model.OrderStatus.CREATED;
import static com.ecommerce.project.model.OrderStatus.DELIVERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
public class OrderControllerIT {

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
    private AddressRepository addressRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /// orderProducts()
    @Test
    void orderProductsShouldSuccessfullyPlaceTheOrder() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);
        Integer initialProductQuantity = savedProduct.getQuantity();

        Cart cart = createCart(savedUser);
        Cart savedCart = cartRepository.save(cart);

        CartItem cartItem = createCartItem(savedProduct, savedCart);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);
        BigDecimal cartTotal = savedCart.getTotalPrice();

        OrderRequestDTO orderRequestDTO = createOrderRequestDTO(savedAddress.getAddressId(), PaymentMethod.CARD, "MOCK");
        String json = objectMapper.writeValueAsString(orderRequestDTO);

        mockMvc.perform(post("/api/order/users")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.orderDate").exists())
                .andExpect(jsonPath("$.totalAmount").value(cartTotal.doubleValue()))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.addressId").value(savedAddress.getAddressId()))

                .andExpect(jsonPath("$.orderItems.length()").value(1))
                .andExpect(jsonPath("$.orderItems[0].orderItemId").isNumber())
                .andExpect(jsonPath("$.orderItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.orderItems[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.orderItems[0].description").value(savedProduct.getDescription()))
                .andExpect(jsonPath("$.orderItems[0].image").value("http://localhost:8080/images/" + savedProduct.getImage()))
                .andExpect(jsonPath("$.orderItems[0].quantityOrdered").value(savedCartItem.getQuantity()))
                .andExpect(jsonPath("$.orderItems[0].orderedProductPrice").value(savedCartItem.getProductPrice().doubleValue()))
                .andExpect(jsonPath("$.orderItems[0].discount").value(savedCartItem.getDiscount().doubleValue()))

                .andExpect(jsonPath("$.payment.paymentId").isNumber())
                .andExpect(jsonPath("$.payment.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.payment.pgPaymentId").exists())
                .andExpect(jsonPath("$.payment.pgStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.payment.pgResponseMessage").exists())
                .andExpect(jsonPath("$.payment.pgName").exists());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();
        assertTrue(cartFromDB.getCartItems().isEmpty());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(BigDecimal.ZERO));

        List<CartItem> cartItems = cartItemRepository.findAll();
        assertTrue(cartItems.isEmpty());

        assertEquals(1, orderRepository.count());
        Order orderFromDB = orderRepository.findAll().getFirst();
        assertEquals(0, orderFromDB.getTotalAmount().compareTo(cartTotal));
        assertEquals(CREATED, orderFromDB.getOrderStatus());
        assertEquals(savedAddress.getAddressId(), orderFromDB.getAddress().getAddressId());
        assertEquals(savedUser.getEmail(), orderFromDB.getEmail());

        assertEquals(1, orderItemRepository.count());
        OrderItem orderItemFromDB = orderItemRepository.findAll().getFirst();
        assertNotNull(orderItemFromDB);
        assertEquals(savedCartItem.getProduct().getProductId(), orderItemFromDB.getProduct().getProductId());
        assertEquals(savedCartItem.getQuantity(), orderItemFromDB.getQuantity());
        assertEquals(0, orderItemFromDB.getOrderedProductPrice().compareTo(savedCartItem.getProductPrice()));
        assertEquals(0, orderItemFromDB.getDiscount().compareTo(savedCartItem.getDiscount()));

        Product productFromDB = productRepository.findById(savedProduct.getProductId()).orElseThrow();
        Integer productStockQuantity = initialProductQuantity - orderItemFromDB.getQuantity();
        assertEquals(productStockQuantity, productFromDB.getQuantity());

        assertEquals(1, paymentRepository.count());
        Payment paymentFromDB = paymentRepository.findAll().getFirst();
        assertNotNull(paymentFromDB);
        assertEquals(orderFromDB.getOrderId(), paymentFromDB.getOrder().getOrderId());
        assertEquals(PaymentMethod.CARD, paymentFromDB.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, paymentFromDB.getPgStatus());
    }

    @Test
    void orderProductsShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        OrderRequestDTO orderRequestDTO = createOrderRequestDTO(savedAddress.getAddressId(), PaymentMethod.CARD, "MOCK");
        String json = objectMapper.writeValueAsString(orderRequestDTO);

        mockMvc.perform(post("/api/order/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void orderProductsShouldReturnNotFoundIfCartDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product);

        OrderRequestDTO orderRequestDTO = createOrderRequestDTO(savedAddress.getAddressId(), PaymentMethod.CARD, "MOCK");
        String json = objectMapper.writeValueAsString(orderRequestDTO);

        mockMvc.perform(post("/api/order/users")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cart not found with email: " + savedUser.getEmail()))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void orderProductsShouldReturnNotFoundIfAddressDoesNotExist() throws Exception {
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
        productRepository.save(product);

        Cart cart = createCart(savedUser);
        cartRepository.save(cart);

        Long addressId = Long.MAX_VALUE;
        OrderRequestDTO orderRequestDTO = createOrderRequestDTO(addressId, PaymentMethod.CARD, "MOCK");
        String json = objectMapper.writeValueAsString(orderRequestDTO);

        mockMvc.perform(post("/api/order/users")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Address not found with addressId: " + addressId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void orderProductsShouldReturnBadRequestIfCartIsEmpty() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product);

        Cart cart = createCart(savedUser);
        cartRepository.save(cart);

        OrderRequestDTO orderRequestDTO = createOrderRequestDTO(savedAddress.getAddressId(), PaymentMethod.CARD, "MOCK");
        String json = objectMapper.writeValueAsString(orderRequestDTO);

        mockMvc.perform(post("/api/order/users")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cart is empty!"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void orderProductsShouldReturnBadRequestIfProductQuantityInCartGreaterThanProductStockQuantity() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

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
        cartItem.setQuantity(11);
        CartItem savedCartItem = cartItemRepository.save(cartItem);

        savedCart.getCartItems().add(savedCartItem);
        savedCart.setTotalPrice(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity())));
        cartRepository.save(savedCart);

        OrderRequestDTO orderRequestDTO = createOrderRequestDTO(savedAddress.getAddressId(), PaymentMethod.CARD, "MOCK");
        String json = objectMapper.writeValueAsString(orderRequestDTO);

        mockMvc.perform(post("/api/order/users")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.['excess items']['Harry Potter 3']").value("Requested: 11, Available: 10"))
                .andExpect(jsonPath("$.message").value("Requested quantity for these items is greater than the available stock! Please remove excess items and then place the order."))
                .andExpect(jsonPath("$.status").value(false));

        assertEquals(0, orderRepository.count());
        assertEquals(0, orderItemRepository.count());
        assertEquals(0, paymentRepository.count());

        Cart cartFromDB = cartRepository.findById(savedCart.getCartId()).orElseThrow();

        assertEquals(1, cartFromDB.getCartItems().size());
        assertEquals(0, cartFromDB.getTotalPrice().compareTo(savedCartItem.getProductPrice().multiply(BigDecimal.valueOf(savedCartItem.getQuantity()))));

        Product productFromDB = productRepository.findById(savedProduct.getProductId()).orElseThrow();
        assertEquals(10, productFromDB.getQuantity());
    }

    /// getAllOrders()
    @Test
    void getAllOrdersShouldReturnAllOrders() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

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

        Payment payment1 = createPayment(PaymentMethod.CARD, "MOCK1");
        Payment savedPayment1 = paymentRepository.save(payment1);

        Order order1 = createOrder(savedUser, savedAddress, savedPayment1);
        Order savedOrder1 = orderRepository.save(order1);

        OrderItem orderItem1 = createOrderItem(savedProduct1);
        orderItem1.setQuantity(3);
        orderItem1.setOrder(savedOrder1);
        OrderItem savedOrderItem1 = orderItemRepository.save(orderItem1);

        OrderItem orderItem2 = createOrderItem(savedProduct2);
        orderItem2.setQuantity(5);
        orderItem2.setOrder(savedOrder1);
        OrderItem savedOrderItem2 = orderItemRepository.save(orderItem2);

        BigDecimal amount1 = savedOrderItem1.getOrderedProductPrice()
                .multiply(BigDecimal.valueOf(savedOrderItem1.getQuantity()))
                .add(savedOrderItem2.getOrderedProductPrice()
                        .multiply(BigDecimal.valueOf(savedOrderItem2.getQuantity())));

        savedOrder1.getOrderItems().add(savedOrderItem1);
        savedOrder1.getOrderItems().add(savedOrderItem2);
        savedOrder1.setTotalAmount(amount1);
        orderRepository.save(savedOrder1);

        Payment payment2 = createPayment(PaymentMethod.CARD, "MOCK2");
        Payment savedPayment2 = paymentRepository.save(payment2);

        Order order2 = createOrder(savedUser, savedAddress, savedPayment2);
        Order savedOrder2 = orderRepository.save(order2);

        OrderItem orderItem3 = createOrderItem(savedProduct1);
        orderItem3.setQuantity(4);
        orderItem3.setOrder(savedOrder2);
        OrderItem savedOrderItem3 = orderItemRepository.save(orderItem3);

        BigDecimal amount2 = savedOrderItem3.getOrderedProductPrice().multiply(BigDecimal.valueOf(savedOrderItem3.getQuantity()));

        savedOrder2.getOrderItems().add(savedOrderItem3);
        savedOrder2.setTotalAmount(amount2);
        orderRepository.save(savedOrder2);

        mockMvc.perform(get("/api/admin/orders")
                        .with(user(userDetails))
                        .param("sortBy", "orderId")
                        .param("sortOrder", "asc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true))

                .andExpect(jsonPath("$.content.length()").value(2))

                .andExpect(jsonPath("$.content[0].orderId").value(savedOrder1.getOrderId()))
                .andExpect(jsonPath("$.content[0].email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.content[0].totalAmount").value(amount1.doubleValue()))
                .andExpect(jsonPath("$.content[0].orderDate").exists())
                .andExpect(jsonPath("$.content[0].orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.content[0].addressId").value(savedAddress.getAddressId()))

                .andExpect(jsonPath("$.content[0].payment.paymentId").value(savedPayment1.getPaymentId()))
                .andExpect(jsonPath("$.content[0].payment.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.content[0].payment.pgStatus").value("SUCCESS"))

                .andExpect(jsonPath("$.content[0].orderItems.length()").value(2))

                .andExpect(jsonPath("$.content[0].orderItems[0].productId").value(savedProduct1.getProductId()))
                .andExpect(jsonPath("$.content[0].orderItems[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[0].orderItems[0].quantityOrdered").value(savedOrderItem1.getQuantity()))
                .andExpect(jsonPath("$.content[0].orderItems[0].orderedProductPrice").value(savedOrderItem1.getOrderedProductPrice().doubleValue()))

                .andExpect(jsonPath("$.content[0].orderItems[1].productId").value(savedProduct2.getProductId()))
                .andExpect(jsonPath("$.content[0].orderItems[1].productName").value(savedProduct2.getProductName()))
                .andExpect(jsonPath("$.content[0].orderItems[1].quantityOrdered").value(savedOrderItem2.getQuantity()))
                .andExpect(jsonPath("$.content[0].orderItems[1].orderedProductPrice").value(savedOrderItem2.getOrderedProductPrice().doubleValue()))

                .andExpect(jsonPath("$.content[1].orderId").value(savedOrder2.getOrderId()))
                .andExpect(jsonPath("$.content[1].email").value(savedUser.getEmail()))
                .andExpect(jsonPath("$.content[1].totalAmount").value(amount2.doubleValue()))
                .andExpect(jsonPath("$.content[1].orderDate").exists())
                .andExpect(jsonPath("$.content[1].orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.content[1].addressId").value(savedAddress.getAddressId()))

                .andExpect(jsonPath("$.content[1].payment.paymentId").value(savedPayment2.getPaymentId()))
                .andExpect(jsonPath("$.content[1].payment.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.content[1].payment.pgStatus").value("SUCCESS"))

                .andExpect(jsonPath("$.content[1].orderItems.length()").value(1))

                .andExpect(jsonPath("$.content[1].orderItems[0].productId").value(savedProduct1.getProductId()))
                .andExpect(jsonPath("$.content[1].orderItems[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[1].orderItems[0].quantityOrdered").value(savedOrderItem3.getQuantity()))
                .andExpect(jsonPath("$.content[1].orderItems[0].orderedProductPrice").value(savedOrderItem3.getOrderedProductPrice().doubleValue()));
    }

    @Test
    void getAllOrdersShouldReturnBadRequestIfPaginationParametersAreInvalid() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/admin/orders")
                        .with(user(userDetails))
                        .param("sortBy", "orderId")
                        .param("sortOrder", "des"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid sort order"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getAllOrdersShouldReturnEmptyPageIfNotOrdersExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/admin/orders")
                        .with(user(userDetails)))
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
    void getAllOrdersShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/admin/orders")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAllOrdersShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// getAllSellerOrders()
    @Test
    void getAllSellerOrdersShouldReturnAllOrdersBelongingToThatSeller() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();
        Role roleSeller = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        User seller1 = createUser("Test Seller 1", "seller1@gmail.com", "password");
        seller1.getRoles().add(roleSeller);
        User savedSeller1 = userRepository.save(seller1);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller1);

        User seller2 = createUser("Test Seller 2", "seller2@gmail.com", "password");
        seller2.getRoles().add(roleSeller);
        User savedSeller2 = userRepository.save(seller2);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedSeller1, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = createProduct(savedSeller2, savedCategory, "Lord of the Rings", "default.png",
                "Lord of the Rings: Return of the King", 10,
                new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        Product savedProduct2 = productRepository.save(product2);

        Payment payment1 = createPayment(PaymentMethod.CARD, "MOCK1");
        Payment savedPayment1 = paymentRepository.save(payment1);

        Order order = createOrder(savedUser, savedAddress, savedPayment1);
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem1 = createOrderItem(savedProduct1);
        orderItem1.setQuantity(3);
        orderItem1.setOrder(savedOrder);
        OrderItem savedOrderItem1 = orderItemRepository.save(orderItem1);

        OrderItem orderItem2 = createOrderItem(savedProduct2);
        orderItem2.setQuantity(5);
        orderItem2.setOrder(savedOrder);
        OrderItem savedOrderItem2 = orderItemRepository.save(orderItem2);

        BigDecimal amount = savedOrderItem1.getOrderedProductPrice()
                .multiply(BigDecimal.valueOf(savedOrderItem1.getQuantity()))
                .add(savedOrderItem2.getOrderedProductPrice()
                        .multiply(BigDecimal.valueOf(savedOrderItem2.getQuantity())));

        savedOrder.getOrderItems().add(savedOrderItem1);
        savedOrder.getOrderItems().add(savedOrderItem2);
        savedOrder.setTotalAmount(amount);
        orderRepository.save(savedOrder);

        BigDecimal sellerAmount = savedOrderItem1.getOrderedProductPrice()
                .multiply(BigDecimal.valueOf(savedOrderItem1.getQuantity()));

        mockMvc.perform(get("/api/seller/orders")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value(savedOrder.getOrderId()))
                .andExpect(jsonPath("$.content[0].orderDate").exists())
                .andExpect(jsonPath("$.content[0].sellerAmount").value(sellerAmount.doubleValue()))
                .andExpect(jsonPath("$.content[0].orderStatus").value(CREATED.name()))

                .andExpect(jsonPath("$.content[0].orderItems.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderItems[0].orderItemId").isNumber())
                .andExpect(jsonPath("$.content[0].orderItems[0].productId").value(savedProduct1.getProductId()))
                .andExpect(jsonPath("$.content[0].orderItems[0].productName").value(savedProduct1.getProductName()))
                .andExpect(jsonPath("$.content[0].orderItems[0].description").value(savedProduct1.getDescription()))
                .andExpect(jsonPath("$.content[0].orderItems[0].image").value("http://localhost:8080/images/" + savedProduct1.getImage()))
                .andExpect(jsonPath("$.content[0].orderItems[0].quantityOrdered").value(savedOrderItem1.getQuantity()))
                .andExpect(jsonPath("$.content[0].orderItems[0].orderedProductPrice").value(savedOrderItem1.getOrderedProductPrice().doubleValue()))
                .andExpect(jsonPath("$.content[0].orderItems[0].discount").value(savedOrderItem1.getDiscount().doubleValue()))

                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(AppConstants.PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void getAllSellerOrdersShouldReturnEmptyPageIfSellerHasNoOrders() throws Exception {
        Role roleUser = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();
        Role roleSeller = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(roleUser);
        User savedUser = userRepository.save(user);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        User seller1 = createUser("Test Seller 1", "seller1@gmail.com", "password");
        seller1.getRoles().add(roleSeller);
        User savedSeller1 = userRepository.save(seller1);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller1);

        User seller2 = createUser("Test Seller 2", "seller2@gmail.com", "password");
        seller2.getRoles().add(roleSeller);
        User savedSeller2 = userRepository.save(seller2);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedSeller2, savedCategory, "Lord of the Rings", "default.png",
                "Lord of the Rings: Return of the King", 10,
                new BigDecimal("150"), new BigDecimal("10"), new BigDecimal("135"));
        Product savedProduct = productRepository.save(product);

        Payment payment = createPayment(PaymentMethod.CARD, "MOCK");
        Payment savedPayment = paymentRepository.save(payment);

        Order order = createOrder(savedUser, savedAddress, savedPayment);
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = createOrderItem(savedProduct);
        orderItem.setQuantity(5);
        orderItem.setOrder(savedOrder);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        BigDecimal amount = savedOrderItem.getOrderedProductPrice()
                .multiply(BigDecimal.valueOf(savedOrderItem.getQuantity()));

        savedOrder.getOrderItems().add(savedOrderItem);
        savedOrder.setTotalAmount(amount);
        orderRepository.save(savedOrder);

        mockMvc.perform(get("/api/seller/orders")
                        .with(user(userDetails)))
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
    void getAllSellerOrdersShouldReturnBadRequestIfPaginationParametersAreInvalid() throws Exception {
        Role roleSeller = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User seller1 = createUser("Test Seller 1", "seller1@gmail.com", "password");
        seller1.getRoles().add(roleSeller);
        User savedSeller1 = userRepository.save(seller1);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedSeller1);

        mockMvc.perform(get("/api/seller/orders")
                        .with(user(userDetails))
                        .param("sortOrder", "ascending"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid sort order"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void getAllSellerOrdersShouldReturnForbiddenIfUserHasRoleUser() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);
        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/seller/orders")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAllSellerOrdersShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        userRepository.save(user);

        mockMvc.perform(get("/api/seller/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// updateOrderStatus()
    @Test
    void updateOrderStatusShouldSuccessfullyUpdateOrderStatus() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        Payment payment = createPayment(PaymentMethod.CARD, "MOCK");
        Payment savedPayment = paymentRepository.save(payment);

        Order order = createOrder(savedUser, savedAddress, savedPayment);
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = createOrderItem(savedProduct);
        orderItem.setOrder(savedOrder);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        BigDecimal amount = savedOrderItem.getOrderedProductPrice()
                .multiply(BigDecimal.valueOf(savedOrderItem.getQuantity()));

        savedOrder.getOrderItems().add(savedOrderItem);
        savedOrder.setTotalAmount(amount);
        orderRepository.save(savedOrder);

        assertEquals(CREATED, savedOrder.getOrderStatus());

        Long orderId = savedOrder.getOrderId();

        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(OrderStatus.DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(savedOrder.getOrderId()))
                .andExpect(jsonPath("$.email").value(savedOrder.getEmail()))

                .andExpect(jsonPath("$.orderItems.length()").value(1))
                .andExpect(jsonPath("$.orderItems[0].orderItemId").value(savedOrderItem.getOrderItemId()))
                .andExpect(jsonPath("$.orderItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.orderItems[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.orderItems[0].quantityOrdered").value(savedOrderItem.getQuantity()))
                .andExpect(jsonPath("$.orderItems[0].orderedProductPrice").value(savedOrderItem.getOrderedProductPrice().doubleValue()))
                .andExpect(jsonPath("$.orderItems[0].discount").value(savedOrderItem.getDiscount().doubleValue()))

                .andExpect(jsonPath("$.orderDate").exists())

                .andExpect(jsonPath("$.payment.paymentId").value(savedPayment.getPaymentId()))
                .andExpect(jsonPath("$.payment.paymentMethod").value(savedPayment.getPaymentMethod().name()))
                .andExpect(jsonPath("$.payment.pgStatus").value(savedPayment.getPgStatus().name()))

                .andExpect(jsonPath("$.totalAmount").value(savedOrder.getTotalAmount().doubleValue()))
                .andExpect(jsonPath("$.orderStatus").value(orderStatusUpdateDTO.getStatus().name()))
                .andExpect(jsonPath("$.addressId").value(savedOrder.getAddress().getAddressId()));

        Order orderFromDB = orderRepository.findById(savedOrder.getOrderId()).orElseThrow();
        assertEquals(orderStatusUpdateDTO.getStatus(), orderFromDB.getOrderStatus());
    }

    @Test
    void updateOrderStatusShouldReturnBadRequestIfOrderStatusIsInvalid() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = 1L;
        String json = """
                {
                    "status": "INVALID_STATUS"
                }
                """;

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid enum value supplied in request body"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateOrderStatusShouldReturnBadRequestIfOrderStatusIsNull() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = 1L;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("must not be null"));
    }

    @Test
    void updateOrderStatusShouldReturnNotFoundIfOrderDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = Long.MAX_VALUE;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Order not found with orderId: " + orderId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateOrderStatusShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = 1L;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", orderId)
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
    void updateOrderStatusShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        userRepository.save(user);

        Long orderId = 1L;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/admin/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"))
                .andExpect(jsonPath("$.status").value(401));
    }

    /// updateOrderStatusSeller()
    @Test
    void updateOrderStatusSellerShouldSuccessfullyUpdateOrderStatus() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        Product savedProduct = productRepository.save(product);

        Payment payment = createPayment(PaymentMethod.CARD, "MOCK");
        Payment savedPayment = paymentRepository.save(payment);

        Order order = createOrder(savedUser, savedAddress, savedPayment);
        Order savedOrder = orderRepository.save(order);

        OrderItem orderItem = createOrderItem(savedProduct);
        orderItem.setOrder(savedOrder);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        BigDecimal amount = savedOrderItem.getOrderedProductPrice()
                .multiply(BigDecimal.valueOf(savedOrderItem.getQuantity()));

        savedOrder.getOrderItems().add(savedOrderItem);
        savedOrder.setTotalAmount(amount);
        orderRepository.save(savedOrder);

        assertEquals(CREATED, savedOrder.getOrderStatus());

        Long orderId = savedOrder.getOrderId();

        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(OrderStatus.DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/seller/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(savedOrder.getOrderId()))
                .andExpect(jsonPath("$.email").value(savedOrder.getEmail()))

                .andExpect(jsonPath("$.orderItems.length()").value(1))
                .andExpect(jsonPath("$.orderItems[0].orderItemId").value(savedOrderItem.getOrderItemId()))
                .andExpect(jsonPath("$.orderItems[0].productId").value(savedProduct.getProductId()))
                .andExpect(jsonPath("$.orderItems[0].productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.orderItems[0].quantityOrdered").value(savedOrderItem.getQuantity()))
                .andExpect(jsonPath("$.orderItems[0].orderedProductPrice").value(savedOrderItem.getOrderedProductPrice().doubleValue()))
                .andExpect(jsonPath("$.orderItems[0].discount").value(savedOrderItem.getDiscount().doubleValue()))

                .andExpect(jsonPath("$.orderDate").exists())

                .andExpect(jsonPath("$.payment.paymentId").value(savedPayment.getPaymentId()))
                .andExpect(jsonPath("$.payment.paymentMethod").value(savedPayment.getPaymentMethod().name()))
                .andExpect(jsonPath("$.payment.pgStatus").value(savedPayment.getPgStatus().name()))

                .andExpect(jsonPath("$.totalAmount").value(savedOrder.getTotalAmount().doubleValue()))
                .andExpect(jsonPath("$.orderStatus").value(orderStatusUpdateDTO.getStatus().name()))
                .andExpect(jsonPath("$.addressId").value(savedOrder.getAddress().getAddressId()));

        Order orderFromDB = orderRepository.findById(savedOrder.getOrderId()).orElseThrow();
        assertEquals(orderStatusUpdateDTO.getStatus(), orderFromDB.getOrderStatus());
    }

    @Test
    void updateOrderStatusSellerShouldReturnBadRequestIfOrderStatusIsInvalid() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = 1L;
        String json = """
                {
                    "status": "INVALID_STATUS"
                }
                """;

        mockMvc.perform(put("/api/seller/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid enum value supplied in request body"))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateOrderStatusSellerShouldReturnBadRequestIfOrderStatusIsNull() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = 1L;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/seller/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("must not be null"));
    }

    @Test
    void updateOrderStatusSellerShouldReturnNotFoundIfOrderDoesNotExist() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = Long.MAX_VALUE;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/seller/orders/{orderId}/status", orderId)
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Order not found with orderId: " + orderId))
                .andExpect(jsonPath("$.status").value(false));
    }

    @Test
    void updateOrderStatusSellerShouldReturnForbiddenIfUserHasRoleUser() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Long orderId = 1L;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/seller/orders/{orderId}/status", orderId)
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
    void updateOrderStatusSellerShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        Role savedRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(savedRole);
        userRepository.save(user);

        Long orderId = 1L;
        OrderStatusUpdateDTO orderStatusUpdateDTO = new OrderStatusUpdateDTO();
        orderStatusUpdateDTO.setStatus(DELIVERED);
        String json = objectMapper.writeValueAsString(orderStatusUpdateDTO);

        mockMvc.perform(put("/api/seller/orders/{orderId}/status", orderId)
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

    private Address createAddress(User user) {
        Address address = new Address();
        address.setStreet("123 Maple Street, Apt 4B");
        address.setBuildingName("Oakwood Commons");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setPincode("627045");
        address.setUser(user);
        return address;
    }

    private OrderRequestDTO createOrderRequestDTO(Long addressId, PaymentMethod paymentMethod, String paymentIntentId) {
        OrderRequestDTO orderRequestDTO = new OrderRequestDTO();
        orderRequestDTO.setAddressId(addressId);
        orderRequestDTO.setPaymentMethod(paymentMethod);
        orderRequestDTO.setPaymentIntentId(paymentIntentId);
        return orderRequestDTO;
    }

    private Payment createPayment(PaymentMethod paymentMethod, String paymentIntentId) {
        return new Payment(
                paymentMethod,
                paymentIntentId,
                PaymentStatus.SUCCESS,
                "Payment Successful",
                "Stripe"
        );
    }

    private Order createOrder(User user, Address address, Payment payment) {
        Order order = new Order();
        order.setEmail(user.getEmail());
        order.setPayment(payment);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setAddress(address);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    private OrderItem createOrderItem(Product product) {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(3);
        orderItem.setDiscount(product.getDiscount());
        orderItem.setOrderedProductPrice(product.getSpecialPrice());
        return orderItem;
    }
}
