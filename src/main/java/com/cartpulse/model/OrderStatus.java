package com.cartpulse.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle state machine.
 * Enforces valid forward fulfillment transitions and safe pre-shipment cancellation.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PACKED || next == CANCELLED;
            case PACKED -> next == SHIPPED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED;
    }

    public Set<OrderStatus> allowedSellerNextStatuses() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED);
            case CONFIRMED -> EnumSet.of(PACKED);
            case PACKED -> EnumSet.of(SHIPPED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}
