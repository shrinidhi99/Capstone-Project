package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.UserResponseDTO;
import com.ecommerce.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public UserResponseDTO getTestUser() {
        return userService.getUserByEmail("test@example.com");
    }
}
