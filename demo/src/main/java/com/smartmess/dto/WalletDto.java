package com.smartmess.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WalletDto {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
    private BigDecimal lowBalanceThreshold;
    private LocalDateTime updatedAt;
}
