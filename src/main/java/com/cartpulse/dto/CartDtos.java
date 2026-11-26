package com.cartpulse.dto;

import com.cartpulse.model.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {}

    public record AddToCartRequest(
            @NotNull(message = "Product ID is required")
            Long productId,

            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity
    ) {}

    public record UpdateCartItemRequest(
            @Min(value = 0, message = "Quantity cannot be negative")
            int quantity
    ) {}

    public record CartItemResponse(
            Long id,
            Long productId,
            String productName,
            String imageUrl,
            Long sellerId,
            String sellerName,
            BigDecimal unitPrice,
            int quantity,
            int availableStock,
            BigDecimal lineTotal
    ) {}

    public record CartResponse(
            List<CartItemResponse> items,
            BigDecimal subtotal,
            int totalItems
    ) {}

    public record PromoPreviewResponse(
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal discountAmount,
            BigDecimal finalTotal,
            String message
    ) {}
}
