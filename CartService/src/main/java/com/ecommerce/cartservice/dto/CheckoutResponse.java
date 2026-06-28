package com.ecommerce.cartservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CheckoutResponse {
    private String userId;
    private String deliveryAddress;
    private BigDecimal totalAmount;
    private int itemCount;
    private String status;
    private LocalDateTime checkedOutAt;
}
