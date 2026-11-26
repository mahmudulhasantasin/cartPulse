package com.cartpulse.service;

import com.cartpulse.dto.ReviewDtos.CreateReviewRequest;
import com.cartpulse.dto.ReviewDtos.ReviewResponse;
import com.cartpulse.exception.ConflictException;
import com.cartpulse.exception.ForbiddenActionException;
import com.cartpulse.exception.ResourceNotFoundException;
import com.cartpulse.model.Order;
import com.cartpulse.model.OrderStatus;
import com.cartpulse.model.Product;
import com.cartpulse.model.Review;
import com.cartpulse.repository.OrderRepository;
import com.cartpulse.repository.ProductRepository;
import com.cartpulse.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found.");
        }
        return reviewRepository.findDetailedByProductId(productId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse createReview(Long productId, Long customerId, CreateReviewRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        if (reviewRepository.existsByProductIdAndCustomerId(productId, customerId)) {
            throw new ConflictException("You have already reviewed this product.");
        }

        List<Order> deliveredOrders = orderRepository.findDeliveredOrdersForProduct(
                customerId, productId, OrderStatus.DELIVERED
        );
        if (deliveredOrders.isEmpty()) {
            throw new ForbiddenActionException("Only verified buyers with a DELIVERED order can review this product.");
        }

        Order verifiedOrder = deliveredOrders.getFirst();
        Review review = new Review();
        review.setProduct(product);
        review.setCustomer(verifiedOrder.getCustomer());
        review.setOrder(verifiedOrder);
        review.setRating(req.rating());
        review.setComment(req.comment().trim());

        Review saved = reviewRepository.save(review);

        // Recalculate product rating summary
        List<Review> allReviews = reviewRepository.findDetailedByProductId(productId);
        int count = allReviews.size();
        double avg = allReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        product.setReviewCount(count);
        product.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        productRepository.save(product);

        return ReviewResponse.from(saved);
    }
}
