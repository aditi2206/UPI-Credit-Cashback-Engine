package com.kiwi.engine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_account_ref", columnList = "account_ref"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    // For USER_WALLET / CREDIT_LIMIT: userId. For CASHBACK_POOL: "SYSTEM"
    @Column(name = "account_ref", nullable = false)
    private String accountRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum AccountType {
        USER_WALLET,     // User's cashback wallet
        CASHBACK_POOL,   // Kiwi's pool funding cashback rewards
        CREDIT_LIMIT     // Tracks credit consumed by the user
    }

    public enum EntryType {
        DEBIT, CREDIT
    }
}
