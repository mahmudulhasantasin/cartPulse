package com.cartpulse.service;

import com.cartpulse.dto.AuthDtos.RegisterRequest;
import com.cartpulse.dto.AuthDtos.UserResponse;
import com.cartpulse.exception.BadRequestException;
import com.cartpulse.exception.ConflictException;
import com.cartpulse.exception.ResourceNotFoundException;
import com.cartpulse.model.Cart;
import com.cartpulse.model.Role;
import com.cartpulse.model.User;
import com.cartpulse.repository.CartRepository;
import com.cartpulse.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, CartRepository cartRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }

        Role assignedRole = (req.role() == Role.SELLER) ? Role.SELLER : Role.CUSTOMER;
        if (req.role() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be self-registered.");
        }

        User user = new User(
                req.fullName().trim(),
                email,
                passwordEncoder.encode(req.password()),
                assignedRole
        );
        User saved = userRepository.save(user);

        if (assignedRole == Role.CUSTOMER) {
            cartRepository.save(new Cart(saved));
        }
        return UserResponse.from(saved);
    }

    public User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
