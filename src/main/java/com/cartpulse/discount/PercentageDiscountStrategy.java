package com.cartpulse.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PercentageDiscountStrategy extends AbstractDiscountStrategy {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    protected BigDecimal computeDiscount(BigDecimal subtotal, BigDecimal discountValue) {
        BigDecimal clampedPercent = discountValue.min(ONE_HUNDRED);
        return subtotal.multiply(clampedPercent)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
