package com.cartpulse.controller;

import com.cartpulse.dto.AuthDtos.LoginRequest;
import com.cartpulse.dto.AuthDtos.RegisterRequest;
import com.cartpulse.dto.AuthDtos.UserResponse;
import com.cartpulse.security.CustomUserDetails;
import com.cartpulse.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes
    ) {
        UserResponse created = authService.register(request);
        authenticateSession(request.email(), request.password(), httpReq, httpRes);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes
    ) {
        CustomUserDetails principal = authenticateSession(request.email(), request.password(), httpReq, httpRes);
        return ResponseEntity.ok(UserResponse.from(principal.getUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
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

    private CustomUserDetails authenticateSession(
            String email,
            String password,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes
    ) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email.trim().toLowerCase(), password)
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpReq, httpRes);
        return (CustomUserDetails) auth.getPrincipal();
    }
}
