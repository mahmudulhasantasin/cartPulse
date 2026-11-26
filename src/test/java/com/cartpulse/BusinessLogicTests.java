package com.cartpulse;

import com.cartpulse.discount.FixedAmountDiscountStrategy;
import com.cartpulse.discount.PercentageDiscountStrategy;
import com.cartpulse.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BusinessLogicTests {

    @Test
    void orderStateMachineEnforcesValidTransitions() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));

        assertTrue(OrderStatus.CONFIRMED.isCancellable());
        assertFalse(OrderStatus.PACKED.isCancellable());
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void discountStrategiesComputeAccurateAmounts() {
        PercentageDiscountStrategy percent = new PercentageDiscountStrategy();
        assertEquals(new BigDecimal("10.00"), percent.calculateDiscount(new BigDecimal("100.00"), new BigDecimal("10")));
        assertEquals(new BigDecimal("50.00"), percent.calculateDiscount(new BigDecimal("50.00"), new BigDecimal("150")));

        FixedAmountDiscountStrategy fixed = new FixedAmountDiscountStrategy();
        assertEquals(new BigDecimal("15.00"), fixed.calculateDiscount(new BigDecimal("100.00"), new BigDecimal("15")));
        assertEquals(new BigDecimal("12.00"), fixed.calculateDiscount(new BigDecimal("12.00"), new BigDecimal("25")));
    }
}
