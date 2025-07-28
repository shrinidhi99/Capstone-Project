package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.UserLoginRequestDTO;
import com.ecommerce.userservice.dto.UserRegisterRequestDTO;
import com.ecommerce.userservice.dto.UserResponseDTO;
import com.ecommerce.userservice.exception.InvalidCredentialsException;
import com.ecommerce.userservice.exception.UserAlreadyExistsException;
import com.ecommerce.userservice.exception.UserNotFoundException;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Override
    public long getTotalRegisteredUsers() {
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
                .role("USER") // Default role
                .build();
        user = userRepository.save(user);
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
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
                .role(user.getRole())
                .build();
    }
}
