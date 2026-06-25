package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.OutOfStockException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
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
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemResponseDTO;
import com.ecommerce.project.payload.OrderResponse;
import com.ecommerce.project.payload.PaymentDTO;
import com.ecommerce.project.payload.SellerOrderDTO;
import com.ecommerce.project.payload.SellerOrderResponse;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.OrderItemRepository;
import com.ecommerce.project.repositories.OrderRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import com.ecommerce.project.util.ImageUrlUtil;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
public class OrderServiceImplTest {
    @InjectMocks
    OrderServiceImpl orderService;

    @Mock
    CartRepository cartRepository;

    @Mock
    AddressRepository addressRepository;

    @Mock
    PaymentService paymentService;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    AuthUtil authUtil;

    @Mock
    ModelMapper modelMapper;

    @Mock
    ImageUrlUtil imageUrlUtil;

    @Mock
    PaginationValidator paginationValidator;

    /// placeOrder()
    @Test
    void placeOrderShouldSuccessfullyPlaceOrder() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Cart cart = createCart(user);
        cart.setCartId(1L);
        cart.setUser(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);

        cart.setCartItems(new ArrayList<>(List.of(cartItem)));
        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

        Address address = createAddress(user);

        String email = user.getEmail();
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        Payment payment = createPayment(paymentMethod);

        Order savedOrder = createOrder(user, address, payment);
        savedOrder.setTotalAmount(cart.getTotalPrice());

        OrderItem orderItem = createOrderItem(product);
        orderItem.setOrder(savedOrder);

        List<OrderItem> orderItems = List.of(orderItem);

        OrderItemResponseDTO orderItemResponseDTO = createOrderItemResponseDTO(orderItem);
        List<OrderItemResponseDTO> orderItemResponseDTOS = List.of(orderItemResponseDTO);

        PaymentDTO paymentDTO = createPaymentDTO(payment);

        OrderDTO orderDTO = createOrderDTO(savedOrder);
        orderDTO.setOrderItems(orderItemResponseDTOS);
        orderDTO.setPayment(paymentDTO);

        when(cartRepository.findCartByEmail(email))
                .thenReturn(cart);

        when(addressRepository.findByIdAndUserEmailId(1L, email))
                .thenReturn(Optional.of(address));

        when(paymentService.createSuccessfulPayment(any(Order.class), eq(paymentMethod)))
                .thenReturn(payment);

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        when(orderItemRepository.saveAll(anyList()))
                .thenReturn(orderItems);

        when(modelMapper.map(savedOrder, OrderDTO.class))
                .thenReturn(orderDTO);

        when(modelMapper.map(any(OrderItem.class), eq(OrderItemResponseDTO.class)))
                .thenReturn(orderItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(cartItem.getProduct().getImage()))
                .thenReturn("http://localhost/images/" + cartItem.getProduct().getImage());

        OrderDTO result = orderService.placeOrder(email, 1L, paymentMethod);

        assertNotNull(result);

        assertEquals(savedOrder.getOrderId(), result.getOrderId());
        assertEquals(email, result.getEmail());
        assertEquals(savedOrder.getTotalAmount(), result.getTotalAmount());
        assertEquals(OrderStatus.CREATED, result.getOrderStatus());
        assertEquals(1L, result.getAddressId());

        assertEquals(1, result.getOrderItems().size());
        OrderItemResponseDTO item = result.getOrderItems().getFirst();
        assertEquals(product.getProductId(), item.getProductId());
        assertEquals(product.getProductName(), item.getProductName());
        assertEquals(cartItem.getQuantity(), item.getQuantityOrdered());
        assertEquals("http://localhost/images/" + product.getImage(), item.getImage());

        assertEquals(7, product.getQuantity());
        assertTrue(cart.getCartItems().isEmpty());
        assertEquals(0, cart.getTotalPrice().compareTo(BigDecimal.ZERO));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order persistedOrder = orderCaptor.getValue();

