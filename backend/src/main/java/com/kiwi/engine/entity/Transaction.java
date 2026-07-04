package com.kiwi.engine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "cashback_earned", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cashbackEarned = BigDecimal.ZERO;

    // Idempotency key — client sends this, we dedup on it
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.cashbackEarned == null) this.cashbackEarned = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Category {
        GROCERY, FOOD, FUEL, TRAVEL, SHOPPING, UTILITIES, OTHER
    }

    public enum PaymentMode {
        UPI_SCAN, UPI_ONLINE, ONLINE
    }

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED
    }
}
