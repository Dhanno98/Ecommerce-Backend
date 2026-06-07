package com.ecommerce.project.payload;

import com.ecommerce.project.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateDTO {

    @NotNull
    private OrderStatus status;
}
