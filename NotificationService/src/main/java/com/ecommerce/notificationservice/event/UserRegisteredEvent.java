package com.ecommerce.notificationservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserRegisteredEvent {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime registeredAt;
}
