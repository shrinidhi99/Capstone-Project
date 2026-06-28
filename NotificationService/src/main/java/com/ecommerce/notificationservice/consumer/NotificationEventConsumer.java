package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.event.OrderConfirmedEvent;
import com.ecommerce.notificationservice.event.UserRegisteredEvent;
import com.ecommerce.notificationservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(EmailService emailService, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user.registered", groupId = "notification-service-group")
    public void handleUserRegistered(String payload) {
        log.info("[Kafka] Received message on topic 'user.registered'");
        try {
            UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
            log.info("[Kafka] Deserialized UserRegisteredEvent — userId: {}, email: {}", event.getUserId(), event.getEmail());
            emailService.sendWelcomeEmail(event.getEmail(), event.getName());
        } catch (Exception e) {
            log.error("[Kafka] Failed to process user.registered event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "order.confirmed", groupId = "notification-service-group")
    public void handleOrderConfirmed(String payload) {
        log.info("[Kafka] Received message on topic 'order.confirmed'");
        try {
            OrderConfirmedEvent event = objectMapper.readValue(payload, OrderConfirmedEvent.class);
            log.info("[Kafka] Deserialized OrderConfirmedEvent — orderId: {}, userId: {}, total: {}", event.getOrderId(), event.getUserId(), event.getTotalAmount());
            emailService.sendOrderConfirmedEmail(event.getOrderId(), event.getUserId(), event.getTotalAmount());
        } catch (Exception e) {
            log.error("[Kafka] Failed to process order.confirmed event: {}", e.getMessage());
        }
    }
}
