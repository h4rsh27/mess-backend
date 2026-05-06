package com.smartmess.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OwnerDashboardDto {

    private UUID messId;
    private String messName;
    private long totalStudents;
    private long todayAttendance;
    private BigDecimal monthlyRevenue;
    private BigDecimal pendingRevenue;
    private long openComplaints;
    private long unreadNotifications;
    private double overallRating;
    private PaymentSummaryDto paymentSummary;
}
