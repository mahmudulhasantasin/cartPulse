package com.cartpulse;

import com.cartpulse.discount.DiscountStrategyFactory;
import com.cartpulse.discount.FixedAmountDiscountStrategy;
import com.cartpulse.discount.PercentageDiscountStrategy;
import com.cartpulse.dto.OrderDtos.CheckoutRequest;
import com.cartpulse.dto.ReviewDtos.CreateReviewRequest;
import com.cartpulse.exception.BadRequestException;
import com.cartpulse.exception.ConflictException;
import com.cartpulse.exception.ForbiddenActionException;
import com.cartpulse.model.*;
import com.cartpulse.repository.*;
import com.cartpulse.service.OrderService;
import com.cartpulse.service.PromoService;
import com.cartpulse.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessLogicTests {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PromoCodeRepository promoCodeRepository;
    @Mock private InventoryLogRepository inventoryLogRepository;
    @Mock private PromoService promoService;
    @Mock private ReviewRepository reviewRepository;

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
        DiscountStrategyFactory factory = new DiscountStrategyFactory();

        assertInstanceOf(PercentageDiscountStrategy.class, factory.getStrategy(DiscountType.PERCENT));
        assertInstanceOf(FixedAmountDiscountStrategy.class, factory.getStrategy(DiscountType.FIXED));

        assertEquals(new BigDecimal("10.00"),
                factory.getStrategy(DiscountType.PERCENT).calculateDiscount(new BigDecimal("100.00"), new BigDecimal("10")));
        assertEquals(new BigDecimal("50.00"),
                factory.getStrategy(DiscountType.PERCENT).calculateDiscount(new BigDecimal("50.00"), new BigDecimal("150")));
        assertEquals(new BigDecimal("15.00"),
                factory.getStrategy(DiscountType.FIXED).calculateDiscount(new BigDecimal("100.00"), new BigDecimal("15")));
        assertEquals(new BigDecimal("12.00"),
                factory.getStrategy(DiscountType.FIXED).calculateDiscount(new BigDecimal("12.00"), new BigDecimal("25")));
    }

    @Test
    void checkoutRejectsEmptyCart() {
        OrderService orderService = new OrderService(
                cartRepository, productRepository, orderRepository, promoCodeRepository, inventoryLogRepository, promoService
        );
        User customer = new User("Buyer", "buyer@test.dev", "hash", Role.CUSTOMER);
        Cart emptyCart = new Cart(customer);
        when(cartRepository.findDetailedByCustomerId(1L)).thenReturn(Optional.of(emptyCart));

        assertThrows(BadRequestException.class, () ->
                orderService.checkout(1L, new CheckoutRequest("House 1, Road 2, Dhaka", "COD", null))
        );
    }

    @Test
    void checkoutThrowsConflictWhenStockIsInsufficient() throws Exception {
        OrderService orderService = new OrderService(
                cartRepository, productRepository, orderRepository, promoCodeRepository, inventoryLogRepository, promoService
        );
        User customer = new User("Buyer", "buyer@test.dev", "hash", Role.CUSTOMER);
        setId(customer, 1L);
        User seller = new User("Seller", "seller@test.dev", "hash", Role.SELLER);
        setId(seller, 2L);

        Product product = new Product();
        setId(product, 10L);
        product.setName("Keyboard");
        product.setSeller(seller);
        product.setPrice(new BigDecimal("50.00"));
        product.setStockQuantity(1);

        Cart cart = new Cart(customer);
        cart.getItems().add(new CartItem(cart, product, 3));

        when(cartRepository.findDetailedByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));

        assertThrows(ConflictException.class, () ->
                orderService.checkout(1L, new CheckoutRequest("House 1, Road 2, Dhaka", "COD", null))
        );
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderRestoresInventoryAndLogsAudit() throws Exception {
        OrderService orderService = new OrderService(
                cartRepository, productRepository, orderRepository, promoCodeRepository, inventoryLogRepository, promoService
        );
        User customer = new User("Buyer", "buyer@test.dev", "hash", Role.CUSTOMER);
        setId(customer, 1L);
        User seller = new User("Seller", "seller@test.dev", "hash", Role.SELLER);
        setId(seller, 2L);

        Product product = new Product();
        setId(product, 10L);
        product.setName("Headphones");
        product.setSeller(seller);
        product.setPrice(new BigDecimal("100.00"));
        product.setStockQuantity(5);

        Order order = new Order();
        setId(order, 99L);
        order.setOrderNumber("CP-2026-TEST");
        order.setCustomer(customer);
        order.setSeller(seller);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSubtotalAmount(new BigDecimal("200.00"));
        order.setTotalAmount(new BigDecimal("200.00"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductNameSnapshot("Headphones");
        item.setUnitPriceSnapshot(new BigDecimal("100.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("200.00"));
        order.getItems().add(item);

        when(orderRepository.findDetailedById(99L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.cancelOrder(99L, 1L);

        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals(7, product.getStockQuantity());
        verify(inventoryLogRepository, times(1)).save(any(InventoryLog.class));
    }

    @Test
    void reviewRequiresDeliveredPurchase() throws Exception {
        ReviewService reviewService = new ReviewService(reviewRepository, productRepository, orderRepository);
        Product product = new Product();
        setId(product, 10L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(reviewRepository.existsByProductIdAndCustomerId(10L, 1L)).thenReturn(false);
        when(orderRepository.findDeliveredOrdersForProduct(10L == 10L ? 1L : 0L, 10L, OrderStatus.DELIVERED))
                .thenReturn(List.of());

        assertThrows(ForbiddenActionException.class, () ->
                reviewService.createReview(10L, 1L, new CreateReviewRequest(5, "Great keyboard for daily coding!"))
        );
    }

    private static void setId(Object target, Long id) throws Exception {
        Field f = target.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(target, id);
    }
}
