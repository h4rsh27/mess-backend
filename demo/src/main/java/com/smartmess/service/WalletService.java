package com.smartmess.service;

import com.smartmess.dto.WalletDto;
import com.smartmess.dto.WalletTransactionDto;
import com.smartmess.entity.Wallet;
import com.smartmess.entity.WalletTransaction;
import com.smartmess.enums.WalletTransactionType;
import com.smartmess.repository.WalletRepository;
import com.smartmess.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Value("${app.wallet.low-balance-threshold:300}")
    private BigDecimal defaultLowBalanceThreshold;

    @Transactional
    public WalletDto getWallet(UUID userId) {
        userService.findById(userId);
        return toDto(getOrCreateWalletEntity(userId));
    }

    @Transactional
    public WalletDto topUp(UUID userId, BigDecimal amount, String reference) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Top up amount must be greater than zero.");
        }
        Wallet wallet = getOrCreateWalletEntity(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.CREDIT, amount, reference, null, "WALLET_TOPUP");
        log.info("Wallet credited for user {} amount {}", userId, amount);
        return toDto(wallet);
    }

    @Transactional
    public Wallet debit(UUID userId, BigDecimal amount, UUID referenceId, String description) {
        Wallet wallet = getOrCreateWalletEntity(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Please top up before booking.");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet = walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.DEBIT, amount, description, referenceId, "MEAL_BOOKING");
        notifyLowBalanceIfNeeded(wallet);
        return wallet;
    }

    @Transactional
    public Wallet refund(UUID userId, BigDecimal amount, UUID referenceId, String description) {
        Wallet wallet = getOrCreateWalletEntity(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet = walletRepository.save(wallet);
        createTransaction(wallet, WalletTransactionType.REFUND, amount, description, referenceId, "MEAL_REFUND");
        return wallet;
    }

    public List<WalletTransactionDto> getRecentTransactions(UUID userId) {
        userService.findById(userId);
        return walletTransactionRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toTransactionDto)
                .toList();
    }

    @Transactional
    public Wallet getOrCreateWalletEntity(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .userId(userId)
                        .lowBalanceThreshold(defaultLowBalanceThreshold)
                        .build()));
    }

    private void notifyLowBalanceIfNeeded(Wallet wallet) {
        if (wallet.getBalance().compareTo(wallet.getLowBalanceThreshold()) <= 0) {
            notificationService.sendNotification(
                    wallet.getUserId(),
                    "Wallet running low",
                    "Your daily meal wallet is running low. Top up to continue booking meals.",
                    com.smartmess.enums.NotificationType.WALLET_LOW
            );
        }
    }

    private void createTransaction(Wallet wallet, WalletTransactionType type, BigDecimal amount,
                                   String description, UUID referenceId, String referenceType) {
        walletTransactionRepository.save(WalletTransaction.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUserId())
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .referenceId(referenceId)
                .referenceType(referenceType)
                .description(description)
                .build());
    }

    private WalletDto toDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .lowBalanceThreshold(wallet.getLowBalanceThreshold())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionDto toTransactionDto(WalletTransaction transaction) {
        return WalletTransactionDto.builder()
                .id(transaction.getId())
                .walletId(transaction.getWalletId())
                .userId(transaction.getUserId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .balanceAfter(transaction.getBalanceAfter())
                .referenceId(transaction.getReferenceId())
                .referenceType(transaction.getReferenceType())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
