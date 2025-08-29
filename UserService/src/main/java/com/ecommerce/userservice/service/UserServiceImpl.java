package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.exception.*;
import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Long getTotalRegisteredUsers() {
        return userRepository.countUsers();
    }

    @Override
    public UserResponseDTO registerUser(UserRegisterRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException(request.getEmail());
        }
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .role(Role.USER) // Default role
                .build();
        user = userRepository.save(user);
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public UserResponseDTO loginUser(UserLoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));
        boolean passwordMatch = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!passwordMatch) {
            throw new InvalidCredentialsException();
        }

        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public JwtResponse loginAndGetToken(UserLoginRequestDTO loginRequestDTO) {
        UserResponseDTO userResponse = loginUser(loginRequestDTO);

        // Generate token using the email + role
        String token = jwtUtil.generateToken(userResponse.getEmail(), userResponse.getRole());

        return new JwtResponse(token);
    }


    @Override
    public UserProfileDTO getUserProfileByEmail(String email) {
        Optional<UserProfileDTO> userProfileOpt = Optional.ofNullable(userRepository.getUserDetails(email));
        return userProfileOpt.orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public UpdateUserDTO updateUser(UpdateUserDTO updateUserDTO) {
        // Get current user from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName(); // email from JWT
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority(); // ROLE_USER / ROLE_ADMIN

        String targetEmail = updateUserDTO.getCurrentEmail();
        String newEmail = updateUserDTO.getNewEmail();

        User user = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new UserNotFoundException(targetEmail));

        // Authorization check
        if (!currentUserRole.equals("ROLE_ADMIN") && !currentUserEmail.equals(targetEmail)) {
            throw new UnauthorizedException("You cannot update another user's profile.");
        }

        // Email change logic
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                // to avoid taking over an existing email
                throw new EmailAlreadyExistsException(newEmail);
            }
            user.setEmail(newEmail);
        }

        // Update name if provided
        String newName = updateUserDTO.getName();
        if (newName != null && !newName.isBlank()) {
            user.setName(newName);
        }

        // Save updated user
        User updatedUser = userRepository.save(user);

        // Map back to DTO (sending updated values)
        return new UpdateUserDTO(
                updatedUser.getEmail(),   // currentEmail (now effectively the "active" email)
                null,                     // newEmail is not relevant in response
                updatedUser.getName()
        );
    }


    @Override
    public UpdateRoleDTO updateRole(UpdateRoleDTO updateRoleDTO) {
        String targetEmail = updateRoleDTO.getEmail();
        String roleToUpdate = updateRoleDTO.getRole();

        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("Email must be provided for role update.");
        }
        if (roleToUpdate == null || roleToUpdate.isBlank()) {
            throw new IllegalArgumentException("Role must be provided.");
        }

        // Get current user from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority(); // ROLE_USER / ROLE_ADMIN

        // Only ADMIN can update roles
        if (!"ROLE_ADMIN".equals(currentUserRole)) {
            throw new UnauthorizedException("Only admins can update roles.");
        }

        // Fetch the user to update
        User user = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new UserNotFoundException(targetEmail));

        // Validate role value
        Role newRole;
        try {
            newRole = Role.valueOf(roleToUpdate.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleToUpdate);
        }

        // Update role and save
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        // Map back to DTO
        return new UpdateRoleDTO(
                updatedUser.getEmail(),
                updatedUser.getRole().name()
        );
    }


    @Override
    public Long deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Store the id before deletion
        Long deletedUserId = user.getId();

        // Delete the user
        userRepository.delete(user);

        // Return the deleted user's id
        return deletedUserId;
    }


}
