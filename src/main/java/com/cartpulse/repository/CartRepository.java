package com.cartpulse.repository;

import com.cartpulse.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product p LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.category WHERE c.customer.id = :customerId")
    Optional<Cart> findDetailedByCustomerId(@Param("customerId") Long customerId);

    Optional<Cart> findByCustomerId(Long customerId);
}
