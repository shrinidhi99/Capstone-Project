package com.ecommerce.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset — Ecommerce Platform");
        message.setText("Your password reset token:\n\n" + token
                + "\n\nThis token expires in 15 minutes."
                + "\n\nIf you did not request a password reset, ignore this email.");
        mailSender.send(message);
        log.info("[SMTP] Password reset email sent to: {}", toEmail);
    }
}
