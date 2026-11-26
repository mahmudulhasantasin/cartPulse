package com.cartpulse.repository;

import com.cartpulse.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("SELECT r FROM Review r JOIN FETCH r.customer WHERE r.product.id = :productId ORDER BY r.createdAt DESC")
    List<Review> findDetailedByProductId(@Param("productId") Long productId);

    boolean existsByProductIdAndCustomerId(Long productId, Long customerId);
}
