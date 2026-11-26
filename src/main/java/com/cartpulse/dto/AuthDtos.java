package com.cartpulse.dto;

import com.cartpulse.model.Role;
import com.cartpulse.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 120, message = "Full name must be 2 to 120 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
            String password,

            Role role
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record UserResponse(
            Long id,
            String fullName,
            String email,
            Role role,
            boolean active
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive());
        }
    }

    public record AuthResponse(
            String token,
            UserResponse user
    ) {}
}
