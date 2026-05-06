package com.smartmess.dto;

import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.MealType;
import com.smartmess.enums.PlanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingDto {

    private UUID id;
    private UUID userId;
    private UUID menuItemId;
    private String menuItemName;
    private UUID messId;
    private LocalDate date;
    private MealType mealType;
    private PlanType planType;
    private BookingStatus status;
    private BigDecimal amountCharged;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime consumedAt;
    private LocalDateTime noShowAt;
}
