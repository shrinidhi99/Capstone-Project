package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final OrderRepository orderRepository;

    public HealthController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/app")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Order Service is up and running!");
    }

    @GetMapping("/db")
    public ResponseEntity<String> dbHealthCheck() {
        long orderCount = orderRepository.count();
        return ResponseEntity.ok("Database connection is healthy! Found " + orderCount + " orders.");
    }
}
