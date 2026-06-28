package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.exception.*;
import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.model.User;
import java.time.LocalDateTime;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock ObjectMapper objectMapper;
    @Mock EmailService emailService;
    @Mock PasswordResetTokenStore tokenStore;

    @InjectMocks UserServiceImpl userService;

    // ── registerUser ──────────────────────────────────────────────

    @Test
    void registerUser_success() throws Exception {
        UserRegisterRequestDTO request = new UserRegisterRequestDTO();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        User saved = User.builder().id(1L).name("Alice").email("alice@example.com").role(Role.USER).build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        UserResponseDTO response = userService.registerUser(request);

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user.registered"), eq("alice@example.com"), anyString());
    }

    @Test
    void registerUser_emailAlreadyExists() {
        UserRegisterRequestDTO request = new UserRegisterRequestDTO();
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(User.builder().build()));

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any());
    }

    // ── loginAndGetToken ──────────────────────────────────────────

    @Test
    void loginAndGetToken_success() {
        UserLoginRequestDTO request = new UserLoginRequestDTO();
        request.setEmail("alice@example.com");
        request.setPassword("secret");

        User user = User.builder().email("alice@example.com").passwordHash("hashed").role(Role.USER).build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("alice@example.com", "USER")).thenReturn("jwt-token");

        JwtResponse response = userService.loginAndGetToken(request);

        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void loginAndGetToken_userNotFound() {
        UserLoginRequestDTO request = new UserLoginRequestDTO();
        request.setEmail("ghost@example.com");
        request.setPassword("secret");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.loginAndGetToken(request));
    }

    @Test
    void loginAndGetToken_wrongPassword() {
        UserLoginRequestDTO request = new UserLoginRequestDTO();
        request.setEmail("alice@example.com");
        request.setPassword("wrong");

        User user = User.builder().email("alice@example.com").passwordHash("hashed").role(Role.USER).build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.loginAndGetToken(request));
    }

    // ── getUserProfileByEmail ─────────────────────────────────────

    @Test
    void getUserProfileByEmail_success() {
        UserProfileDTO profile = new UserProfileDTO(1L, "Alice", Role.USER, LocalDateTime.now(), LocalDateTime.now(), "alice@example.com");
        when(userRepository.getUserDetails("alice@example.com")).thenReturn(profile);

        UserProfileDTO result = userService.getUserProfileByEmail("alice@example.com");

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getUserProfileByEmail_notFound() {
        when(userRepository.getUserDetails("ghost@example.com")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserProfileByEmail("ghost@example.com"));
    }

    // ── deleteUser ────────────────────────────────────────────────

    @Test
    void deleteUser_success() {
        User user = User.builder().id(5L).email("alice@example.com").build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        Long deletedId = userService.deleteUser("alice@example.com");

        assertThat(deletedId).isEqualTo(5L);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser("ghost@example.com"));
        verify(userRepository, never()).delete(any());
    }

    // ── forgotPassword ────────────────────────────────────────────

    @Test
    void forgotPassword_userExists_sendsEmail() {
        User user = User.builder().email("alice@example.com").build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(tokenStore.createToken("alice@example.com")).thenReturn("reset-token");

        userService.forgotPassword("alice@example.com");

        verify(tokenStore).createToken("alice@example.com");
        verify(emailService).sendPasswordResetEmail("alice@example.com", "reset-token");
    }

    @Test
    void forgotPassword_userNotFound_noEmailSent() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        userService.forgotPassword("ghost@example.com");

        verify(emailService, never()).sendPasswordResetEmail(any(), any());
    }

    // ── resetPassword ─────────────────────────────────────────────

    @Test
    void resetPassword_validToken_updatesPassword() {
        User user = User.builder().email("alice@example.com").passwordHash("old-hash").build();
        when(tokenStore.resolveEmail("valid-token")).thenReturn("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.resetPassword("valid-token", "newpass");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(tokenStore).invalidate("valid-token");
    }

    @Test
    void resetPassword_invalidToken_throwsException() {
        when(tokenStore.resolveEmail("bad-token")).thenReturn(null);

        assertThrows(InvalidResetTokenException.class,
                () -> userService.resetPassword("bad-token", "newpass"));
        verify(userRepository, never()).save(any());
    }
}
