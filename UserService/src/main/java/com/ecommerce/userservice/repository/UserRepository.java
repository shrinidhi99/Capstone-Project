package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.dto.UserProfileDTO;
import com.ecommerce.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    // used by the health check endpoint
    @Query("SELECT COUNT(u) FROM User u")
    long countUsers();

    @Query("SELECT new com.ecommerce.userservice.dto.UserProfileDTO(" +
            "u.id, u.name, u.role, u.createdAt, u.updatedAt, u.email) " +
            "FROM User u WHERE u.email = ?1")
    UserProfileDTO getUserDetails(String email);
}
