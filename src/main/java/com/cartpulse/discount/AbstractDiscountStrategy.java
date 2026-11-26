package com.cartpulse.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Abstract base class demonstrating the Template Method pattern,
 * shared protected helpers, and inheritance for discount strategies.
 */
public abstract class AbstractDiscountStrategy implements DiscountStrategy {

    @Override
    public final BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal discountValue) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0
                || discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rawDiscount = computeDiscount(subtotal, discountValue);
        return clampToSubtotal(rawDiscount, subtotal);
    }

    protected abstract BigDecimal computeDiscount(BigDecimal subtotal, BigDecimal discountValue);

    protected BigDecimal clampToSubtotal(BigDecimal discount, BigDecimal subtotal) {
        return discount.max(BigDecimal.ZERO)
                .min(subtotal)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
