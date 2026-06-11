package com.ecommerce.project.service;

import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.PaymentStatus;
import com.ecommerce.project.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment createSuccessfulPayment(Order order, PaymentMethod paymentMethod) {
        Payment payment = new Payment(
                paymentMethod,
                "MOCK",
                PaymentStatus.SUCCESS,
                "Payment Successful",
                "Stripe");

        payment.setOrder(order);
        return paymentRepository.save(payment);
    }
}
