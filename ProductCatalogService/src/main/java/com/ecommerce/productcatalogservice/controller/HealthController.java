package com.ecommerce.productcatalogservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping("/app")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Product Catalog Service is up and running!");
    }
}
