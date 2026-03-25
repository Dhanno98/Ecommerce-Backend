package com.ecommerce.project.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    @Schema(description = "Category ID", example = "1")
    private Long categoryId;

    @Schema(description = "Category name for category you wish to create", example = "Laptop")
    @NotBlank
    @Size(min = 5, message = "Category name must contain at least 5 characters")
    private String categoryName;
}
