package com.cartpulse.repository;

import com.cartpulse.model.Order;
import com.cartpulse.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.seller JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    List<Order> findDetailedByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.seller JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.seller.id = :sellerId ORDER BY o.createdAt DESC")
    List<Order> findDetailedBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.seller JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findDetailedById(@Param("id") Long id);

    @Query("SELECT o FROM Order o JOIN o.items i WHERE o.customer.id = :customerId AND i.product.id = :productId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findDeliveredOrdersForProduct(@Param("customerId") Long customerId,
                                              @Param("productId") Long productId,
                                              @Param("status") OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.product p JOIN FETCH p.category WHERE o.seller.id = :sellerId AND o.status <> :excludedStatus")
    List<Order> findActiveOrdersForSellerAnalytics(@Param("sellerId") Long sellerId,
                                                   @Param("excludedStatus") OrderStatus excludedStatus);

    @Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items i JOIN FETCH i.product p JOIN FETCH p.category")
    List<Order> findAllDetailedForAdminAnalytics();
}
