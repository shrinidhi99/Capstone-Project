package com.ecommerce.productcatalogservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must be 150 characters or fewer")
    private String name;

    @NotBlank(message = "Product description is required")
    @Size(max = 1000, message = "Product description must be 1000 characters or fewer")
    private String description;

    @NotBlank(message = "Product category is required")
    @Size(max = 80, message = "Product category must be 80 characters or fewer")
    private String category;

    @Size(max = 80, message = "Brand must be 80 characters or fewer")
    private String brand;

    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Product quantity is required")
    @Min(value = 0, message = "Product quantity cannot be negative")
    private Integer quantity;

    @Size(max = 500, message = "Image URL must be 500 characters or fewer")
    private String imageUrl;
}
