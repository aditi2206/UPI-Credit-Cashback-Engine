package com.kiwi.engine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_log", indexes = {
    @Index(name = "idx_event_user_id", columnList = "user_id"),
    @Index(name = "idx_event_type", columnList = "event_type")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    // JSON snapshot of the state at time of event
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum EventType {
        PAYMENT_INITIATED,
        CREDIT_LIMIT_CHECKED,
        IDEMPOTENCY_HIT,       // Duplicate request detected
        CASHBACK_APPLIED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        LIMIT_INSUFFICIENT
    }
}
