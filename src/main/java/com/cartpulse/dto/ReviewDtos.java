package com.cartpulse.dto;

import com.cartpulse.model.Review;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public final class ReviewDtos {

    private ReviewDtos() {}

    public record CreateReviewRequest(
            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5")
            int rating,

            @NotBlank(message = "Review comment is required")
            @Size(min = 10, max = 1000, message = "Review must be between 10 and 1000 characters")
            String comment
    ) {}

    public record ReviewResponse(
            Long id,
            Long productId,
            String customerName,
            int rating,
            String comment,
            LocalDateTime createdAt
    ) {
        public static ReviewResponse from(Review r) {
            return new ReviewResponse(
                    r.getId(),
                    r.getProduct().getId(),
                    r.getCustomer().getFullName(),
                    r.getRating(),
                    r.getComment(),
                    r.getCreatedAt()
            );
        }
    }
}
