package com.smartmess.dto;

import com.smartmess.enums.WalletTransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WalletTransactionDto {
    private UUID id;
    private UUID walletId;
    private UUID userId;
    private WalletTransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private UUID referenceId;
    private String referenceType;
    private String description;
    private LocalDateTime createdAt;
}
