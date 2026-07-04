package com.kiwi.engine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cashback_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashbackRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transaction.Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private Transaction.PaymentMode paymentMode;

    // e.g. 5.00 means 5%
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    // Monthly cap per user for this rule (e.g. ₹500)
    @Column(name = "monthly_cap", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyCap;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return Boolean.TRUE.equals(isActive)
                && !now.isBefore(validFrom)
                && (validTo == null || now.isBefore(validTo));
    }
}
