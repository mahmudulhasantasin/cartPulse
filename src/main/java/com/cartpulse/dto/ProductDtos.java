package com.cartpulse.dto;

import com.cartpulse.model.InventoryLog;
import com.cartpulse.model.InventoryReason;
import com.cartpulse.model.Product;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ProductDtos {

    private ProductDtos() {}

    public record ProductRequest(
            @NotBlank(message = "Product name is required")
            @Size(min = 3, max = 160, message = "Product name must be 3 to 160 characters")
            String name,

            @NotNull(message = "Category is required")
            Long categoryId,

            @NotBlank(message = "Description is required")
            @Size(min = 10, max = 4000, message = "Description must be at least 10 characters")
            String description,

            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.01", message = "Price must be greater than zero")
            BigDecimal price,

            @NotNull(message = "Stock quantity is required")
            @Min(value = 0, message = "Stock quantity cannot be negative")
            Integer stockQuantity,

            @Min(value = 1, message = "Low-stock threshold must be at least 1")
            Integer lowStockThreshold,

            @Size(max = 500, message = "Image URL is too long")
            String imageUrl
    ) {}

    public record RestockRequest(
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Restock amount must be at least 1")
            Integer amount
    ) {}

    public record ProductResponse(
            Long id,
            String name,
            String description,
            BigDecimal price,
            int stockQuantity,
            int lowStockThreshold,
            String imageUrl,
            BigDecimal avgRating,
            int reviewCount,
            boolean active,
            Long categoryId,
            String categoryName,
            Long sellerId,
            String sellerName,
            LocalDateTime createdAt
    ) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(
                    p.getId(),
                    p.getName(),
                    p.getDescription(),
                    p.getPrice(),
                    p.getStockQuantity(),
                    p.getLowStockThreshold(),
                    p.getImageUrl(),
                    p.getAvgRating(),
                    p.getReviewCount(),
                    p.isActive(),
                    p.getCategory().getId(),
                    p.getCategory().getName(),
                    p.getSeller().getId(),
                    p.getSeller().getFullName(),
                    p.getCreatedAt()
            );
        }
    }

    public record InventoryLogResponse(
            Long id,
            int changeAmount,
            InventoryReason reason,
            Long referenceId,
            LocalDateTime createdAt
    ) {
        public static InventoryLogResponse from(InventoryLog log) {
            return new InventoryLogResponse(
                    log.getId(),
                    log.getChangeAmount(),
                    log.getReason(),
                    log.getReferenceId(),
                    log.getCreatedAt()
            );
        }
    }
}
