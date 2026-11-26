package com.cartpulse.controller;

import com.cartpulse.dto.AdminDtos.SellerAnalyticsResponse;
import com.cartpulse.dto.OrderDtos.OrderResponse;
import com.cartpulse.dto.OrderDtos.UpdateOrderStatusRequest;
import com.cartpulse.dto.ProductDtos.*;
import com.cartpulse.model.Role;
import com.cartpulse.security.CustomUserDetails;
import com.cartpulse.service.AnalyticsService;
import com.cartpulse.service.OrderService;
import com.cartpulse.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final ProductService productService;
    private final OrderService orderService;
    private final AnalyticsService analyticsService;

    public SellerController(
            ProductService productService,
            OrderService orderService,
            AnalyticsService analyticsService
    ) {
        this.productService = productService;
        this.orderService = orderService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public SellerAnalyticsResponse getAnalytics(@AuthenticationPrincipal CustomUserDetails principal) {
        return analyticsService.getSellerAnalytics(principal.getId());
    }

    @GetMapping("/products")
    public List<ProductResponse> listProducts(@AuthenticationPrincipal CustomUserDetails principal) {
        return productService.getSellerProducts(principal.getId());
    }

    @GetMapping("/products/low-stock")
    public List<ProductResponse> listLowStock(@AuthenticationPrincipal CustomUserDetails principal) {
        return productService.getLowStockProducts(principal.getId());
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(principal.getId(), request));
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.updateProduct(id, principal.getId(), isAdmin(principal), request);
    }

    @PostMapping("/products/{id}/restock")
    public ProductResponse restockProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody RestockRequest request
    ) {
        return productService.restockProduct(id, principal.getId(), isAdmin(principal), request.amount());
    }

    @PatchMapping("/products/{id}/toggle")
    public ProductResponse toggleProduct(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id
    ) {
        return productService.toggleActive(id, principal.getId(), isAdmin(principal));
    }

    @GetMapping("/products/{id}/inventory-logs")
    public List<InventoryLogResponse> inventoryLogs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id
    ) {
        return productService.getInventoryLogs(id, principal.getId(), isAdmin(principal));
    }

    @GetMapping("/orders")
    public List<OrderResponse> listOrders(@AuthenticationPrincipal CustomUserDetails principal) {
        return orderService.getSellerOrders(principal.getId());
    }

    @PatchMapping("/orders/{id}/status")
    public OrderResponse advanceStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.advanceStatus(id, principal.getId(), isAdmin(principal), request.status());
    }

    private boolean isAdmin(CustomUserDetails principal) {
        return principal.getUser().getRole() == Role.ADMIN;
    }
}
