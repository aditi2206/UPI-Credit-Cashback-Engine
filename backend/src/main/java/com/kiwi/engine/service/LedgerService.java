package com.kiwi.engine.service;

import com.kiwi.engine.entity.LedgerEntry;
import com.kiwi.engine.entity.Transaction;
import com.kiwi.engine.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Writes double-entry ledger rows for a completed payment.
     *
     * Movement 1 — credit consumed:
     *   CREDIT_LIMIT (user) → DEBIT (user used ₹amount of their credit card)
     *
     * Movement 2 — cashback rewarded (if any):
     *   CASHBACK_POOL (system) → DEBIT  (Kiwi's pool gives out ₹cashback)
     *   USER_WALLET (user)     → CREDIT (user receives ₹cashback)
     */
    public List<LedgerEntry> recordPayment(Transaction transaction, BigDecimal cashback) {
        List<LedgerEntry> entries = new ArrayList<>();
        String userRef = "USER_" + transaction.getUser().getId();

        // Movement 1: credit limit consumption
        entries.add(LedgerEntry.builder()
                .transaction(transaction)
                .accountType(LedgerEntry.AccountType.CREDIT_LIMIT)
                .accountRef(userRef)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(transaction.getAmount())
                .build());

        // Movement 2: cashback (only if earned)
        if (cashback.compareTo(BigDecimal.ZERO) > 0) {
            entries.add(LedgerEntry.builder()
                    .transaction(transaction)
                    .accountType(LedgerEntry.AccountType.CASHBACK_POOL)
                    .accountRef("SYSTEM")
                    .entryType(LedgerEntry.EntryType.DEBIT)
                    .amount(cashback)
                    .build());

            entries.add(LedgerEntry.builder()
                    .transaction(transaction)
                    .accountType(LedgerEntry.AccountType.USER_WALLET)
                    .accountRef(userRef)
                    .entryType(LedgerEntry.EntryType.CREDIT)
                    .amount(cashback)
                    .build());
        }

        List<LedgerEntry> saved = ledgerEntryRepository.saveAll(entries);
        log.debug("Wrote {} ledger entries for txn={}", saved.size(), transaction.getId());
        return saved;
    }

    public BigDecimal getWalletBalance(Long userId) {
        return ledgerEntryRepository.calculateWalletBalance("USER_" + userId);
    }

    public List<LedgerEntry> getLedgerForUser(Long userId) {
        return ledgerEntryRepository.findByAccountRefOrderByCreatedAtDesc("USER_" + userId);
    }
}
