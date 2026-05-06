package com.smartmess.dto;

import com.smartmess.enums.MealType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMenuItemRequest {

    @NotBlank(message = "Item name is required")
    private String name;

    @NotNull(message = "Meal type is required")
    private MealType mealType;

    // For specific-date items
    private LocalDate date;

    // For recurring weekly items (1=Mon, 7=Sun)
    private Integer dayOfWeek;

    private Integer preparedQuantity;

    private String description;
}
