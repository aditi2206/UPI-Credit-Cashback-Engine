package com.kiwi.engine.repository;

import com.kiwi.engine.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    List<EventLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<EventLog> findByTransactionIdOrderByCreatedAtAsc(Long transactionId);
}
