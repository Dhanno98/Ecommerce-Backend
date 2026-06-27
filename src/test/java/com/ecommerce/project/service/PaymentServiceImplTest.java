package com.ecommerce.project.service;

import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.Order;
import com.ecommerce.project.model.OrderStatus;
import com.ecommerce.project.model.Payment;
import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.PaymentStatus;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @InjectMocks
    PaymentServiceImpl paymentService;

    @Mock
    PaymentRepository paymentRepository;

    /// createSuccessfulPayment()
    @Test
    void createSuccessfulPaymentShouldCreateAPaymentSuccessfully() {
        User user = createUser(1L);

        Address address = createAddress();
        address.setAddressId(1L);
        address.setUser(user);

        Order order = createOrder(user, address);
        order.setTotalAmount(new BigDecimal("270"));

        PaymentMethod paymentMethod = PaymentMethod.CARD;

        Payment savedPayment = createPayment(order, paymentMethod);

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        Payment result = paymentService.createSuccessfulPayment(order, paymentMethod);

        assertNotNull(result);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment payment = captor.getValue();

        assertEquals(order, payment.getOrder());
        assertEquals(paymentMethod, payment.getPaymentMethod());
        assertEquals("MOCK", payment.getPgPaymentId());
        assertEquals(PaymentStatus.SUCCESS, payment.getPgStatus());
        assertEquals("Payment Successful", payment.getPgResponseMessage());
        assertEquals("Stripe", payment.getPgName());

        assertSame(savedPayment, result);
    }

    /// HELPERS
    private User createUser(Long userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("Test User");
        user.setEmail("user@gmail.com");
        user.setPassword("password");
        return user;
    }

    private Address createAddress() {
        Address address = new Address();

        address.setStreet("123 Maple Street, Apt 4B");
        address.setBuildingName("Oakwood Commons");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setPincode("62704");

        return address;
    }

    private Order createOrder(User user, Address address) {
        Order order = new Order();
        order.setOrderId(1L);
        order.setEmail(user.getEmail());
        order.setOrderStatus(OrderStatus.CREATED);
        order.setAddress(address);
        return order;
    }

    private Payment createPayment(Order order, PaymentMethod paymentMethod) {
        Payment payment = new Payment();

        payment.setPaymentId(1L);
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        payment.setPgPaymentId("MOCK");
        payment.setPgStatus(PaymentStatus.SUCCESS);
        payment.setPgResponseMessage("Payment Successful");
        payment.setPgName("Stripe");

        return payment;
    }

}
