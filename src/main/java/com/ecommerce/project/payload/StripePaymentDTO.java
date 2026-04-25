package com.ecommerce.project.payload;

import lombok.Data;

import java.util.Map;

@Data
public class StripePaymentDTO {
    private Long amount;
    private String currency;
    private String email;
    private String name;
    private AddressDTO address;
    private String description;
    private Map<String, String> metadata;
}
