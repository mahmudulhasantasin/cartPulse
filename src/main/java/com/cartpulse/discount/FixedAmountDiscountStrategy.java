package com.cartpulse.discount;

import java.math.BigDecimal;

public class FixedAmountDiscountStrategy extends AbstractDiscountStrategy {

    @Override
    protected BigDecimal computeDiscount(BigDecimal subtotal, BigDecimal discountValue) {
        return discountValue;
    }
}
