package com.cartpulse.controller;

import com.cartpulse.dto.ReviewDtos.CreateReviewRequest;
import com.cartpulse.dto.ReviewDtos.ReviewResponse;
import com.cartpulse.security.CustomUserDetails;
import com.cartpulse.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewResponse> listReviews(@PathVariable Long productId) {
        return reviewService.getProductReviews(productId);
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        ReviewResponse created = reviewService.createReview(productId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
