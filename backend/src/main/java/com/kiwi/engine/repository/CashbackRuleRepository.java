package com.kiwi.engine.repository;

import com.kiwi.engine.entity.CashbackRule;
import com.kiwi.engine.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CashbackRuleRepository extends JpaRepository<CashbackRule, Long> {

    @Query("""
        SELECT r FROM CashbackRule r
        WHERE r.category = :category
          AND r.paymentMode = :mode
          AND r.isActive = true
          AND r.validFrom <= :now
          AND (r.validTo IS NULL OR r.validTo > :now)
        ORDER BY r.percentage DESC
    """)
    List<CashbackRule> findApplicableRules(
        @Param("category") Transaction.Category category,
        @Param("mode") Transaction.PaymentMode mode,
        @Param("now") LocalDateTime now
    );
}
