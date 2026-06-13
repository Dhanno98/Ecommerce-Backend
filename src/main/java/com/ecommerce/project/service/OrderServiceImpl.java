package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.OutOfStockException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.OrderItem;
import com.ecommerce.project.model.OrderStatus;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderItemResponseDTO;
import com.ecommerce.project.payload.OrderResponse;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;

    private final AddressRepository addressRepository;

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final ProductRepository productRepository;

    private final PaymentService paymentService;

    private final ModelMapper modelMapper;

    private final AuthUtil authUtil;

    private final ImageUrlUtil imageUrlUtil;

    private final PaginationValidator paginationValidator;

    private final static List<String> ALLOWED_SORT_FIELDS = List.of("orderId", "email", "orderDate", "totalAmount", "orderStatus");

    @Transactional
    @Override
    public OrderDTO placeOrder(String emailId, Long addressId, PaymentMethod paymentMethod) {
        log.info("Order placement requested. email={}, addressId={}, paymentMethod={}", emailId, addressId, paymentMethod);

        // Getting User Cart
        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            log.warn("Order placement failed. Cart not found for email={}", emailId);
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        Address address = addressRepository.findByIdAndUserEmailId(addressId, emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));

        List<CartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()) {
            log.warn("Order placement failed. Cart is empty. email={}", emailId);
            throw new APIException("Cart is empty!");
        }

        // Handle Partial stock depletion: User has more items in cart than available in stock.
        Map<String, String> stockErrors = new HashMap<>();
        for (CartItem cartItem : cartItems) {
            int requested = cartItem.getQuantity();
            int available = cartItem.getProduct().getQuantity();

            if (requested > available) {
                stockErrors.put(cartItem.getProduct().getProductName(), String.format("Requested: %d, Available: %d", requested, available));
            }
        }
        if (!stockErrors.isEmpty()) {
            log.warn("Order placement failed due to stock shortage. email={}, issues={}", emailId, stockErrors);
            throw new OutOfStockException("Inventory Issues", stockErrors);
        }

        // Create a new Order with Payment info
        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setAddress(address);

        Payment payment = paymentService.createSuccessfulPayment(order, paymentMethod);
        order.setPayment(payment);
        Order savedOrder = orderRepository.save(order);
        log.info("Order created. orderId={}, email={}, amount={}",
                savedOrder.getOrderId(), emailId, savedOrder.getTotalAmount());

        // Convert CartItems into OrderItems
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }

        orderItems = orderItemRepository.saveAll(orderItems);

        // Update Product stock
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }
        cartItems.clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info("Order completed successfully. orderId={}, email={}, itemsCount={}, totalAmount={}",
                savedOrder.getOrderId(), emailId, orderItems.size(), savedOrder.getTotalAmount());

        // Send back order summary
        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

        orderDTO.setOrderItems(
                orderItems
                        .stream()
                        .map(this::mapToOrderItemResponseDTO)
                        .toList()
        );

        orderDTO.setAddressId(addressId);

        return orderDTO;
    }

    @Override
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Order> pageOrders = orderRepository.findAll(pageDetails);
        List<Order> orders = pageOrders.getContent();

        List<OrderDTO> orderDTOS = orders.stream()
                .map(order -> {
                    OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
                    orderDTO.setOrderItems(
                            order.getOrderItems()
                                    .stream()
                                    .map(this::mapToOrderItemResponseDTO)
                                    .toList());
                    return orderDTO;
                })
                .toList();

        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setContent(orderDTOS);
        orderResponse.setPageNumber(pageOrders.getNumber());
        orderResponse.setPageSize(pageOrders.getSize());
        orderResponse.setTotalElements(pageOrders.getTotalElements());
        orderResponse.setTotalPages(pageOrders.getTotalPages());
        orderResponse.setLastPage(pageOrders.isLast());

        return orderResponse;
    }

    @Transactional
    @Override
    public OrderDTO updateOrder(Long orderId, OrderStatus status) {
        log.info("Order status update requested. orderId={}, newStatus={}", orderId, status);

        Order order = orderRepository.findOrderWithDetailsByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        order.setOrderStatus(status);

        log.info("Order status updated successfully. orderId={}, status={}", orderId, status);

        OrderDTO orderDTO = modelMapper.map(order, OrderDTO.class);
        orderDTO.setOrderItems(order.getOrderItems()
                .stream()
                .map(this::mapToOrderItemResponseDTO)
                .toList());

        return orderDTO;
    }

    @Override
    public SellerOrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        paginationValidator.validate(pageNumber, pageSize, sortBy, sortOrder, ALLOWED_SORT_FIELDS);

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        User seller = authUtil.loggedInUser();

        Page<Order> pageOrders = orderRepository.findOrderBySellerId(seller.getUserId(), pageDetails);

        List<Order> sellerOrders = pageOrders.getContent();

        List<SellerOrderDTO> sellerOrderDTOS = sellerOrders.stream()
                .map(order -> {
                    SellerOrderDTO sellerOrderDTO = modelMapper.map(order, SellerOrderDTO.class);
                    List<OrderItem> filteredItems = order.getOrderItems()
                                    .stream()
                                    .filter(item ->
                                            item.getProduct()
                                                    .getUser().getUserId().equals(seller.getUserId())).toList();

                    BigDecimal sellerTotal = filteredItems.stream()
                            .map(item ->
                                    item.getOrderedProductPrice()
                                            .multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<OrderItemResponseDTO> responseDTOS = filteredItems.stream().map(this::mapToOrderItemResponseDTO).toList();

                    sellerOrderDTO.setOrderItems(responseDTOS);
                    sellerOrderDTO.setSellerAmount(sellerTotal);
                    return sellerOrderDTO;
                })
                .toList();

        SellerOrderResponse sellerOrderResponse = new SellerOrderResponse();
        sellerOrderResponse.setContent(sellerOrderDTOS);
        sellerOrderResponse.setPageNumber(pageOrders.getNumber());
        sellerOrderResponse.setPageSize(pageOrders.getSize());
        sellerOrderResponse.setTotalElements(pageOrders.getTotalElements());
        sellerOrderResponse.setTotalPages(pageOrders.getTotalPages());
        sellerOrderResponse.setLastPage(pageOrders.isLast());

        return sellerOrderResponse;
    }

    private OrderItemResponseDTO mapToOrderItemResponseDTO(OrderItem item) {
        OrderItemResponseDTO dto = modelMapper.map(item, OrderItemResponseDTO.class);
        dto.setProductId(item.getProduct().getProductId());
        dto.setProductName(item.getProduct().getProductName());
        dto.setDescription(item.getProduct().getDescription());
        dto.setImage(imageUrlUtil.constructImageUrl(item.getProduct().getImage()));
        dto.setQuantityOrdered(item.getQuantity());
        return dto;
    }
}
