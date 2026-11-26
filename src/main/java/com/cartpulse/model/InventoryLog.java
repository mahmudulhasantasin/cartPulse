package com.cartpulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "change_amount", nullable = false)
    private int changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InventoryReason reason;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public InventoryLog() {}

    public InventoryLog(Product product, int changeAmount, InventoryReason reason, Long referenceId) {
        this.product = product;
        this.changeAmount = changeAmount;
        this.reason = reason;
        this.referenceId = referenceId;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getChangeAmount() { return changeAmount; }
    public InventoryReason getReason() { return reason; }
    public Long getReferenceId() { return referenceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
