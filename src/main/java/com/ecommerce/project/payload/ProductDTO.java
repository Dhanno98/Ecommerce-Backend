package com.ecommerce.project.payload;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long productId;

    @NotBlank
    @Size(min = 3, message = "Product name must contain at least 3 characters.")
    private String productName;

    private String image;

    @NotBlank
    @Size(min = 6, message = "Product description must contain at least 6 characters.")
    private String description;

    @Min(value = 0, message = "Product quantity cannot be negative")
    private Integer quantity;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal discount;

    private BigDecimal specialPrice;
}
