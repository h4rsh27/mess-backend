package com.smartmess.dto;

import com.smartmess.enums.ApprovalStatus;
import com.smartmess.enums.PlanType;
import lombok.Builder;
import lombok.Data;


import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StudentDashboardDto {

    private UUID userId;
    private String name;
    private String email;
    private PlanType planType;
    private ApprovalStatus approvalStatus;
    private MessDto currentMess;
    private List<MenuItemDto> todayMenu;
    private long mealsThisMonth;
    private PaymentDto currentBill;
    private long unreadNotifications;
    private List<ComplaintDto> recentComplaints;
}
