package com.smartmess.dto;

import com.smartmess.enums.AttendanceMethod;
import com.smartmess.enums.MealType;
import lombok.Data;

import java.util.UUID;

@Data
public class MarkAttendanceRequest {

    private UUID menuItemId;

    private MealType mealType;

    private AttendanceMethod method;

    // For QR-based attendance — the token extracted from QR scan
    private String qrToken;
}
