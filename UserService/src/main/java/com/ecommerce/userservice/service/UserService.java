package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserResponseDTO;

public interface UserService {
    UserResponseDTO getUserByEmail(String email);
    long getTotalRegisteredUsers();
}
