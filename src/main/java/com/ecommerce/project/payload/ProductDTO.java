package com.ecommerce.project.payload;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Double price;
    private Double discount;
    private Double specialPrice;
}
