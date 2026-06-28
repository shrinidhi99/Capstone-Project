package com.ecommerce.productcatalogservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/app")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Product Catalog Service is up and running!");
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, String>> dbHealthCheck() {
        Map<String, String> response = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            connection.isValid(2);
            response.put("status", "UP");
            response.put("database", connection.getMetaData().getDatabaseProductName());
            response.put("url", connection.getMetaData().getURL());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
