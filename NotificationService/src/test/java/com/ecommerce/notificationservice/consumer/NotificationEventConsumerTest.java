package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock EmailService emailService;

    NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new NotificationEventConsumer(emailService, objectMapper);
    }

    @Test
    void handleUserRegistered_validPayload_callsSendWelcomeEmail() throws Exception {
        String payload = """
                {"userId":1,"name":"Alice","email":"alice@example.com","role":"USER","registeredAt":"2026-01-01T10:00:00"}
                """;

        consumer.handleUserRegistered(payload);

        verify(emailService).sendWelcomeEmail("alice@example.com", "Alice");
    }

    @Test
    void handleUserRegistered_invalidJson_doesNotCallEmailService() {
        consumer.handleUserRegistered("not-valid-json");

        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void handleOrderConfirmed_validPayload_callsSendOrderConfirmedEmail() throws Exception {
        String payload = """
                {"orderId":42,"userId":"user-123","status":"CONFIRMED","totalAmount":599.00,"confirmedAt":"2026-01-01T10:00:00"}
                """;

        consumer.handleOrderConfirmed(payload);

        verify(emailService).sendOrderConfirmedEmail(42L, "user-123", new BigDecimal("599.00"));
    }

    @Test
    void handleOrderConfirmed_invalidJson_doesNotCallEmailService() {
        consumer.handleOrderConfirmed("not-valid-json");

        verify(emailService, never()).sendOrderConfirmedEmail(any(), anyString(), any());
    }
}
