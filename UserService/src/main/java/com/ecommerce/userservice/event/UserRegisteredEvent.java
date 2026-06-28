package com.ecommerce.userservice.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserRegisteredEvent {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime registeredAt;
}
