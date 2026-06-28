package com.ecommerce.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${spring.mail.username}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Welcome to Ecommerce Platform!");
        message.setText("Hi " + name + ",\n\nYour account has been created successfully. Welcome aboard!\n\nHappy shopping!");
        mailSender.send(message);
        log.info("[SMTP] Welcome email sent to: {}", toEmail);
    }

    @Override
    public void sendOrderConfirmedEmail(Long orderId, String userId, BigDecimal totalAmount) {
        // In production, userId would be used to look up the user's email via UserService.
        // For demo, notification is sent to the configured sender email.
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(senderEmail);
        message.setSubject("Order Confirmed — #" + orderId);
        message.setText("Your order #" + orderId + " has been confirmed.\n\nTotal: ₹" + totalAmount + "\n\nThank you for shopping with us!");
        mailSender.send(message);
        log.info("[SMTP] Order confirmation email sent for orderId: {}, userId: {}", orderId, userId);
    }
}
