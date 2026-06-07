package com.ecommerce.project.service;

import com.ecommerce.project.model.OrderStatus;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.payload.OrderDTO;
import com.ecommerce.project.payload.OrderResponse;
import com.ecommerce.project.payload.SellerOrderResponse;

public interface OrderService {

    OrderDTO placeOrder(String emailId, Long addressId, PaymentMethod paymentMethod);

    OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderDTO updateOrder(Long orderId, OrderStatus status);

    SellerOrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
}
