package com.cartpulse.discount;

import java.math.BigDecimal;

/**
 * Strategy pattern interface for calculating promotional discounts.
 */
public interface DiscountStrategy {
    BigDecimal calculateDiscount(BigDecimal subtotal, BigDecimal discountValue);
}
