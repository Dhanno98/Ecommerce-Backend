package com.ecommerce.project.controller;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.service.OrderService;
import com.ecommerce.project.service.StripeService;
import com.ecommerce.project.util.AuthUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    private final AuthUtil authUtil;

    private final StripeService stripeService;

    @PostMapping("/order/users")
    public ResponseEntity<OrderDTO> orderProducts(@RequestBody OrderRequestDTO orderRequestDTO) {
        String emailId = authUtil.loggedInEmail();
        OrderDTO orderDTO = orderService.placeOrder(emailId, orderRequestDTO.getAddressId(), orderRequestDTO.getPaymentMethod());
        return new ResponseEntity<>(orderDTO, HttpStatus.CREATED);
    }

    @PostMapping("/order/stripe-client-secret")
    public ResponseEntity<String> createStripeClientSecret(@RequestBody StripePaymentDTO stripePaymentDTO) throws StripeException {
        System.out.println("StripePaymentDTO Received " + stripePaymentDTO);
        PaymentIntent paymentIntent = stripeService.paymentIntent(stripePaymentDTO);
        return new ResponseEntity<>(paymentIntent.getClientSecret(), HttpStatus.CREATED);
    }

    @GetMapping("/admin/orders")
    public ResponseEntity<OrderResponse> getAllOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        OrderResponse orderResponse = orderService.getAllOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @GetMapping("/seller/orders")
    public ResponseEntity<SellerOrderResponse> getAllSellerOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        SellerOrderResponse sellerOrderResponse = orderService.getAllSellerOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(sellerOrderResponse, HttpStatus.OK);
    }

    @PutMapping("/admin/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId,
                                                      @Valid @RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO) {
        OrderDTO orderDTO = orderService.updateOrder(orderId, orderStatusUpdateDTO.getStatus());
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }

    @PutMapping("/seller/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatusSeller(@PathVariable Long orderId,
                                                            @Valid @RequestBody OrderStatusUpdateDTO orderStatusUpdateDTO) {
        OrderDTO orderDTO = orderService.updateOrder(orderId, orderStatusUpdateDTO.getStatus());
        return new ResponseEntity<>(orderDTO, HttpStatus.OK);
    }
}
