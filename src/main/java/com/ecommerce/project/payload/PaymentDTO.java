package com.ecommerce.project.payload;

import com.ecommerce.project.model.PaymentMethod;
import com.ecommerce.project.model.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private Long paymentId;

    @NotBlank
    @Size(min = 4, message = "Payment method must contain at least 4 characters.")
    private PaymentMethod paymentMethod;

    private String pgPaymentId;
    private PaymentStatus pgStatus;
    private String pgResponseMessage;
    private String pgName;
}
