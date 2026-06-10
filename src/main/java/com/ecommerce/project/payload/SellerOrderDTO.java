package com.ecommerce.project.payload;

import com.ecommerce.project.model.OrderStatus;
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
public class SellerOrderDTO {
    private Long orderId;
    private List<OrderItemResponseDTO> orderItems = new ArrayList<>();
    private LocalDateTime orderDate;
    private BigDecimal sellerAmount;
    private OrderStatus orderStatus;
}
