package com.kiwi.engine.repository;

import com.kiwi.engine.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionId(Long transactionId);

    // Derive wallet balance: sum of CREDITs minus sum of DEBITs for USER_WALLET
    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN l.entryType = 'CREDIT' THEN l.amount
                 ELSE -l.amount END), 0)
        FROM LedgerEntry l
        WHERE l.accountType = 'USER_WALLET'
          AND l.accountRef = :accountRef
    """)
    BigDecimal calculateWalletBalance(@Param("accountRef") String accountRef);

    // Cashback already earned this month for cap enforcement
    @Query("""
        SELECT COALESCE(SUM(l.amount), 0)
        FROM LedgerEntry l
        WHERE l.accountType = 'USER_WALLET'
          AND l.accountRef = :accountRef
          AND l.entryType = 'CREDIT'
          AND l.createdAt >= :from
          AND l.createdAt < :to
    """)
    BigDecimal sumCashbackInPeriod(
        @Param("accountRef") String accountRef,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    List<LedgerEntry> findByAccountRefOrderByCreatedAtDesc(String accountRef);
}
