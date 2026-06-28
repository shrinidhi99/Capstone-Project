package com.ecommerce.cartservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private String userId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private LocalDateTime updatedAt;
}
