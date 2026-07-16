package com.ecommerce.project.service;

import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;

public interface PaymentService {
    Payment createSuccessfulPayment(Order order, PaymentMethod paymentMethod, String paymentIntentId);
}
