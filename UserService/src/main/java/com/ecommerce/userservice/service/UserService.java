package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;

public interface UserService {
    Long getTotalRegisteredUsers();
    UserResponseDTO registerUser(UserRegisterRequestDTO registerRequestDTO);
    JwtResponse loginAndGetToken(UserLoginRequestDTO loginRequestDTO);
    UserProfileDTO getUserProfileByEmail(String email);
    UpdateUserDTO updateUser(UpdateUserDTO updateUserDTO);
    UpdateRoleDTO updateRole(UpdateRoleDTO updateRoleDTO);
    Long deleteUser(String email);
}
