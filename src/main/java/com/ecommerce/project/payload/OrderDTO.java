package com.ecommerce.project.payload;

import com.ecommerce.project.model.OrderStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long orderId;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    private List<OrderItemResponseDTO> orderItems = new ArrayList<>();
    private LocalDateTime orderDate;
    private PaymentDTO payment;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private Long addressId;
}
