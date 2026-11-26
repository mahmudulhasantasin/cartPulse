package com.cartpulse.repository;

import com.cartpulse.model.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    List<InventoryLog> findTop30ByProductIdOrderByCreatedAtDesc(Long productId);
}
