package com.ecommerce.notificationservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OrderConfirmedEvent {
    private Long orderId;
    private String userId;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime confirmedAt;
}
