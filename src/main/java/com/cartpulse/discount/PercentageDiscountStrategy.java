package com.cartpulse.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal discountValue) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal clampedPercent = discountValue.min(ONE_HUNDRED).max(BigDecimal.ZERO);
        BigDecimal discount = subtotal.multiply(clampedPercent)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        return discount.min(subtotal);
    }
}
