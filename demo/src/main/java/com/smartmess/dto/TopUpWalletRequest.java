package com.smartmess.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopUpWalletRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Top up amount must be at least 1")
    private BigDecimal amount;

    @Size(max = 255, message = "Reference must not exceed 255 characters")
    private String reference;
}