        assertEquals(email, persistedOrder.getEmail());
        assertEquals(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())),
                persistedOrder.getTotalAmount());
        assertEquals(OrderStatus.CREATED, persistedOrder.getOrderStatus());
        assertEquals(address, persistedOrder.getAddress());
        assertEquals(payment, persistedOrder.getPayment());
        assertNotNull(persistedOrder.getOrderDate());

        assertEquals(paymentMethod, persistedOrder.getPayment().getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, persistedOrder.getPayment().getPgStatus());

        ArgumentCaptor<List<OrderItem>> orderItemCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(orderItemCaptor.capture());
        OrderItem capturedItem = orderItemCaptor.getValue().getFirst();

        assertEquals(cartItem.getProduct(), capturedItem.getProduct());
        assertEquals(cartItem.getQuantity(), capturedItem.getQuantity());
        assertEquals(savedOrder.getOrderId(), capturedItem.getOrder().getOrderId());
        assertEquals(cartItem.getProduct().getDiscount(), capturedItem.getDiscount());
        assertEquals(cartItem.getProductPrice(), capturedItem.getOrderedProductPrice());

        verify(cartRepository).findCartByEmail(email);
        verify(addressRepository).findByIdAndUserEmailId(1L, email);
        verify(paymentService).createSuccessfulPayment(any(Order.class), eq(paymentMethod));
        verify(productRepository).save(product);
        verify(cartRepository).save(cart);
        verify(modelMapper).map(savedOrder, OrderDTO.class);
        verify(modelMapper).map(orderItem, OrderItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(product.getImage());
    }

    @Test
    void placeOrderShouldThrowResourceNotFoundExceptionIfCartDoesNotExist() {
        User user = createUser(1L);
        String email = user.getEmail();
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        when(cartRepository.findCartByEmail(email))
                .thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.placeOrder(email, 1L, paymentMethod)
        );

        assertEquals("Cart not found with email: " + email, exception.getMessage());

        verify(cartRepository).findCartByEmail(email);
        verifyNoInteractions(addressRepository);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void placeOrderShouldThrowResourceNotFoundExceptionIfAddressDoesNotExist() {
        User user = createUser(1L);
        Cart cart = createCart(user);
        String email = user.getEmail();
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        when(cartRepository.findCartByEmail(email))
                .thenReturn(cart);

        when(addressRepository.findByIdAndUserEmailId(1L, email))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.placeOrder(email, 1L, paymentMethod)
        );

        assertEquals("Address not found with addressId: 1", exception.getMessage());

        verify(cartRepository).findCartByEmail(email);
        verify(addressRepository).findByIdAndUserEmailId(1L, email);
        verifyNoInteractions(paymentService);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void placeOrderShouldThrowApiExceptionIfCartIsEmpty() {
        User user = createUser(1L);
        Cart cart = createCart(user);
        Address address = createAddress(user);
        String email = user.getEmail();
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        when(cartRepository.findCartByEmail(email))
                .thenReturn(cart);

        when(addressRepository.findByIdAndUserEmailId(1L, email))
                .thenReturn(Optional.of(address));

        APIException exception = assertThrows(
                APIException.class,
                () -> orderService.placeOrder(email, 1L, paymentMethod)
        );

        assertEquals("Cart is empty!", exception.getMessage());

        verify(cartRepository).findCartByEmail(email);
        verify(addressRepository).findByIdAndUserEmailId(1L, email);
        verifyNoInteractions(paymentService);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void placeOrderShouldThrowOutOfStockExceptionIfRequestedQuantityIsGreaterThanAvailableQuantity() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);
        product.setQuantity(10);

        Cart cart = createCart(user);
        Address address = createAddress(user);

        CartItem cartItem = createCartItem(product);
        cartItem.setCart(cart);
        cartItem.setQuantity(11);

        cart.setCartItems(new ArrayList<>(List.of(cartItem)));
        cart.setTotalPrice(cartItem.getProductPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));

        String email = user.getEmail();
        PaymentMethod paymentMethod = PaymentMethod.CARD;

        when(cartRepository.findCartByEmail(email))
                .thenReturn(cart);

        when(addressRepository.findByIdAndUserEmailId(1L, email))
                .thenReturn(Optional.of(address));

        OutOfStockException exception = assertThrows(
                OutOfStockException.class,
                () -> orderService.placeOrder(email, 1L, paymentMethod)
        );

        assertEquals("Inventory Issues", exception.getMessage());
        assertEquals(1, exception.getErrors().size());
        assertTrue(exception.getErrors().containsKey(product.getProductName()));
        assertEquals("Requested: " + cartItem.getQuantity() + ", Available: " + product.getQuantity(),
                exception.getErrors().get(product.getProductName()));
        assertEquals(10, product.getQuantity());

        verify(cartRepository).findCartByEmail(email);
        verify(addressRepository).findByIdAndUserEmailId(1L, email);
        verifyNoInteractions(paymentService);
        verifyNoInteractions(orderRepository);
        verifyNoInteractions(orderItemRepository);
        verifyNoInteractions(productRepository);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    /// getAllOrders()
    @Test
    void getAllOrdersShouldReturnAllOrders() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Address address = createAddress(user);
        Payment payment = createPayment(PaymentMethod.CARD);

        Order order = createOrder(user, address, payment);

        OrderItem orderItem = createOrderItem(product);
        orderItem.setOrder(order);

        List<OrderItem> orderItems = List.of(orderItem);
        order.setOrderItems(orderItems);
        order.setTotalAmount(orderItems.getFirst().getOrderedProductPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));

        List<Order> orders = List.of(order);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(orders, pageable, orders.size());

        OrderItemResponseDTO orderItemResponseDTO = createOrderItemResponseDTO(orderItem);
        List<OrderItemResponseDTO> orderItemResponseDTOS = List.of(orderItemResponseDTO);

        PaymentDTO paymentDTO = createPaymentDTO(payment);

        OrderDTO orderDTO = createOrderDTO(order);
        orderDTO.setOrderItems(orderItemResponseDTOS);
        orderDTO.setPayment(paymentDTO);
        orderDTO.setAddressId(address.getAddressId());

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(orderPage);

        when(modelMapper.map(order, OrderDTO.class))
                .thenReturn(orderDTO);

        when(modelMapper.map(orderItem, OrderItemResponseDTO.class))
                .thenReturn(orderItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(orderItem.getProduct().getImage()))
                .thenReturn("http://localhost/images/" + orderItem.getProduct().getImage());

        OrderResponse result = orderService.getAllOrders(0, 10, "orderId", "asc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        OrderDTO returnedOrder = result.getContent().getFirst();
        assertEquals(order.getTotalAmount(), returnedOrder.getTotalAmount());
        assertEquals(order.getOrderStatus(), returnedOrder.getOrderStatus());
        assertEquals(order.getOrderId(), returnedOrder.getOrderId());
        assertEquals(order.getEmail(), returnedOrder.getEmail());
        assertEquals(address.getAddressId(), returnedOrder.getAddressId());

        OrderItemResponseDTO item = returnedOrder.getOrderItems().getFirst();
        assertEquals(orderItem.getOrderItemId(), item.getOrderItemId());
        assertEquals(orderItem.getProduct().getProductId(), item.getProductId());
        assertEquals(orderItem.getQuantity(), item.getQuantityOrdered());
        assertEquals(orderItem.getOrderedProductPrice(), item.getOrderedProductPrice());
        assertEquals(orderItem.getDiscount(), item.getDiscount());

        assertEquals(product.getProductName(), item.getProductName());
        assertEquals(product.getDescription(), item.getDescription());
        assertEquals("http://localhost/images/" + product.getImage(), item.getImage());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();

        assertEquals(0, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());

        Sort.Order sortOrder = captured.getSort().iterator().next();
        assertEquals("orderId", sortOrder.getProperty());
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());

        verify(paginationValidator).validate(eq(0), eq(10), eq("orderId"), eq("asc"), anyList());
        verify(modelMapper).map(order, OrderDTO.class);
        verify(modelMapper).map(orderItem, OrderItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(orderItem.getProduct().getImage());
    }

    @Test
    void getAllOrdersShouldReturnEmptyResponseWhenNoOrdersExist() {
        Page<Order> emptyPage= Page.empty();

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        OrderResponse result = orderService.getAllOrders(0, 10, "orderId", "asc");

        assertTrue(result.getContent().isEmpty());
        assertEquals(0L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        verify(paginationValidator).validate(eq(0), eq(10), eq("orderId"), eq("asc"), anyList());
        verify(orderRepository).findAll(any(Pageable.class));
        verify(modelMapper, never()).map(any(Order.class), eq(OrderDTO.class));
        verify(modelMapper, never()).map(any(OrderItem.class), eq(OrderItemResponseDTO.class));
        verify(imageUrlUtil, never()).constructImageUrl(any(String.class));
    }

    /// updateOrder()
    @Test
    void updateOrderShouldSuccessfullyUpdateOrder() {
        User user = createUser(1L);
        Category category = createCategory();

        Product product = createProduct(user, category);
        product.setProductId(1L);

        Payment payment = createPayment(PaymentMethod.CARD);
        Address address = createAddress(user);

        Order order = createOrder(user, address, payment);

        OrderItem orderItem = createOrderItem(product);
        orderItem.setOrder(order);

        List<OrderItem> orderItems = List.of(orderItem);
        order.setOrderItems(orderItems);
        order.setTotalAmount(orderItems.getFirst().getOrderedProductPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));

        OrderItemResponseDTO orderItemResponseDTO = createOrderItemResponseDTO(orderItem);
        List<OrderItemResponseDTO> orderItemResponseDTOS = List.of(orderItemResponseDTO);

        PaymentDTO paymentDTO = createPaymentDTO(payment);

        OrderStatus orderStatus = OrderStatus.DELIVERED;

        OrderDTO orderDTO = createOrderDTO(order);
        orderDTO.setOrderStatus(orderStatus);
        orderDTO.setOrderItems(orderItemResponseDTOS);
        orderDTO.setPayment(paymentDTO);
        orderDTO.setAddressId(address.getAddressId());

        when(orderRepository.findOrderWithDetailsByOrderId(1L))
                .thenReturn(Optional.of(order));

        when(modelMapper.map(order, OrderDTO.class))
                .thenReturn(orderDTO);

        when(modelMapper.map(orderItem, OrderItemResponseDTO.class))
                .thenReturn(orderItemResponseDTO);

        when(imageUrlUtil.constructImageUrl(orderItem.getProduct().getImage()))
                .thenReturn("http://localhost/images/" + orderItem.getProduct().getImage());

        OrderDTO result = orderService.updateOrder(1L, orderStatus);

        assertNotNull(result);
        assertEquals(OrderStatus.DELIVERED, result.getOrderStatus());
        assertEquals(OrderStatus.DELIVERED, order.getOrderStatus());

        verify(orderRepository).findOrderWithDetailsByOrderId(1L);
        verify(modelMapper).map(order, OrderDTO.class);
        verify(modelMapper).map(orderItem, OrderItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(orderItem.getProduct().getImage());
    }

    @Test
    void updateOrderShouldThrowResourceNotFoundExceptionIfOrderDoesNotExist() {
        when(orderRepository.findOrderWithDetailsByOrderId(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.updateOrder(1L, OrderStatus.DELIVERED)
        );

        assertEquals("Order not found with orderId: 1", exception.getMessage());

        verify(orderRepository).findOrderWithDetailsByOrderId(1L);
        verifyNoInteractions(modelMapper);
        verifyNoInteractions(imageUrlUtil);
    }

    /// getAllSellerOrders()
    @Test
    void getAllSellerOrdersShouldReturnAllOrdersForProductsOwnedByTheSeller() {
        User seller1 = createSeller(1L, "Test Seller 1");
        User seller2 = createSeller(2L, "Test Seller 2");
        User user = createUser(3L);

        Category category1 = createCategory();
        category1.setCategoryId(1L);
        category1.setCategoryName("Books");

        Product product1 = createProduct(seller1, category1);
        product1.setProductId(1L);
        product1.setProductName("Harry Potter 3");
        product1.setImage("default1.png");
        product1.setDescription("Harry Potter and the Prisoner of Azkaban");
        product1.setQuantity(10);
        product1.setPrice(new BigDecimal("100"));
        product1.setDiscount(new BigDecimal("10"));
        product1.setSpecialPrice(new BigDecimal("90.00"));

        Category category2 = createCategory();
        category2.setCategoryId(2L);
        category2.setCategoryName("Electronics");

        Product product2 = createProduct(seller2, category2);
        product2.setProductId(2L);
        product2.setProductName("iPhone 16");
        product2.setImage("default2.png");
        product2.setDescription("Brand new iPhone 16");
        product2.setQuantity(15);
        product2.setPrice(new BigDecimal("1000"));
        product2.setDiscount(new BigDecimal("10"));
        product2.setSpecialPrice(new BigDecimal("900.00"));

        Address address = createAddress(user);
        Payment payment = createPayment(PaymentMethod.CARD);

        OrderItem orderItem1 = createOrderItem(product1);
        orderItem1.setOrderItemId(1L);
        orderItem1.setQuantity(3);

        OrderItem orderItem2 = createOrderItem(product2);
        orderItem2.setOrderItemId(2L);
        orderItem2.setQuantity(4);

        Order sellerOrder = createOrder(user, address, payment);

        orderItem1.setOrder(sellerOrder);
        orderItem2.setOrder(sellerOrder);

        List<OrderItem> orderItems = List.of(orderItem1, orderItem2);
        sellerOrder.setOrderItems(orderItems);
        BigDecimal totalAmount = orderItem1.getOrderedProductPrice().multiply(BigDecimal.valueOf(orderItem1.getQuantity()))
                        .add(orderItem2.getOrderedProductPrice().multiply(BigDecimal.valueOf(orderItem2.getQuantity())));
        sellerOrder.setTotalAmount(totalAmount);

        List<Order> sellerOrders = List.of(sellerOrder);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(sellerOrders, pageable, sellerOrders.size());

        OrderItemResponseDTO responseDTO = createOrderItemResponseDTO(orderItem1);

        SellerOrderDTO sellerOrderDTO = new SellerOrderDTO();
        sellerOrderDTO.setOrderId(sellerOrder.getOrderId());
        sellerOrderDTO.setOrderStatus(sellerOrder.getOrderStatus());

        doNothing()
                .when(paginationValidator)
                .validate(anyInt(), anyInt(), anyString(), anyString(), anyList());

        when(authUtil.loggedInUser())
                .thenReturn(seller1);

        when(orderRepository.findOrderBySellerId(eq(seller1.getUserId()), any(Pageable.class)))
                .thenReturn(orderPage);

        when(modelMapper.map(sellerOrder, SellerOrderDTO.class))
                .thenReturn(sellerOrderDTO);

        when(modelMapper.map(orderItem1, OrderItemResponseDTO.class))
                .thenReturn(responseDTO);

        when(imageUrlUtil.constructImageUrl(orderItem1.getProduct().getImage()))
                .thenReturn("http://localhost/images/" + orderItem1.getProduct().getImage());

        SellerOrderResponse result = orderService.getAllSellerOrders(0, 10, "orderId", "asc");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLastPage());

        SellerOrderDTO returnedSellerOrder = result.getContent().getFirst();
        assertEquals(1, returnedSellerOrder.getOrderItems().size());

        BigDecimal expectedSellerAmount = orderItem1.getOrderedProductPrice().multiply(BigDecimal.valueOf(orderItem1.getQuantity()));
        assertEquals(expectedSellerAmount, returnedSellerOrder.getSellerAmount());

        assertTrue(
                returnedSellerOrder.getOrderItems()
                        .stream()
                        .noneMatch(i -> i.getOrderItemId().equals(orderItem2.getOrderItemId()))
        );

        assertEquals(sellerOrder.getOrderId(), returnedSellerOrder.getOrderId());
        assertEquals(sellerOrder.getOrderStatus(), returnedSellerOrder.getOrderStatus());

        OrderItemResponseDTO responseItem = returnedSellerOrder.getOrderItems().getFirst();
        assertEquals(orderItem1.getOrderItemId(), responseItem.getOrderItemId());
        assertNotEquals(orderItem2.getOrderItemId(), responseItem.getOrderItemId());

        assertEquals(orderItem1.getProduct().getProductId(), responseItem.getProductId());
        assertEquals(orderItem1.getQuantity(), responseItem.getQuantityOrdered());
        assertEquals(orderItem1.getOrderedProductPrice(), responseItem.getOrderedProductPrice());
        assertEquals(orderItem1.getDiscount(), responseItem.getDiscount());

        assertEquals(product1.getProductName(), responseItem.getProductName());
        assertEquals(product1.getDescription(), responseItem.getDescription());
        assertEquals("http://localhost/images/" + product1.getImage(), responseItem.getImage());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findOrderBySellerId(eq(seller1.getUserId()), pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();

        assertEquals(0, captured.getPageNumber());
        assertEquals(10, captured.getPageSize());

        Sort.Order sortOrder = captured.getSort().iterator().next();
        assertEquals("orderId", sortOrder.getProperty());
        assertEquals(Sort.Direction.ASC, sortOrder.getDirection());

        verify(paginationValidator).validate(eq(0), eq(10), eq("orderId"), eq("asc"), anyList());
        verify(authUtil).loggedInUser();
        verify(modelMapper).map(sellerOrder, SellerOrderDTO.class);
        verify(modelMapper).map(orderItem1, OrderItemResponseDTO.class);
        verify(modelMapper, never()).map(orderItem2, OrderItemResponseDTO.class);
        verify(imageUrlUtil).constructImageUrl(orderItem1.getProduct().getImage());
        verify(imageUrlUtil, never()).constructImageUrl(orderItem2.getProduct().getImage());
    }

    /// HELPERS
    private Category createCategory() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Books");
        return category;
    }

    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("Test User");
        user.setEmail("user@gmail.com");
        user.setPassword("password");
        return user;
    }

    private User createSeller(Long id, String name) {
        User seller = new User();
        seller.setUserId(id);
        seller.setUserName(name);
        seller.setEmail(name.toLowerCase().replaceAll("\\s", "") + "@gmail.com");
        seller.setPassword("password");
        return seller;
    }

    private Product createProduct(User owner, Category category) {
        Product product = new Product();

        product.setProductName("Harry Potter 3");
        product.setImage("default.png");
        product.setDescription("Harry Potter and the Prisoner of Azkaban");
        product.setQuantity(10);
        product.setPrice(new BigDecimal("100"));
        product.setDiscount(new BigDecimal("10"));
        product.setSpecialPrice(new BigDecimal("90.00"));
        product.setCategory(category);
        product.setUser(owner);

        return product;
    }

    private Cart createCart(User user) {
        Cart cart = new Cart();
        cart.setCartId(1L);
        cart.setUser(user);
        return cart;
    }

    private CartItem createCartItem(Product product) {
        CartItem cartItem = new CartItem();

        cartItem.setCartItemId(1L);
        cartItem.setProduct(product);
        cartItem.setQuantity(3);
        cartItem.setDiscount(product.getDiscount());
        cartItem.setProductPrice(product.getSpecialPrice());

        return cartItem;
    }

    private Address createAddress(User user) {
        Address address = new Address();

        address.setAddressId(1L);
        address.setStreet("123 Maple Street, Apt 4B");
        address.setBuildingName("Oakwood Commons");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setPincode("62704");
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

        order.setOrderId(1L);
        order.setEmail(user.getEmail());
        order.setPayment(payment);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setAddress(address);

        return order;
    }

    private OrderItem createOrderItem(Product product) {
        OrderItem orderItem = new OrderItem();

        orderItem.setOrderItemId(1L);
        orderItem.setProduct(product);
        orderItem.setQuantity(3);
        orderItem.setDiscount(product.getDiscount());
        orderItem.setOrderedProductPrice(product.getSpecialPrice());

        return orderItem;
    }

    private OrderItemResponseDTO createOrderItemResponseDTO(OrderItem orderItem) {
        OrderItemResponseDTO orderItemResponseDTO = new OrderItemResponseDTO();

        orderItemResponseDTO.setOrderItemId(orderItem.getOrderItemId());
        orderItemResponseDTO.setOrderedProductPrice(orderItem.getOrderedProductPrice());
        orderItemResponseDTO.setDiscount(orderItem.getDiscount());

        return orderItemResponseDTO;
    }

    private PaymentDTO createPaymentDTO(Payment payment) {
        PaymentDTO paymentDTO = new PaymentDTO();

        paymentDTO.setPaymentId(payment.getPaymentId());
        paymentDTO.setPaymentMethod(payment.getPaymentMethod());
        paymentDTO.setPgPaymentId(payment.getPgPaymentId());
        paymentDTO.setPgStatus(payment.getPgStatus());
        paymentDTO.setPgResponseMessage(payment.getPgResponseMessage());
        paymentDTO.setPgName(payment.getPgName());

        return paymentDTO;
    }

    private OrderDTO createOrderDTO(Order order) {
        OrderDTO orderDTO = new OrderDTO();

        orderDTO.setOrderId(order.getOrderId());
        orderDTO.setEmail(order.getEmail());
        orderDTO.setTotalAmount(order.getTotalAmount());
        orderDTO.setOrderStatus(order.getOrderStatus());

        return orderDTO;
    }
}
