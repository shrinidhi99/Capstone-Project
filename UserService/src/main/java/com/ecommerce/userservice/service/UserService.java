package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;

public interface UserService {

    /** Returns total count of registered users. */
    Long getTotalRegisteredUsers();

    /** Registers a new user and publishes a user.registered Kafka event. */
    UserResponseDTO registerUser(UserRegisterRequestDTO registerRequestDTO);

    /** Authenticates credentials and returns a signed JWT. */
    JwtResponse loginAndGetToken(UserLoginRequestDTO loginRequestDTO);

    /** Fetches a user's profile by email address. */
    UserProfileDTO getUserProfileByEmail(String email);

    /** Updates name or email; only the account owner or an admin may call this. */
    UpdateUserDTO updateUser(UpdateUserDTO updateUserDTO);

    /** Changes a user's role; restricted to admins. */
    UpdateRoleDTO updateRole(UpdateRoleDTO updateRoleDTO);

    /** Permanently deletes the user with the given email. */
    Long deleteUser(String email);

    /** Generates a 15-minute reset token and emails it to the user. */
    void forgotPassword(String email);

    /** Validates the reset token and updates the user's password. */
    void resetPassword(String token, String newPassword);
}
