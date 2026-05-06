package com.smartmess.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentSummaryDto {

    private long totalStudents;
    private long paidCount;
    private long unpaidCount;
    private long partialCount;
    private BigDecimal totalRevenue;
    private BigDecimal pendingRevenue;
    private int month;
    private int year;
}
