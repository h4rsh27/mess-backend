package com.smartmess.dto;

import com.smartmess.enums.PaymentStatus;
import com.smartmess.enums.PlanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentDto {

    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID messId;
    private String messName;
    private Integer month;
    private Integer year;
    private BigDecimal amount;
    private PaymentStatus status;
    private PlanType planType;
    private LocalDate paidAt;
    private String transactionId;
    private String notes;
    private LocalDateTime createdAt;
}
