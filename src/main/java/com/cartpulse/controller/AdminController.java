package com.cartpulse.controller;

import com.cartpulse.dto.AdminDtos.*;
import com.cartpulse.dto.AuthDtos.UserResponse;
import com.cartpulse.security.CustomUserDetails;
import com.cartpulse.service.AdminService;
import com.cartpulse.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AnalyticsService analyticsService;

    public AdminController(AdminService adminService, AnalyticsService analyticsService) {
        this.adminService = adminService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public AdminAnalyticsResponse getAnalytics() {
        return analyticsService.getAdminAnalytics();
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCategory(request));
    }

    @GetMapping("/promos")
    public List<PromoResponse> listPromos() {
        return adminService.listPromos();
    }

    @PostMapping("/promos")
    public ResponseEntity<PromoResponse> createPromo(@Valid @RequestBody PromoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createPromo(request));
    }

    @PatchMapping("/promos/{id}/toggle")
    public PromoResponse togglePromo(@PathVariable Long id) {
        return adminService.togglePromo(id);
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return adminService.listUsers();
    }

    @PatchMapping("/users/{id}/toggle")
    public UserResponse toggleUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return adminService.toggleUserActive(id, principal.getId());
    }
}
