package com.smartmess.dto;

import com.smartmess.enums.MealType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MenuItemDto {

    private UUID id;
    private UUID messId;
    private String name;
    private MealType mealType;
    private LocalDate date;
    private Integer dayOfWeek;
    private boolean isAvailable;
    private Integer preparedQuantity;
    private String description;
    private Double avgRating;
    private int totalFeedbacks;
    private LocalDateTime createdAt;
}
