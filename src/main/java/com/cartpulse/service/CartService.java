package com.cartpulse.service;

import com.cartpulse.dto.CartDtos.AddToCartRequest;
import com.cartpulse.dto.CartDtos.CartItemResponse;
import com.cartpulse.dto.CartDtos.CartResponse;
import com.cartpulse.exception.BadRequestException;
import com.cartpulse.exception.ConflictException;
import com.cartpulse.exception.ResourceNotFoundException;
import com.cartpulse.model.Cart;
import com.cartpulse.model.CartItem;
import com.cartpulse.model.Product;
import com.cartpulse.model.User;
import com.cartpulse.repository.CartItemRepository;
import com.cartpulse.repository.CartRepository;
import com.cartpulse.repository.ProductRepository;
import com.cartpulse.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getCart(Long customerId) {
        Cart cart = loadOrCreateDetailedCart(customerId);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(Long customerId, AddToCartRequest req) {
        Product product = productRepository.findDetailedById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        if (!product.isActive()) {
            throw new BadRequestException("This product is no longer available.");
        }
        if (product.getStockQuantity() <= 0) {
            throw new ConflictException("This product is currently out of stock.");
        }

        Cart cart = loadOrCreateDetailedCart(customerId);
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int desiredQty = (existing != null ? existing.getQuantity() : 0) + req.quantity();
        if (desiredQty > product.getStockQuantity()) {
            throw new ConflictException("Only " + product.getStockQuantity() + " units available in stock.");
        }

        if (existing != null) {
            existing.setQuantity(desiredQty);
        } else {
            cart.getItems().add(new CartItem(cart, product, desiredQty));
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(loadOrCreateDetailedCart(customerId));
    }

    @Transactional
    public CartResponse updateQuantity(Long customerId, Long productId, int quantity) {
        Cart cart = loadOrCreateDetailedCart(customerId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item is not in your cart."));

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            if (quantity > item.getProduct().getStockQuantity()) {
                throw new ConflictException("Only " + item.getProduct().getStockQuantity() + " units available in stock.");
            }
            item.setQuantity(quantity);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(loadOrCreateDetailedCart(customerId));
    }

    @Transactional
    public CartResponse clearCart(Long customerId) {
        Cart cart = loadOrCreateDetailedCart(customerId);
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    private Cart loadOrCreateDetailedCart(Long customerId) {
        return cartRepository.findDetailedByCustomerId(customerId)
                .orElseGet(() -> {
                    User customer = userRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
                    return cartRepository.save(new Cart(customer));
                });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemDtos = cart.getItems().stream()
                .map(i -> {
                    Product p = i.getProduct();
                    BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
                    return new CartItemResponse(
                            i.getId(),
                            p.getId(),
                            p.getName(),
                            p.getImageUrl(),
                            p.getSeller().getId(),
                            p.getSeller().getFullName(),
                            p.getPrice(),
                            i.getQuantity(),
                            p.getStockQuantity(),
                            lineTotal
                    );
                })
                .toList();

        BigDecimal subtotal = itemDtos.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemDtos.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        return new CartResponse(itemDtos, subtotal, totalItems);
    }
}
