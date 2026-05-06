package com.smartmess.dto;

import com.smartmess.enums.AttendanceMethod;
import com.smartmess.enums.MealType;
import com.smartmess.enums.PlanType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AttendanceDto {

    private UUID id;
    private UUID userId;
    private String userName;
    private UUID messId;
    private UUID menuItemId;
    private String menuItemName;
    private LocalDate date;
    private MealType mealType;
    private AttendanceMethod markedVia;
    private PlanType planType;
    private String receiptCode;
    private String consumptionStatus;
    private boolean feedbackPending;
    private LocalDateTime markedAt;
}
