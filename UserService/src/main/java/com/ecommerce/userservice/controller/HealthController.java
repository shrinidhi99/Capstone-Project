package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    private final UserService userService;

    public HealthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/app")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("User Management Service is up and running!");
    }

    @GetMapping("/db")
    public ResponseEntity<String> dbHealthCheck() {
        Long userCount = userService.getTotalRegisteredUsers();
        return ResponseEntity.ok("Database connection is healthy! Found " + userCount + " registered users.");
    }
}
