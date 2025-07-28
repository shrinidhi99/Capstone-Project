package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserLoginRequestDTO;
import com.ecommerce.userservice.dto.UserRegisterRequestDTO;
import com.ecommerce.userservice.dto.UserResponseDTO;

public interface UserService {
    UserResponseDTO getUserByEmail(String email);

    long getTotalRegisteredUsers();

    UserResponseDTO registerUser(UserRegisterRequestDTO registerRequestDTO);

    UserResponseDTO loginUser(UserLoginRequestDTO userLoginRequestDTO);
}
