package com.smartmess.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessDto {

    private UUID id;
    private String name;
    private String location;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal dailyPrice;
    private BigDecimal weeklyPrice;
    private BigDecimal rating;
    private boolean isActive;
    private boolean isApproved;
    private String imageUrl;
    private UUID ownerId;
    private String ownerName;
    private int totalStudents;
    private LocalDateTime createdAt;
}
