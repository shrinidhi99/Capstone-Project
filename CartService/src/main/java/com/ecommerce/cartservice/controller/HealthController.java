package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.repository.CartRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    private final CartRepository cartRepository;

    public HealthController(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @GetMapping("/app")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Cart Service is up and running!");
    }

    @GetMapping("/db")
    public ResponseEntity<String> dbHealthCheck() {
        long cartCount = cartRepository.count();
        return ResponseEntity.ok("MongoDB connection is healthy! Found " + cartCount + " carts.");
    }
}
