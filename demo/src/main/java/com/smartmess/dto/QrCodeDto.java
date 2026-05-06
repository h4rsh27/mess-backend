package com.smartmess.dto;

import com.smartmess.enums.MealType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class QrCodeDto {
    private UUID menuItemId;
    private MealType mealType;
    private String qrImage;
    private LocalDateTime issuedAt;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer refreshIntervalMinutes;
}
