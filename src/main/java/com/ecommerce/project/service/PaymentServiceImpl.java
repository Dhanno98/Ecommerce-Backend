package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.PaymentStatus;
import com.ecommerce.project.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public Payment createSuccessfulPayment(Order order, PaymentMethod paymentMethod, String paymentIntentId) {

        log.info("Creating payment. orderId={}, paymentMethod={}, paymentIntentId={}",
                order.getOrderId(), paymentMethod, paymentIntentId);

        Payment existingPayment = paymentRepository.findByPgPaymentId(paymentIntentId);
        if (existingPayment != null) {
            throw new APIException("This payment has already been processed");
        }

        Payment payment = new Payment(
                paymentMethod,
                paymentIntentId,
                PaymentStatus.SUCCESS,
                "Payment Successful",
                "Stripe");

        payment.setOrder(order);

        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            throw new APIException("This payment has already been processed");
        }

        log.info("Payment created successfully. paymentId={}, orderId={}, status={}",
                savedPayment.getPaymentId(), order.getOrderId(), savedPayment.getPgStatus());
        return savedPayment;
    }
}
