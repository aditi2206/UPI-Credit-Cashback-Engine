package com.kiwi.engine.repository;

import com.kiwi.engine.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findRecentByUserId(@Param("userId") Long userId);
}
