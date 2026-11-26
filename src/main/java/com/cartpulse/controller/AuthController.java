package com.cartpulse.controller;

import com.cartpulse.dto.AuthDtos.AuthResponse;
import com.cartpulse.dto.AuthDtos.LoginRequest;
import com.cartpulse.dto.AuthDtos.RegisterRequest;
import com.cartpulse.dto.AuthDtos.UserResponse;
import com.cartpulse.security.CustomUserDetails;
import com.cartpulse.security.JwtTokenProvider;
import com.cartpulse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse created = authService.register(request);
        String token = jwtTokenProvider.generateToken(created.id(), created.email(), created.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, created));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        UserResponse user = UserResponse.from(principal.getUser());
        String token = jwtTokenProvider.generateToken(user.id(), user.email(), user.role());
        return ResponseEntity.ok(new AuthResponse(token, user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Signed out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(@AuthenticationPrincipal CustomUserDetails principal) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (principal == null) {
            body.put("authenticated", false);
            body.put("user", null);
        } else {
            body.put("authenticated", true);
            body.put("user", UserResponse.from(authService.getUserOrThrow(principal.getId())));
        }
        return ResponseEntity.ok(body);
    }
}
