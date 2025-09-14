package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.dto.UserProfileDTO;
import com.ecommerce.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    /*
     * Custom query to check for database connectivity from the UserService
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countUsers();

    /*
     * Get user details by email that can be displayed in profile page if needed
     */
    @Query("SELECT new com.ecommerce.userservice.dto.UserProfileDTO(" +
            "u.id, u.name, u.role, u.createdAt, u.updatedAt, u.email) " +
            "FROM User u WHERE u.email = ?1")
    UserProfileDTO getUserDetails(String email);
}
