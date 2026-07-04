package com.kiwi.engine;

import com.kiwi.engine.dto.Dtos.PaymentRequest;
import com.kiwi.engine.dto.Dtos.PaymentResponse;
import com.kiwi.engine.entity.Transaction;
import com.kiwi.engine.entity.User;
import com.kiwi.engine.repository.UserRepository;
import com.kiwi.engine.service.LedgerService;
import com.kiwi.engine.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyTest {

    @Autowired private PaymentService paymentService;
    @Autowired private UserRepository userRepository;
    @Autowired private LedgerService ledgerService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .name("Concurrency Test User")
                .email("concurrency_" + UUID.randomUUID() + "@test.com")
                .creditLimit(new BigDecimal("100000.00"))
                .build());
    }

    @Test
    void fiftySimultaneousPayments_balanceShouldBeExact() throws InterruptedException {
        int threadCount = 50;
        BigDecimal amountPerPayment = new BigDecimal("100.00");
        // 5% cashback on UPI_SCAN, so each payment earns ₹5 cashback
        BigDecimal expectedCashbackPerPayment = new BigDecimal("5.00");
        BigDecimal expectedTotalCashback = expectedCashbackPerPayment
                .multiply(BigDecimal.valueOf(threadCount));

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<PaymentResponse>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String key = "test-key-" + i + "-" + UUID.randomUUID();
            futures.add(executor.submit(() -> {
                latch.await(); // All threads start simultaneously
                try {
                    PaymentRequest req = new PaymentRequest(
                            testUser.getId(),
                            amountPerPayment,
                            Transaction.Category.GROCERY,
                            Transaction.PaymentMode.UPI_SCAN,
                            key
                    );
                    PaymentResponse res = paymentService.processPayment(req);
                    successCount.incrementAndGet();
                    return res;
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    return null;
                }
            }));
        }

        latch.countDown(); // Fire all threads at once
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("=== Concurrency Test Results ===");
        System.out.println("Successful payments: " + successCount.get());
        System.out.println("Failed payments:     " + failCount.get());

        BigDecimal actualWalletBalance = ledgerService.getWalletBalance(testUser.getId());
        BigDecimal actualUsedCredit = userRepository.findById(testUser.getId())
                .map(User::getUsedCredit)
                .orElse(BigDecimal.ZERO);

        System.out.println("Expected wallet balance: ₹" + expectedTotalCashback);
        System.out.println("Actual wallet balance:   ₹" + actualWalletBalance);
        System.out.println("Expected used credit:    ₹" + amountPerPayment.multiply(BigDecimal.valueOf(successCount.get())));
        System.out.println("Actual used credit:      ₹" + actualUsedCredit);

        // All 50 should succeed (each has a unique idempotency key and there's enough limit)
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Wallet balance must be exactly successCount * ₹5 — no lost updates
        assertThat(actualWalletBalance).isEqualByComparingTo(expectedTotalCashback);

        // Used credit must be exactly successCount * ₹100
        BigDecimal expectedUsed = amountPerPayment.multiply(BigDecimal.valueOf(threadCount));
        assertThat(actualUsedCredit).isEqualByComparingTo(expectedUsed);
    }
}
