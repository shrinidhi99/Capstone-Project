package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody UserRegisterRequestDTO registerRequestDTO) {
        return new ResponseEntity<>(userService.registerUser(registerRequestDTO), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> loginUser(@Valid @RequestBody UserLoginRequestDTO userLoginRequestDTO) {
        return ResponseEntity.ok(userService.loginAndGetToken(userLoginRequestDTO));
    }

    @GetMapping("/details")
    public ResponseEntity<UserProfileDTO> getUserProfileDetailsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserProfileByEmail(email));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<UpdateUserDTO> updateUser(@RequestBody UpdateUserDTO updateUserDTO) {
        return ResponseEntity.ok(userService.updateUser(updateUserDTO));
    }

    @PutMapping("/update-role")
    public ResponseEntity<UpdateRoleDTO> updateRole(@RequestBody UpdateRoleDTO updateRoleDTO) {
        return ResponseEntity.ok(userService.updateRole(updateRoleDTO));
    }

    @DeleteMapping
    public ResponseEntity<Long> deleteUser(@RequestParam String email) {
        return ResponseEntity.ok(userService.deleteUser(email));
    }
}
