package com.ecommerce.project.payload;

import com.ecommerce.project.model.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {
    private Long addressId;
    private PaymentMethod paymentMethod;
}
