package com.smartmess.dto;

import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.MealType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TodayMealStatusDto {
    private UUID menuItemId;
    private String menuItemName;
    private MealType mealType;
    private BookingStatus bookingStatus;
    private boolean consumed;
    private boolean canBook;
    private boolean canCancel;
    private boolean canScan;
    private boolean feedbackPending;
    private String planType;
    private String statusLabel;
    private LocalDateTime cutoffAt;
    private LocalDateTime qrActiveFrom;
    private LocalDateTime qrActiveUntil;
    private Integer countdownSeconds;
}
