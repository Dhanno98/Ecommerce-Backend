package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDTO {
    private Long productId;

    private String productName;

    private String image;

    private String description;

    private Integer quantity; // quantity in cart

    private BigDecimal price;

    private BigDecimal discount;

    private BigDecimal specialPrice;
}
