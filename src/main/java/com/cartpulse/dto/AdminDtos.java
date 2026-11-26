package com.cartpulse.dto;

import com.cartpulse.model.Category;
import com.cartpulse.model.DiscountType;
import com.cartpulse.model.PromoCode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {}

    public record CategoryRequest(
            @NotBlank(message = "Category name is required")
            @Size(min = 2, max = 80, message = "Category name must be 2 to 80 characters")
            String name,
            String description
    ) {}

    public record CategoryResponse(Long id, String name, String slug, String description) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription());
        }
    }

    public record PromoRequest(
            @NotBlank(message = "Promo code is required")
            @Size(min = 4, max = 40, message = "Promo code must be 4 to 40 characters")
            String code,

            @NotNull(message = "Discount type is required")
            DiscountType discountType,

            @NotNull(message = "Discount value is required")
            @DecimalMin(value = "0.01", message = "Discount value must be positive")
            BigDecimal discountValue,

            @NotNull
            @DecimalMin(value = "0.00")
            BigDecimal minOrderAmount,

            @Min(1)
            int maxUses,

            @NotNull(message = "Expiration date is required")
            LocalDate expiresAt
    ) {}

    public record PromoResponse(
            Long id,
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            int maxUses,
            int usedCount,
            LocalDate expiresAt,
            boolean active
    ) {
        public static PromoResponse from(PromoCode p) {
            return new PromoResponse(
                    p.getId(),
                    p.getCode(),
                    p.getDiscountType(),
                    p.getDiscountValue(),
                    p.getMinOrderAmount(),
                    p.getMaxUses(),
                    p.getUsedCount(),
                    p.getExpiresAt(),
                    p.isActive()
            );
        }
    }

    public record ChartSeries(List<String> labels, List<BigDecimal> values) {}

    public record SellerAnalyticsResponse(
            BigDecimal grossRevenue,
            long totalOrders,
            long activeProducts,
            long lowStockCount,
            ChartSeries dailyRevenue,
            Map<String, BigDecimal> salesByCategory
    ) {}

    public record AdminAnalyticsResponse(
            BigDecimal platformGmv,
            long totalOrders,
            long activePromos,
            long totalUsers,
            ChartSeries monthlyVolume,
            Map<String, Long> ordersByStatus
    ) {}
}
