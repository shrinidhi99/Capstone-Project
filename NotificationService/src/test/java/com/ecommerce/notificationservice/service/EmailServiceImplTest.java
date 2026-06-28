package com.ecommerce.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock JavaMailSender mailSender;

    EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, "sender@example.com");
    }

    @Test
    void sendWelcomeEmail_sendsToCorrectRecipient() {
        emailService.sendWelcomeEmail("alice@example.com", "Alice");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).containsExactly("alice@example.com");
        assertThat(sent.getSubject()).isEqualTo("Welcome to Ecommerce Platform!");
        assertThat(sent.getText()).contains("Alice");
    }

    @Test
    void sendOrderConfirmedEmail_sendsToSenderEmail() {
        emailService.sendOrderConfirmedEmail(42L, "user-123", new BigDecimal("599.00"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getTo()).containsExactly("sender@example.com");
        assertThat(sent.getSubject()).isEqualTo("Order Confirmed — #42");
        assertThat(sent.getText()).contains("42").contains("599.00");
    }
}
