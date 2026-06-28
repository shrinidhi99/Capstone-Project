package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;

public interface UserService {

    /** Used by the health/db endpoint to confirm DB connectivity. */
    Long getTotalRegisteredUsers();

    /** Throws UserAlreadyExistsException if email is taken; publishes user.registered on success. */
    UserResponseDTO registerUser(UserRegisterRequestDTO registerRequestDTO);

    /** Returns a signed JWT; throws InvalidCredentialsException on bad password. */
    JwtResponse loginAndGetToken(UserLoginRequestDTO loginRequestDTO);

    UserProfileDTO getUserProfileByEmail(String email);

    /** Owner or admin only — other callers get 403 from the security layer. */
    UpdateUserDTO updateUser(UpdateUserDTO updateUserDTO);

    /** Admin-only. */
    UpdateRoleDTO updateRole(UpdateRoleDTO updateRoleDTO);

    Long deleteUser(String email);

    /** Silent no-op if the email isn't registered — avoids leaking which accounts exist. */
    void forgotPassword(String email);

    /** Token expires in 15 min; throws InvalidResetTokenException if unknown or expired. */
    void resetPassword(String token, String newPassword);
}
