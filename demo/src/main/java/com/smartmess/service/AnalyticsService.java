package com.smartmess.service;

import com.smartmess.dto.AnalyticsDto;
import com.smartmess.entity.MealBooking;
import com.smartmess.entity.MenuItem;
import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.MealType;
import com.smartmess.enums.PlanType;
import com.smartmess.enums.Role;
import com.smartmess.repository.AttendanceRepository;
import com.smartmess.repository.FeedbackRepository;
import com.smartmess.repository.MenuItemRepository;
import com.smartmess.repository.PaymentRepository;
import com.smartmess.repository.QrTokenRepository;
import com.smartmess.repository.UserRepository;
import com.smartmess.repository.MealBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final PaymentRepository paymentRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final MealBookingRepository mealBookingRepository;
    private final QrTokenRepository qrTokenRepository;
    private final MealScheduleService mealScheduleService;

    public AnalyticsDto getOwnerAnalytics(UUID messId) {
        LocalDate today = LocalDate.now();

        List<Object[]> ranked = feedbackRepository.getRankedMenuItems(messId);
        List<AnalyticsDto.FoodRankDto> allRanked = ranked.stream()
                .filter(row -> row[0] != null)
                .map(row -> {
                    UUID itemId = (UUID) row[0];
                    double avgRating = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
                    long feedbackCount = ((Number) row[2]).longValue();
                    MenuItem item = menuItemRepository.findById(itemId).orElse(null);
                    long attendanceCount = item != null
                            ? attendanceRepository.countByMenuItemIdAndDate(itemId, item.getDate() != null ? item.getDate() : today)
                            : 0;
                    return AnalyticsDto.FoodRankDto.builder()
                            .itemName(item != null ? item.getName() : "Unknown")
                            .avgRating(avgRating)
                            .totalFeedbacks((int) feedbackCount)
                            .attendanceCount((int) attendanceCount)
                            .build();
                })
                .toList();

        int topN = Math.min(5, allRanked.size());
        List<AnalyticsDto.FoodRankDto> topFoods = allRanked.subList(0, topN);
        List<AnalyticsDto.FoodRankDto> bottomFoods = allRanked.size() > topN
                ? allRanked.subList(Math.max(0, allRanked.size() - topN), allRanked.size())
                : List.of();

        LocalDate trendFrom = today.minusDays(29);
        DateTimeFormatter trendFormat = DateTimeFormatter.ofPattern("MMM dd");
        List<AnalyticsDto.DailyAttendanceDto> trend = attendanceRepository.countDailyAttendance(messId, trendFrom, today)
                .stream()
                .map(row -> AnalyticsDto.DailyAttendanceDto.builder()
                        .date(((LocalDate) row[0]).format(trendFormat))
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        List<AnalyticsDto.MonthlyRevenueDto> revenueGraph = paymentRepository.getMonthlyRevenue(messId)
                .stream()
                .map(row -> {
                    int month = ((Number) row[0]).intValue();
                    int year = ((Number) row[1]).intValue();
                    BigDecimal revenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
                    return AnalyticsDto.MonthlyRevenueDto.builder()
                            .month(LocalDate.of(year, month, 1).format(DateTimeFormatter.ofPattern("MMM yyyy")))
                            .revenue(revenue)
                            .build();
                })
                .toList();

        List<AnalyticsDto.MealBreakdownDto> mealBreakdown = buildMealBreakdown(messId, today);
        AnalyticsDto.CurrentMealStatsDto currentMealStats = resolveCurrentMealStats(messId, mealBreakdown);
        List<AnalyticsDto.OwnerAlertDto> alerts = buildAlerts(mealBreakdown);

        List<AnalyticsDto.WastageDto> wastage = mealBreakdown.stream()
                .map(meal -> AnalyticsDto.WastageDto.builder()
                        .itemName(meal.getMealName())
                        .date(meal.getDate())
                        .preparedQty(menuItemRepository.findByMessIdAndDateAndMealType(messId, today, MealType.valueOf(meal.getMealType()))
                                .stream().findFirst().map(MenuItem::getPreparedQuantity).orElse(null))
                        .actualConsumed(meal.getActualScanned())
                        .estimatedWastage((int) meal.getFoodWaste())
                        .build())
                .toList();

        long totalStudents = userRepository.countByMessIdAndRole(messId, Role.ROLE_STUDENT);
        long todayAttendance = attendanceRepository.countByMessIdAndDate(messId, today);

        return AnalyticsDto.builder()
                .topFoods(topFoods)
                .bottomFoods(bottomFoods)
                .attendanceTrend(trend)
                .revenueGraph(revenueGraph)
                .todayAttendance(todayAttendance)
                .totalStudents(totalStudents)
                .wastageData(wastage)
                .currentMealStats(currentMealStats)
                .mealBreakdown(mealBreakdown)
                .alerts(alerts)
                .monthlyUsers(userRepository.countByMessIdAndRoleAndPlanType(messId, Role.ROLE_STUDENT, PlanType.MONTHLY))
                .dailyUsers(userRepository.countByMessIdAndRoleAndPlanType(messId, Role.ROLE_STUDENT, PlanType.DAILY))
                .weeklyUsers(userRepository.countByMessIdAndRoleAndPlanType(messId, Role.ROLE_STUDENT, PlanType.WEEKLY))
                .build();
    }

    private List<AnalyticsDto.MealBreakdownDto> buildMealBreakdown(UUID messId, LocalDate date) {
        long monthlyActive = userRepository.countByMessIdAndRoleAndPlanType(messId, Role.ROLE_STUDENT, PlanType.MONTHLY);
        List<AnalyticsDto.MealBreakdownDto> breakdown = new ArrayList<>();

        for (MenuItem item : menuItemRepository.findByMessIdAndDate(messId, date)) {
            List<MealBooking> bookings = mealBookingRepository.findByMenuItemIdAndDate(item.getId(), date);
            long dailyBookings = bookings.stream()
                    .filter(booking -> booking.getPlanType() == PlanType.DAILY)
                    .filter(booking -> booking.getStatus() == BookingStatus.BOOKED
                            || booking.getStatus() == BookingStatus.CONSUMED
                            || booking.getStatus() == BookingStatus.NO_SHOW)
                    .count();
            long monthlyCancellations = bookings.stream()
                    .filter(booking -> booking.getPlanType() != PlanType.DAILY)
                    .filter(booking -> booking.getStatus() == BookingStatus.CANCELLED)
                    .count();
            long noShows = bookings.stream()
                    .filter(booking -> booking.getStatus() == BookingStatus.NO_SHOW)
                    .count();
            long actualScanned = attendanceRepository.countByMenuItemIdAndDate(item.getId(), date);
            long expectedCount = dailyBookings + Math.max(0, monthlyActive - monthlyCancellations);
            long predictedCount = predictExpectedCount(messId, item);
            long foodWaste = Math.max(0, expectedCount - actualScanned);

            breakdown.add(AnalyticsDto.MealBreakdownDto.builder()
                    .mealName(item.getName())
                    .mealType(item.getMealType().name())
                    .date(date.toString())
                    .dailyBookings(dailyBookings)
                    .monthlyActive(monthlyActive)
                    .monthlyCancellations(monthlyCancellations)
                    .expectedCount(expectedCount)
                    .actualScanned(actualScanned)
                    .noShows(noShows)
                    .foodWaste(foodWaste)
                    .predictedCount(predictedCount)
                    .build());
        }

        return breakdown.stream()
                .sorted(Comparator.comparing(AnalyticsDto.MealBreakdownDto::getMealType))
                .toList();
    }

    private AnalyticsDto.CurrentMealStatsDto resolveCurrentMealStats(UUID messId, List<AnalyticsDto.MealBreakdownDto> mealBreakdown) {
        MealType currentMeal = mealScheduleService.resolveCurrentMealType(LocalDateTime.now());
        MenuItem currentItem = menuItemRepository.findByMessIdAndDateAndMealType(messId, LocalDate.now(), currentMeal)
                .stream()
                .findFirst()
                .orElse(null);
        return mealBreakdown.stream()
                .filter(meal -> meal.getMealType().equals(currentMeal.name()))
                .findFirst()
                .map(meal -> AnalyticsDto.CurrentMealStatsDto.builder()
                        .mealName(meal.getMealName())
                        .mealType(meal.getMealType())
                        .dailyBookings(meal.getDailyBookings())
                        .monthlyActive(meal.getMonthlyActive())
                        .monthlyCancelled(meal.getMonthlyCancellations())
                        .expectedCount(meal.getExpectedCount())
                        .actualScanned(meal.getActualScanned())
                        .noShows(meal.getNoShows())
                        .predictedCount(meal.getPredictedCount())
                        .foodWaste(meal.getFoodWaste())
                        .qrValidUntil(currentItem == null ? null : qrTokenRepository.findByMenuItemIdAndDateAndMealTypeAndIsActiveTrue(
                                        currentItem.getId(), LocalDate.now(), currentMeal)
                                .map(token -> token.getValidUntil().toString())
                                .orElse(null))
                        .build())
                .orElse(null);
    }

    private List<AnalyticsDto.OwnerAlertDto> buildAlerts(List<AnalyticsDto.MealBreakdownDto> mealBreakdown) {
        List<AnalyticsDto.OwnerAlertDto> alerts = new ArrayList<>();
        for (AnalyticsDto.MealBreakdownDto meal : mealBreakdown) {
            if (meal.getDailyBookings() < 5) {
                alerts.add(AnalyticsDto.OwnerAlertDto.builder()
                        .type("LOW_BOOKINGS")
                        .title("Low bookings detected")
                        .message(meal.getMealType() + " has only " + meal.getDailyBookings() + " daily bookings so far.")
                        .severity("medium")
                        .build());
            }
            if (meal.getMonthlyCancellations() > 0 && meal.getMonthlyCancellations() >= Math.max(3, meal.getMonthlyActive() / 4)) {
                alerts.add(AnalyticsDto.OwnerAlertDto.builder()
                        .type("HIGH_CANCELLATIONS")
                        .title("High cancellations")
                        .message(meal.getMealType() + " has " + meal.getMonthlyCancellations() + " subscription cancellations today.")
                        .severity("high")
                        .build());
            }
        }
        return alerts;
    }

    private long predictExpectedCount(UUID messId, MenuItem item) {
        List<MenuItem> history = menuItemRepository.findByMessIdAndMealType(messId, item.getMealType()).stream()
                .filter(menuItem -> menuItem.getDate() != null && menuItem.getDate().isBefore(LocalDate.now()))
                .filter(menuItem -> menuItem.getDate().getDayOfWeek().equals(LocalDate.now().getDayOfWeek()))
                .sorted(Comparator.comparing(MenuItem::getDate).reversed())
                .limit(4)
                .toList();

        if (history.isEmpty()) {
            return item.getPreparedQuantity() != null ? item.getPreparedQuantity() : 0;
        }

        return Math.round((float) history.stream()
                .mapToLong(menuItem -> attendanceRepository.countByMenuItemIdAndDate(menuItem.getId(), menuItem.getDate()))
                .average()
                .orElse(0));
    }
}
