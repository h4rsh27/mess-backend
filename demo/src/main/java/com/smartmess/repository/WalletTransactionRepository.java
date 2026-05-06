package com.smartmess.repository;

import com.smartmess.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    List<WalletTransaction> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}
