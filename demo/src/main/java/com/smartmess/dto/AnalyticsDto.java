package com.smartmess.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
@Builder
public class AnalyticsDto {

    // Popular / Least-liked food
    private List<FoodRankDto> topFoods;
    private List<FoodRankDto> bottomFoods;

    // Attendance trends
    private List<DailyAttendanceDto> attendanceTrend;

    // Revenue data
    private List<MonthlyRevenueDto> revenueGraph;

    // User split
    private long monthlyUsers;
    private long dailyUsers;
    private long weeklyUsers;

    // Today's overview
    private long todayAttendance;
    private long totalStudents;

    // Wastage
    private List<WastageDto> wastageData;
    private CurrentMealStatsDto currentMealStats;
    private List<MealBreakdownDto> mealBreakdown;
    private List<OwnerAlertDto> alerts;

    @Data
    @Builder
    public static class FoodRankDto {
        private String itemName;
        private double avgRating;
        private int totalFeedbacks;
        private int attendanceCount;
    }

    @Data
    @Builder
    public static class DailyAttendanceDto {
        private String date;
        private long count;
    }

    @Data
    @Builder
    public static class MonthlyRevenueDto {
        private String month; // e.g. "Jan 2025"
        private BigDecimal revenue;
        private long paidStudents;
    }

    @Data
    @Builder
    public static class WastageDto {
        private String itemName;
        private String date;
        private Integer preparedQty;
        private long actualConsumed;
        private int estimatedWastage;
    }

    @Data
    @Builder
    public static class CurrentMealStatsDto {
        private String mealName;
        private String mealType;
        private long dailyBookings;
        private long monthlyActive;
        private long monthlyCancelled;
        private long expectedCount;
        private long actualScanned;
        private long noShows;
        private long predictedCount;
        private String qrValidUntil;
        private long foodWaste;
    }

    @Data
    @Builder
    public static class MealBreakdownDto {
        private String mealName;
        private String mealType;
        private String date;
        private long dailyBookings;
        private long monthlyActive;
        private long monthlyCancellations;
        private long expectedCount;
        private long actualScanned;
        private long noShows;
        private long foodWaste;
        private long predictedCount;
    }

    @Data
    @Builder
    public static class OwnerAlertDto {
        private String type;
        private String title;
        private String message;
        private String severity;
    }
}
