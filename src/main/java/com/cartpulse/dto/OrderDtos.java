package com.cartpulse.dto;

import com.cartpulse.model.Order;
import com.cartpulse.model.OrderItem;
import com.cartpulse.model.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {}

    public record CheckoutRequest(
            @NotBlank(message = "Shipping address is required")
            @Size(min = 10, max = 500, message = "Please enter a complete shipping address (min 10 characters)")
            String shippingAddress,

            @NotBlank(message = "Payment method is required")
            String paymentMethod,

            String promoCode
    ) {}

    public record UpdateOrderStatusRequest(
            @NotNull(message = "Next order status is required")
            OrderStatus status
    ) {}

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProductNameSnapshot(),
                    item.getUnitPriceSnapshot(),
                    item.getQuantity(),
                    item.getLineTotal()
            );
        }
    }

    public record OrderResponse(
            Long id,
            String orderNumber,
            Long customerId,
            String customerName,
            Long sellerId,
            String sellerName,
            BigDecimal subtotalAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            OrderStatus status,
            String shippingAddress,
            String paymentMethod,
            LocalDateTime createdAt,
            List<OrderItemResponse> items
    ) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(),
                    o.getOrderNumber(),
                    o.getCustomer().getId(),
                    o.getCustomer().getFullName(),
                    o.getSeller().getId(),
                    o.getSeller().getFullName(),
                    o.getSubtotalAmount(),
                    o.getDiscountAmount(),
                    o.getTotalAmount(),
                    o.getStatus(),
                    o.getShippingAddress(),
                    o.getPaymentMethod(),
                    o.getCreatedAt(),
                    o.getItems().stream().map(OrderItemResponse::from).toList()
            );
        }
    }
}
