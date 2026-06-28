package com.ecommerce.cartservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private String userId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private LocalDateTime placedAt;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItem {
        private Long productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
    }
}
