package com.kiwi.engine.service;

import com.kiwi.engine.dto.Dtos.*;
import com.kiwi.engine.entity.User;
import com.kiwi.engine.exception.UserNotFoundException;
import com.kiwi.engine.repository.TransactionRepository;
import com.kiwi.engine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;

    public User createUser(CreateUserRequest request) {
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .creditLimit(request.getCreditLimit())
                .build();
        return userRepository.save(user);
    }

    public UserSummaryResponse getUserSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<TransactionSummary> recent = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(10)
                .map(t -> TransactionSummary.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .category(t.getCategory().name())
                        .paymentMode(t.getPaymentMode().name())
                        .status(t.getStatus().name())
                        .cashbackEarned(t.getCashbackEarned())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return UserSummaryResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .creditLimit(user.getCreditLimit())
                .usedCredit(user.getUsedCredit())
                .availableCredit(user.getAvailableCredit())
                .walletBalance(ledgerService.getWalletBalance(userId))
                .recentTransactions(recent)
                .build();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
