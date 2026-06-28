package com.ecommerce.notificationservice.service;

import java.math.BigDecimal;

public interface EmailService {

    /** Sends a welcome email to a newly registered user. */
    void sendWelcomeEmail(String toEmail, String name);

    /** Sends an order confirmation email for the given order. */
    void sendOrderConfirmedEmail(Long orderId, String userId, BigDecimal totalAmount);
}
