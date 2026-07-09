package com.ecommerce.project.integration;

import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.AppRole;
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
import com.ecommerce.project.repositories.AddressRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AnalyticsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    /// getAnalytics()
    @Test
    void getAnalyticsShouldSuccessfullyReturnAnalyticsDataIfUserIsAdmin() throws Exception {
        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(adminRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address = createAddress(savedUser);
        Address savedAddress = addressRepository.save(address);

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

        Payment payment1 = createPayment(PaymentMethod.CARD);
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

        Payment payment2 = createPayment(PaymentMethod.CARD);
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

        Long productCount = productRepository.count();
        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        Long totalOrders = orderRepository.count();

        mockMvc.perform(get("/api/admin/app/analytics")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productCount").value(String.valueOf(productCount)))
                .andExpect(jsonPath("$.totalRevenue").value(totalRevenue.toString()))
                .andExpect(jsonPath("$.totalOrders").value(String.valueOf(totalOrders)));
    }

    @Test
    void getAnalyticsShouldReturnZeroRevenueWhenNoOrdersExist() throws Exception {
        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(adminRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        Address address = createAddress(savedUser);
        addressRepository.save(address);

        Category category = createCategory("books");
        Category savedCategory = categoryRepository.save(category);

        Product product1 = createProduct(savedUser, savedCategory, "Harry Potter 3", "default.png",
                "Harry Potter and the Prisoner of Azkaban", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product1);

        Product product2 = createProduct(savedUser, savedCategory, "Lord of the Rings", "default.png",
                "Lord of the Rings: Return of the King", 10,
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"));
        productRepository.save(product2);

        Long productCount = productRepository.count();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Long totalOrders = orderRepository.count();

        mockMvc.perform(get("/api/admin/app/analytics")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productCount").value(String.valueOf(productCount)))
                .andExpect(jsonPath("$.totalRevenue").value(totalRevenue.toString()))
                .andExpect(jsonPath("$.totalOrders").value(String.valueOf(totalOrders)));
    }

    @Test
    void getAnalyticsShouldReturnForbiddenIfUserIsNotAdmin() throws Exception {
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER).orElseThrow();

        User user = createUser("Test User", "user@gmail.com", "password");
        user.getRoles().add(userRole);
        User savedUser = userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);

        mockMvc.perform(get("/api/admin/app/analytics")
                        .with(user(userDetails)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getAnalyticsShouldReturnUnauthorizedIfUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/app/analytics"))
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

    private Payment createPayment(PaymentMethod paymentMethod) {
        return new Payment(
                paymentMethod,
                "MOCK",
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
