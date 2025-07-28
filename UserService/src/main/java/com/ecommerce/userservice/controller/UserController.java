package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.UserLoginRequestDTO;
import com.ecommerce.userservice.dto.UserRegisterRequestDTO;
import com.ecommerce.userservice.dto.UserResponseDTO;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/test")
    public UserResponseDTO getTestUser() {
        return userService.getUserByEmail("test@example.com");
    }

    @PostMapping("/register")
    public UserResponseDTO registerUser(@Valid @RequestBody UserRegisterRequestDTO registerRequestDTO) {
        return userService.registerUser(registerRequestDTO);
    }

    @PostMapping("/login")
    public UserResponseDTO loginUser(@Valid @RequestBody UserLoginRequestDTO userLoginRequestDTO) {
        return userService.loginUser(userLoginRequestDTO);
    }
}
