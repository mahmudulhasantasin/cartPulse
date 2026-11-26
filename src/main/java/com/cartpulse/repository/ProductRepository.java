package com.cartpulse.repository;

import com.cartpulse.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Product p JOIN FETCH p.category JOIN FETCH p.seller WHERE p.id = :id")
    Optional<Product> findDetailedById(@Param("id") Long id);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.seller.id = :sellerId ORDER BY p.createdAt DESC")
    List<Product> findAllBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.seller.id = :sellerId AND p.active = true AND p.stockQuantity <= p.lowStockThreshold ORDER BY p.stockQuantity ASC")
    List<Product> findLowStockBySeller(@Param("sellerId") Long sellerId);
}
