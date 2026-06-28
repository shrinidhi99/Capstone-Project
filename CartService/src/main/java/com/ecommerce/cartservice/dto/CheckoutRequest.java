package com.ecommerce.cartservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckoutRequest {
    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;
}
