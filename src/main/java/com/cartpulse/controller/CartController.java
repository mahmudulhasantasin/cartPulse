package com.cartpulse.controller;

import com.cartpulse.dto.CartDtos.AddToCartRequest;
import com.cartpulse.dto.CartDtos.CartResponse;
import com.cartpulse.dto.CartDtos.PromoPreviewResponse;
import com.cartpulse.dto.CartDtos.UpdateCartItemRequest;
import com.cartpulse.security.CustomUserDetails;
import com.cartpulse.service.CartService;
import com.cartpulse.service.PromoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final PromoService promoService;

    public CartController(CartService cartService, PromoService promoService) {
        this.cartService = cartService;
        this.promoService = promoService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal CustomUserDetails principal) {
        return cartService.getCart(principal.getId());
    }

    @PostMapping("/items")
    public CartResponse addItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddToCartRequest request
    ) {
        return cartService.addItem(principal.getId(), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateQuantity(principal.getId(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long productId
    ) {
        return cartService.updateQuantity(principal.getId(), productId, 0);
    }

    @DeleteMapping
    public CartResponse clearCart(@AuthenticationPrincipal CustomUserDetails principal) {
        return cartService.clearCart(principal.getId());
    }

    @GetMapping("/promo-preview")
    public PromoPreviewResponse previewPromo(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam String code
    ) {
        CartResponse cart = cartService.getCart(principal.getId());
        return promoService.previewPromo(code, cart.subtotal());
    }
}
