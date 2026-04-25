package com.ecommerce.project.service;

import com.ecommerce.project.payload.StripePaymentDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeSearchResult;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Autowired
    private AddressRepository addressRepository;

    @Override
    public PaymentIntent paymentIntent(StripePaymentDTO stripePaymentDTO) throws StripeException {

        /* search() method NOT AVAILABLE IN INDIA!
        Customer customer;
        // Retrieve and check if the customer exist
        StripeClient client = new StripeClient(stripeApiKey);
        CustomerSearchParams searchParams =
                CustomerSearchParams.builder()
                        .setQuery("email:'" + stripePaymentDTO.getEmail() + "'")
                        .build();
        //CustomerSearchResult customers = Customer.search(searchParams);
        StripeSearchResult<Customer> stripeSearchResult = client.v1().customers().search(searchParams);

        if (stripeSearchResult.getData().isEmpty()) {
            // Create new customer
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                            .setName(stripePaymentDTO.getName())
                            .setEmail(stripePaymentDTO.getEmail())
                            .setAddress(
                                CustomerCreateParams.Address.builder()
                                        .setLine1(stripePaymentDTO.getAddress().getStreet())
                                        .setCity(stripePaymentDTO.getAddress().getCity())
                                        .setState(stripePaymentDTO.getAddress().getState())
                                        .setPostalCode(stripePaymentDTO.getAddress().getPincode())
                                        .setCountry(stripePaymentDTO.getAddress().getCountry())
                                        .build()
                            )
                            .build();
            customer = Customer.create(customerParams);
        } else {
            // Fetch the customer that exist
            customer = stripeSearchResult.getData().getFirst();
        }
        */

        CustomerCreateParams customerParams = CustomerCreateParams.builder()
                .setName(stripePaymentDTO.getName())
                .setEmail(stripePaymentDTO.getEmail())
                .setAddress(
                        CustomerCreateParams.Address.builder()
                                .setLine1(stripePaymentDTO.getAddress().getStreet())
                                .setCity(stripePaymentDTO.getAddress().getCity())
                                .setState(stripePaymentDTO.getAddress().getState())
                                .setPostalCode(stripePaymentDTO.getAddress().getPincode())
                                .setCountry(stripePaymentDTO.getAddress().getCountry().equalsIgnoreCase("India") ? "IN" :
                                            stripePaymentDTO.getAddress().getCountry().equalsIgnoreCase("USA") ? "US" :
                                            stripePaymentDTO.getAddress().getCountry())
                                .build()
                )
                .build();
        Customer customer = Customer.create(customerParams);

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(stripePaymentDTO.getAmount())
                        .setCurrency(stripePaymentDTO.getCurrency())
                        .setCustomer(customer.getId())
                        .setDescription(stripePaymentDTO.getDescription())
                        .setShipping(
                                PaymentIntentCreateParams.Shipping.builder()
                                        .setName(customer.getName())
                                        .setAddress(
                                                PaymentIntentCreateParams.Shipping.Address.builder()
                                                        .setLine1(customer.getAddress().getLine1())
                                                        .setCity(customer.getAddress().getCity())
                                                        .setState(customer.getAddress().getState())
                                                        .setCountry(customer.getAddress().getCountry())
                                                        .setPostalCode(customer.getAddress().getPostalCode())
                                                        .build()
                                        )
                                        .build()
                        )
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

        return PaymentIntent.create(params);
    }
}
