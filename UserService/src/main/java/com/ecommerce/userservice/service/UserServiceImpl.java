package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.exception.*;
import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Long getTotalRegisteredUsers() {
        Long count = userRepository.countUsers();
        log.info("getTotalRegisteredUsers — count: {}", count);
        return count;
    }

    @Override
    public UserResponseDTO registerUser(UserRegisterRequestDTO request) {
        log.info("registerUser called — name: {}, email: {}", request.getName(), request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new UserAlreadyExistsException(request.getEmail());
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .role(Role.USER)
                .build();
        user = userRepository.save(user);
        log.info("[MySQL] User registered successfully — id: {}, email: {}", user.getId(), user.getEmail());

        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public JwtResponse loginAndGetToken(UserLoginRequestDTO request) {
        log.info("loginAndGetToken called — email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — user not found for email: {}", request.getEmail());
                    return new UserNotFoundException(request.getEmail());
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed — invalid credentials for email: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }

        log.info("Login successful — email: {}, role: {}", user.getEmail(), user.getRole());
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        log.info("JWT token generated for email: {}", user.getEmail());
        return new JwtResponse(token);
    }

    @Override
    public UserProfileDTO getUserProfileByEmail(String email) {
        log.info("getUserProfileByEmail called — email: {}", email);
        UserProfileDTO userProfile = userRepository.getUserDetails(email);
        if (userProfile == null) {
            log.warn("[MySQL] User profile not found for email: {}", email);
            throw new UserNotFoundException(email);
        }
        log.info("[MySQL] User profile fetched for email: {}", email);
        return userProfile;
    }

    @Override
    public UpdateUserDTO updateUser(UpdateUserDTO updateUserDTO) {
        log.info("updateUser called — targetEmail: {}, newEmail: {}, newName: {}",
                updateUserDTO.getCurrentEmail(), updateUserDTO.getNewEmail(), updateUserDTO.getName());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        log.info("Authenticated as — email: {}, role: {}", currentUserEmail, currentUserRole);

        String targetEmail = updateUserDTO.getCurrentEmail();
        User user = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> {
                    log.warn("[MySQL] User not found for email: {}", targetEmail);
                    return new UserNotFoundException(targetEmail);
                });

        if (!currentUserRole.equals("ROLE_ADMIN") && !currentUserEmail.equals(targetEmail)) {
            log.warn("Unauthorised update attempt — {} tried to update profile of {}", currentUserEmail, targetEmail);
            throw new UnauthorizedException("You cannot update another user's profile.");
        }

        String newEmail = updateUserDTO.getNewEmail();
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            if (userRepository.findByEmail(newEmail).isPresent()) {
                log.warn("Email update failed — {} is already taken", newEmail);
                throw new EmailAlreadyExistsException(newEmail);
            }
            log.info("Updating email from {} to {}", user.getEmail(), newEmail);
            user.setEmail(newEmail);
        }

        String newName = updateUserDTO.getName();
        if (newName != null && !newName.isBlank()) {
            log.info("Updating name to: {}", newName);
            user.setName(newName);
        }

        User updatedUser = userRepository.save(user);
        log.info("[MySQL] User updated successfully — email: {}", updatedUser.getEmail());
        return new UpdateUserDTO(updatedUser.getEmail(), null, updatedUser.getName());
    }

    @Override
    public UpdateRoleDTO updateRole(UpdateRoleDTO updateRoleDTO) {
        log.info("updateRole called — targetEmail: {}, newRole: {}", updateRoleDTO.getEmail(), updateRoleDTO.getRole());

        String targetEmail = updateRoleDTO.getEmail();
        String roleToUpdate = updateRoleDTO.getRole();

        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("Email must be provided for role update.");
        }
        if (roleToUpdate == null || roleToUpdate.isBlank()) {
            throw new IllegalArgumentException("Role must be provided.");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        log.info("Role update requested by user with role: {}", currentUserRole);

        if (!"ROLE_ADMIN".equals(currentUserRole)) {
            log.warn("Unauthorised role update attempt — requester role: {}", currentUserRole);
            throw new UnauthorizedException("Only admins can update roles.");
        }

        User user = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> {
                    log.warn("[MySQL] User not found for email: {}", targetEmail);
                    return new UserNotFoundException(targetEmail);
                });

        Role newRole;
        try {
            newRole = Role.valueOf(roleToUpdate.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role value provided: {}", roleToUpdate);
            throw new IllegalArgumentException("Invalid role: " + roleToUpdate);
        }

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        log.info("[MySQL] Role updated for email: {} — new role: {}", updatedUser.getEmail(), updatedUser.getRole());
        return new UpdateRoleDTO(updatedUser.getEmail(), updatedUser.getRole().name());
    }

    @Override
    public Long deleteUser(String email) {
        log.info("deleteUser called — email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[MySQL] User not found for email: {}", email);
                    return new UserNotFoundException(email);
                });

        Long deletedUserId = user.getId();
        userRepository.delete(user);
        log.info("[MySQL] User deleted — id: {}, email: {}", deletedUserId, email);
        return deletedUserId;
    }
}
