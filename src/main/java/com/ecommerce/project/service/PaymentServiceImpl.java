package com.ecommerce.project.service;

import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.PaymentStatus;
import com.ecommerce.project.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment createSuccessfulPayment(Order order, PaymentMethod paymentMethod) {

        log.info("Creating payment. orderId={}, paymentMethod={}", order.getOrderId(), paymentMethod);

        Payment payment = new Payment(
                paymentMethod,
                "MOCK",
                PaymentStatus.SUCCESS,
                "Payment Successful",
                "Stripe");

        payment.setOrder(order);

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment created successfully. paymentId={}, orderId={}, status={}",
                savedPayment.getPaymentId(), order.getOrderId(), savedPayment.getPgStatus());
        return savedPayment;
    }
}
