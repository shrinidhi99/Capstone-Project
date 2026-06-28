package com.ecommerce.notificationservice.service;

import java.math.BigDecimal;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String name);
    void sendOrderConfirmedEmail(Long orderId, String userId, BigDecimal totalAmount);
}
