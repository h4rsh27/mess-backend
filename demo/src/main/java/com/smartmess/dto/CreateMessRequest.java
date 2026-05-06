package com.smartmess.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateMessRequest {

    @NotBlank(message = "Mess name is required")
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    private String description;

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly price must be positive")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Daily price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Daily price must be positive")
    private BigDecimal dailyPrice;

    private BigDecimal weeklyPrice;

    private String imageUrl;
}
