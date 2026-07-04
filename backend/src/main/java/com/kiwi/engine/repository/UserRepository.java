package com.kiwi.engine.repository;

import com.kiwi.engine.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Pessimistic write lock for credit limit deduction — prevents concurrent overdraft
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);
}
