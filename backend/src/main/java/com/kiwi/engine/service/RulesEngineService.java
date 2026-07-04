package com.kiwi.engine.service;

import com.kiwi.engine.entity.CashbackRule;
import com.kiwi.engine.entity.Transaction;
import com.kiwi.engine.repository.CashbackRuleRepository;
import com.kiwi.engine.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RulesEngineService {

    private final CashbackRuleRepository ruleRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Finds the best applicable rule and calculates final cashback after cap enforcement.
     */
    public BigDecimal calculateCashback(Long userId, BigDecimal paymentAmount,
                                        Transaction.Category category,
                                        Transaction.PaymentMode paymentMode) {

        List<CashbackRule> rules = ruleRepository.findApplicableRules(
                category, paymentMode, LocalDateTime.now());

        if (rules.isEmpty()) {
            log.debug("No cashback rules found for category={} mode={}", category, paymentMode);
            return BigDecimal.ZERO;
        }

        // Pick the highest-percentage rule
        CashbackRule bestRule = rules.get(0);
        log.debug("Applying rule id={} percentage={} cap={}", bestRule.getId(),
                bestRule.getPercentage(), bestRule.getMonthlyCap());

        // Raw cashback = amount * percentage / 100
        BigDecimal rawCashback = paymentAmount
                .multiply(bestRule.getPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Enforce monthly cap
        BigDecimal alreadyEarned = getMonthlyEarned(userId, category, paymentMode);
        BigDecimal remaining = bestRule.getMonthlyCap().subtract(alreadyEarned);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Monthly cap already reached for user={}", userId);
            return BigDecimal.ZERO;
        }

        BigDecimal finalCashback = rawCashback.min(remaining);
        log.debug("Cashback calculated: raw={} alreadyEarned={} remaining={} final={}",
                rawCashback, alreadyEarned, remaining, finalCashback);
        return finalCashback;
    }

    private BigDecimal getMonthlyEarned(Long userId, Transaction.Category category,
                                         Transaction.PaymentMode mode) {
        YearMonth current = YearMonth.now();
        LocalDateTime from = current.atDay(1).atStartOfDay();
        LocalDateTime to = current.atEndOfMonth().atTime(23, 59, 59);
        String accountRef = "USER_" + userId;
        return ledgerEntryRepository.sumCashbackInPeriod(accountRef, from, to);
    }

    public Optional<CashbackRule> findBestRule(Transaction.Category category,
                                                Transaction.PaymentMode mode) {
        List<CashbackRule> rules = ruleRepository.findApplicableRules(
                category, mode, LocalDateTime.now());
        return rules.isEmpty() ? Optional.empty() : Optional.of(rules.get(0));
    }
}
