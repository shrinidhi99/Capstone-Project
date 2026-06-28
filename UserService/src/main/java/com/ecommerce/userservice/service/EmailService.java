package com.ecommerce.userservice.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String token);
}
