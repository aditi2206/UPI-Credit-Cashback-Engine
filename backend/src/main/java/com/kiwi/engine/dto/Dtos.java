package com.kiwi.engine.dto;

import com.kiwi.engine.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    // ── Requests ──────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PaymentRequest {
        @NotNull(message = "User ID is required")
        private Long userId;

        @NotNull @DecimalMin(value = "1.0", message = "Amount must be at least ₹1")
        private BigDecimal amount;

        @NotNull(message = "Category is required")
        private Transaction.Category category;

        @NotNull(message = "Payment mode is required")
        private Transaction.PaymentMode paymentMode;

        @NotBlank(message = "Idempotency key is required")
        @Size(min = 8, max = 64)
        private String idempotencyKey;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreateUserRequest {
        @NotBlank private String name;
        @NotBlank @Email private String email;
        @NotNull @DecimalMin("1000") private BigDecimal creditLimit;
    }

    // ── Responses ─────────────────────────────────────────

    @Getter @Setter @Builder
    public static class PaymentResponse {
        private Long transactionId;
        private BigDecimal amount;
        private BigDecimal cashbackEarned;
        private BigDecimal walletBalance;
        private BigDecimal availableCredit;
        private String status;
        private boolean idempotencyHit;   // true if this was a duplicate request
        private String message;
        private LocalDateTime processedAt;
    }

    @Getter @Setter @Builder
    public static class UserSummaryResponse {
        private Long userId;
        private String name;
        private String email;
        private BigDecimal creditLimit;
        private BigDecimal usedCredit;
        private BigDecimal availableCredit;
        private BigDecimal walletBalance;
        private List<TransactionSummary> recentTransactions;
    }

    @Getter @Setter @Builder
    public static class TransactionSummary {
        private Long id;
        private BigDecimal amount;
        private String category;
        private String paymentMode;
        private String status;
        private BigDecimal cashbackEarned;
        private LocalDateTime createdAt;
    }

    @Getter @Setter @Builder
    public static class LedgerEntryResponse {
        private Long id;
        private String accountType;
        private String accountRef;
        private String entryType;
        private BigDecimal amount;
        private LocalDateTime createdAt;
    }

    @Getter @Setter @Builder
    public static class ErrorResponse {
        private String code;
        private String message;
        private LocalDateTime timestamp;
    }
}
