package com.smartmess.dto;

import com.smartmess.enums.MealType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateBookingRequest {

    @NotNull(message = "Menu item ID is required")
    private UUID menuItemId;

    @NotNull(message = "Meal type is required")
    private MealType mealType;

    @NotNull(message = "Date is required")
    private LocalDate date;
}
