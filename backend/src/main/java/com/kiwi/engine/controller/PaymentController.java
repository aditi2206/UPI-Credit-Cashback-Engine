package com.kiwi.engine.controller;

import com.kiwi.engine.dto.Dtos.*;
import com.kiwi.engine.entity.LedgerEntry;
import com.kiwi.engine.service.AuditService;
import com.kiwi.engine.service.LedgerService;
import com.kiwi.engine.service.PaymentService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final LedgerService ledgerService;
    private final AuditService auditService;

    // Simple in-memory rate limiter: 10 requests/minute globally (demo purposes)
    private final Bucket rateLimiter = Bucket.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
            .build();

    @PostMapping
    public ResponseEntity<?> makePayment(@Valid @RequestBody PaymentRequest request) {
        if (!rateLimiter.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ErrorResponse.builder()
                            .code("RATE_LIMITED")
                            .message("Too many requests. Please slow down.")
                            .build());
        }
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ledger/{userId}")
    public ResponseEntity<List<LedgerEntryResponse>> getLedger(@PathVariable Long userId) {
        List<LedgerEntryResponse> entries = ledgerService.getLedgerForUser(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/audit/{userId}")
    public ResponseEntity<?> getAuditLog(@PathVariable Long userId) {
        return ResponseEntity.ok(auditService.getEventsForUser(userId));
    }

    private LedgerEntryResponse toResponse(LedgerEntry e) {
        return LedgerEntryResponse.builder()
                .id(e.getId())
                .accountType(e.getAccountType().name())
                .accountRef(e.getAccountRef())
                .entryType(e.getEntryType().name())
                .amount(e.getAmount())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
