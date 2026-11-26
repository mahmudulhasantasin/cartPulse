package com.cartpulse.controller;

import com.cartpulse.dto.OrderDtos.CheckoutRequest;
import com.cartpulse.dto.OrderDtos.OrderResponse;
import com.cartpulse.security.CustomUserDetails;
import com.cartpulse.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> myOrders(@AuthenticationPrincipal CustomUserDetails principal) {
        return orderService.getCustomerOrders(principal.getId());
    }

    @PostMapping("/checkout")
    public ResponseEntity<List<OrderResponse>> checkout(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CheckoutRequest request
    ) {
        List<OrderResponse> orders = orderService.checkout(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orders);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long orderId
    ) {
        return orderService.cancelOrder(orderId, principal.getId());
    }
}
