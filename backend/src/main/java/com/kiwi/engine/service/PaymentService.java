package com.kiwi.engine.service;

import com.kiwi.engine.dto.Dtos.*;
import com.kiwi.engine.entity.*;
import com.kiwi.engine.exception.InsufficientCreditException;
import com.kiwi.engine.exception.UserNotFoundException;
import com.kiwi.engine.repository.TransactionRepository;
import com.kiwi.engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RulesEngineService rulesEngineService;
    private final LedgerService ledgerService;
    private final AuditService auditService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment: userId={} amount={} key={}",
                request.getUserId(), request.getAmount(), request.getIdempotencyKey());

        // ── Step 1: Idempotency check
        Optional<Transaction> existing = transactionRepository
                .findByIdempotencyKey(request.getIdempotencyKey());

        if (existing.isPresent()) {
            log.info("Idempotency hit for key={}", request.getIdempotencyKey());
            Transaction txn = existing.get();
            auditService.log(request.getUserId(), txn.getId(),
                    EventLog.EventType.IDEMPOTENCY_HIT,
                    "{\"key\":\"" + request.getIdempotencyKey() + "\"}");

            return PaymentResponse.builder()
                    .transactionId(txn.getId())
                    .amount(txn.getAmount())
                    .cashbackEarned(txn.getCashbackEarned())
                    .walletBalance(ledgerService.getWalletBalance(request.getUserId()))
                    .availableCredit(getUserAvailableCredit(request.getUserId()))
                    .status(txn.getStatus().name())
                    .idempotencyHit(true)
                    .message("Duplicate request : returning original result")
                    .processedAt(txn.getCreatedAt())
                    .build();
        }

        // ── Step 2: Load user with optimistic lock
        User user = userRepository.findByIdWithLock(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));

        auditService.log(user.getId(), null, EventLog.EventType.PAYMENT_INITIATED,
                String.format("{\"amount\":%s,\"category\":\"%s\",\"mode\":\"%s\"}",
                        request.getAmount(), request.getCategory(), request.getPaymentMode()));

        // ── Step 3: Credit limit check
        if (user.getAvailableCredit().compareTo(request.getAmount()) < 0) {
            auditService.log(user.getId(), null, EventLog.EventType.LIMIT_INSUFFICIENT,
                    String.format("{\"requested\":%s,\"available\":%s}",
                            request.getAmount(), user.getAvailableCredit()));
            throw new InsufficientCreditException(request.getAmount(), user.getAvailableCredit());
        }

        // ── Step 4: Create transaction (PENDING)
        Transaction txn = Transaction.builder()
                .user(user)
                .amount(request.getAmount())
                .category(request.getCategory())
                .paymentMode(request.getPaymentMode())
                .idempotencyKey(request.getIdempotencyKey())
                .status(Transaction.TransactionStatus.PENDING)
                .build();
        txn = transactionRepository.save(txn);

        // ── Step 5: Deduct credit limit
        user.setUsedCredit(user.getUsedCredit().add(request.getAmount()));
        userRepository.save(user);

        // ── Step 6: Calculate cashback via rules engine
        BigDecimal cashback = rulesEngineService.calculateCashback(
                user.getId(), request.getAmount(),
                request.getCategory(), request.getPaymentMode());

        auditService.log(user.getId(), txn.getId(), EventLog.EventType.CASHBACK_APPLIED,
                String.format("{\"cashback\":%s}", cashback));

        // ── Step 7: Write ledger entries (double-entry)
        ledgerService.recordPayment(txn, cashback);

        // ── Step 8: Mark transaction SUCCESS
        txn.setCashbackEarned(cashback);
        txn.setStatus(Transaction.TransactionStatus.SUCCESS);
        txn = transactionRepository.save(txn);

        auditService.log(user.getId(), txn.getId(), EventLog.EventType.PAYMENT_SUCCESS,
                String.format("{\"transactionId\":%d,\"cashback\":%s}", txn.getId(), cashback));

        BigDecimal walletBalance = ledgerService.getWalletBalance(user.getId());

        log.info("Payment SUCCESS: txnId={} cashback={} walletBalance={}",
                txn.getId(), cashback, walletBalance);

        return PaymentResponse.builder()
                .transactionId(txn.getId())
                .amount(txn.getAmount())
                .cashbackEarned(cashback)
                .walletBalance(walletBalance)
                .availableCredit(user.getAvailableCredit())
                .status("SUCCESS")
                .idempotencyHit(false)
                .message(cashback.compareTo(BigDecimal.ZERO) > 0
                        ? "Payment successful! ₹" + cashback + " cashback earned."
                        : "Payment successful! No cashback for this transaction.")
                .processedAt(LocalDateTime.now())
                .build();
    }

    private BigDecimal getUserAvailableCredit(Long userId) {
        return userRepository.findById(userId)
                .map(User::getAvailableCredit)
                .orElse(BigDecimal.ZERO);
    }
}
