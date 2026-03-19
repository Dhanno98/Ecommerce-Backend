package com.ecommerce.project.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {
    private Long addressId;

    @NotBlank
    @Size(min = 4, message = "Payment method must contain at least 4 characters.")
    private String paymentMethod;

    private String pgName;
    private String pgPaymentId;
    private String pgStatus;
    private String pgResponseMessage;
}
