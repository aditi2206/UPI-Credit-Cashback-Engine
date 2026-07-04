package com.kiwi.engine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "used_credit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal usedCredit = BigDecimal.ZERO;

    // Optimistic locking — prevents concurrent update bugs
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.usedCredit == null) this.usedCredit = BigDecimal.ZERO;
    }

    public BigDecimal getAvailableCredit() {
        return creditLimit.subtract(usedCredit);
    }
}
