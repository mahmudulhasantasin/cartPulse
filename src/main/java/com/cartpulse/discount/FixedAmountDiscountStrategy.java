package com.cartpulse.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FixedAmountDiscountStrategy implements DiscountStrategy {

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal discountValue) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = discountValue.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return normalized.min(subtotal);
    }
}
