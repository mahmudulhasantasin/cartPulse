package com.cartpulse.discount;

import com.cartpulse.model.DiscountType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class DiscountStrategyFactory {

    private final Map<DiscountType, DiscountStrategy> strategies = new EnumMap<>(DiscountType.class);

    public DiscountStrategyFactory() {
        strategies.put(DiscountType.PERCENT, new PercentageDiscountStrategy());
        strategies.put(DiscountType.FIXED, new FixedAmountDiscountStrategy());
    }

    public DiscountStrategy getStrategy(DiscountType type) {
        DiscountStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported discount type: " + type);
        }
        return strategy;
    }
}
