package com.cartpulse.service;

import com.cartpulse.dto.OrderDtos.CheckoutRequest;
import com.cartpulse.dto.OrderDtos.OrderResponse;
import com.cartpulse.exception.BadRequestException;
import com.cartpulse.exception.ConflictException;
import com.cartpulse.exception.ForbiddenActionException;
import com.cartpulse.exception.ResourceNotFoundException;
import com.cartpulse.model.*;
import com.cartpulse.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final PromoService promoService;

    public OrderService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            PromoCodeRepository promoCodeRepository,
            InventoryLogRepository inventoryLogRepository,
            PromoService promoService
    ) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.promoService = promoService;
    }

    /**
     * Transactional multi-vendor checkout:
     * 1. Locks products in consistent ID order (deadlock-free pessimistic write lock).
     * 2. Validates active status & sufficient stock.
     * 3. Splits cart items into one Order per Seller.
     * 4. Applies Strategy-pattern promo discount proportionally across orders.
     * 5. Deducts stock, records inventory logs, and clears cart atomically.
     */
    @Transactional
    public List<OrderResponse> checkout(Long customerId, CheckoutRequest req) {
        Cart cart = cartRepository.findDetailedByCustomerId(customerId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty."));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Add at least one item before checking out.");
        }

        // Lock products in deterministic ID order to avoid concurrent checkout deadlocks
        List<CartItem> sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();

        Map<Long, Product> lockedProducts = new HashMap<>();
        for (CartItem item : sortedItems) {
            Product locked = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
            if (!locked.isActive()) {
                throw new BadRequestException("Product '" + locked.getName() + "' is no longer available.");
            }
            if (locked.getStockQuantity() < item.getQuantity()) {
                throw new ConflictException(
                        String.format("Insufficient stock for '%s'. Available: %d, Requested: %d.",
                                locked.getName(), locked.getStockQuantity(), item.getQuantity())
                );
            }
            lockedProducts.put(locked.getId(), locked);
        }

        // Group items by Seller
        Map<Long, List<CartItem>> bySeller = new LinkedHashMap<>();
        BigDecimal overallSubtotal = BigDecimal.ZERO;
        for (CartItem item : sortedItems) {
            Product p = lockedProducts.get(item.getProduct().getId());
            overallSubtotal = overallSubtotal.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            bySeller.computeIfAbsent(p.getSeller().getId(), k -> new ArrayList<>()).add(item);
        }

        // Validate promo code against overall checkout subtotal
        PromoCode promo = null;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        if (req.promoCode() != null && !req.promoCode().isBlank()) {
            promo = promoService.validatePromo(req.promoCode(), overallSubtotal);
            totalDiscount = promoService.calculateDiscountAmount(promo, overallSubtotal);
            promo.setUsedCount(promo.getUsedCount() + 1);
            promoCodeRepository.save(promo);
        }

        // Create one Order per Seller
        List<Order> createdOrders = new ArrayList<>();
        BigDecimal remainingDiscount = totalDiscount;
        int groupIdx = 0;
        int totalGroups = bySeller.size();

        for (List<CartItem> sellerItems : bySeller.values()) {
            groupIdx++;
            Product firstProduct = lockedProducts.get(sellerItems.getFirst().getProduct().getId());
            User seller = firstProduct.getSeller();

            Order order = new Order();
            order.setOrderNumber(generateOrderNumber());
            order.setCustomer(cart.getCustomer());
            order.setSeller(seller);
            order.setPromoCode(promo);
            order.setStatus(OrderStatus.PENDING);
            order.setShippingAddress(req.shippingAddress().trim());
            order.setPaymentMethod(req.paymentMethod().trim());

            BigDecimal sellerSubtotal = BigDecimal.ZERO;
            for (CartItem ci : sellerItems) {
                Product p = lockedProducts.get(ci.getProduct().getId());
                BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
                sellerSubtotal = sellerSubtotal.add(lineTotal);

                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(p);
                oi.setProductNameSnapshot(p.getName());
                oi.setUnitPriceSnapshot(p.getPrice());
                oi.setQuantity(ci.getQuantity());
                oi.setLineTotal(lineTotal);
                order.getItems().add(oi);
            }

            BigDecimal sellerDiscount;
            if (totalDiscount.compareTo(BigDecimal.ZERO) == 0 || overallSubtotal.compareTo(BigDecimal.ZERO) == 0) {
                sellerDiscount = BigDecimal.ZERO;
            } else if (groupIdx == totalGroups) {
                sellerDiscount = remainingDiscount;
            } else {
                sellerDiscount = totalDiscount.multiply(sellerSubtotal)
                        .divide(overallSubtotal, 2, RoundingMode.HALF_UP)
                        .min(remainingDiscount);
                remainingDiscount = remainingDiscount.subtract(sellerDiscount);
            }

            order.setSubtotalAmount(sellerSubtotal);
            order.setDiscountAmount(sellerDiscount);
            order.setTotalAmount(sellerSubtotal.subtract(sellerDiscount).max(BigDecimal.ZERO));

            Order savedOrder = orderRepository.save(order);
            createdOrders.add(savedOrder);

            // Deduct stock and write inventory audit log
            for (CartItem ci : sellerItems) {
                Product p = lockedProducts.get(ci.getProduct().getId());
                p.setStockQuantity(p.getStockQuantity() - ci.getQuantity());
                productRepository.save(p);
                inventoryLogRepository.save(
                        new InventoryLog(p, -ci.getQuantity(), InventoryReason.ORDER_DEDUCTION, savedOrder.getId())
                );
            }
        }

        // Clear customer cart
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return createdOrders.stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(Long customerId) {
        return orderRepository.findDetailedByCustomerId(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getSellerOrders(Long sellerId) {
        return orderRepository.findDetailedBySellerId(sellerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new ForbiddenActionException("You can only cancel your own orders.");
        }
        if (!order.getStatus().isCancellable()) {
            throw new ConflictException("Order cannot be cancelled once it is " + order.getStatus() + ".");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        // Restore stock atomically
        List<OrderItem> sortedItems = order.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();
        for (OrderItem item : sortedItems) {
            Product locked = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
            locked.setStockQuantity(locked.getStockQuantity() + item.getQuantity());
            productRepository.save(locked);
            inventoryLogRepository.save(
                    new InventoryLog(locked, item.getQuantity(), InventoryReason.ORDER_CANCEL_RESTORE, order.getId())
            );
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse advanceStatus(Long orderId, Long sellerId, boolean isAdmin, OrderStatus nextStatus) {
        Order order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));

        if (!isAdmin && !order.getSeller().getId().equals(sellerId)) {
            throw new ForbiddenActionException("You can only update orders belonging to your store.");
        }
        if (nextStatus == OrderStatus.CANCELLED) {
            throw new BadRequestException("Sellers cannot cancel orders via workflow progression.");
        }
        if (!order.getStatus().canTransitionTo(nextStatus)) {
            throw new ConflictException(
                    "Invalid status transition from " + order.getStatus() + " to " + nextStatus + "."
            );
        }

        order.setStatus(nextStatus);
        order.setUpdatedAt(LocalDateTime.now());
        return OrderResponse.from(orderRepository.save(order));
    }

    private String generateOrderNumber() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CP-" + LocalDateTime.now().getYear() + "-" + suffix;
    }
}
