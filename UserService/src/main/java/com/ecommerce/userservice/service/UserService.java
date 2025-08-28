package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;

public interface UserService {
    UserResponseDTO getUserByEmail(String email);

    Long getTotalRegisteredUsers();

    UserResponseDTO registerUser(UserRegisterRequestDTO registerRequestDTO);

    UserResponseDTO loginUser(UserLoginRequestDTO userLoginRequestDTO);

    UserProfileDTO getUserProfileByEmail(String email);

    UpdateUserDTO updateUser(UpdateUserDTO updateUserDTO);

    UpdateRoleDTO updateRole(UpdateRoleDTO updateRoleDTO);

    Long deleteUser(String email);

    JwtResponse loginAndGetToken(UserLoginRequestDTO loginRequestDTO);
}
