package com.ecommerce.userservice.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PasswordResetTokenStore {

    private record TokenEntry(String email, Instant expiry) {}

    private final Map<String, TokenEntry> store = new ConcurrentHashMap<>();

    public String createToken(String email) {
        String token = UUID.randomUUID().toString();
        store.put(token, new TokenEntry(email, Instant.now().plus(15, ChronoUnit.MINUTES)));
        return token;
    }

    public String resolveEmail(String token) {
        TokenEntry entry = store.get(token);
        if (entry == null || Instant.now().isAfter(entry.expiry())) {
            store.remove(token);
            return null;
        }
        return entry.email();
    }

    public void invalidate(String token) {
        store.remove(token);
    }
}
