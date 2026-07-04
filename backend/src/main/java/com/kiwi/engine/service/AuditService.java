package com.kiwi.engine.service;

import com.kiwi.engine.entity.EventLog;
import com.kiwi.engine.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final EventLogRepository eventLogRepository;

    public void log(Long userId, Long transactionId, EventLog.EventType type, String payload) {
        eventLogRepository.save(EventLog.builder()
                .userId(userId)
                .transactionId(transactionId)
                .eventType(type)
                .payload(payload)
                .build());
    }

    public List<EventLog> getEventsForUser(Long userId) {
        return eventLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<EventLog> getEventsForTransaction(Long transactionId) {
        return eventLogRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId);
    }
}
