package com.smartmess.service;

import com.smartmess.enums.MealType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Service
public class MealScheduleService {

    private final Map<MealType, LocalTime> mealStarts = Map.of(
            MealType.BREAKFAST, LocalTime.of(7, 0),
            MealType.LUNCH, LocalTime.of(13, 0),
            MealType.SNACKS, LocalTime.of(17, 0),
            MealType.DINNER, LocalTime.of(20, 0)
    );

    private final Map<MealType, LocalTime> mealEnds = Map.of(
            MealType.BREAKFAST, LocalTime.of(10, 0),
            MealType.LUNCH, LocalTime.of(15, 0),
            MealType.SNACKS, LocalTime.of(18, 0),
            MealType.DINNER, LocalTime.of(22, 0)
    );

    private final int cutoffMinutes;
    private final int graceMinutes;

    public MealScheduleService(
            @Value("${app.meal.cutoff-minutes:120}") int cutoffMinutes,
            @Value("${app.meal.grace-minutes:20}") int graceMinutes) {
        this.cutoffMinutes = cutoffMinutes;
        this.graceMinutes = graceMinutes;
    }

    public LocalDateTime getBookingCutoff(LocalDate date, MealType mealType) {
        return getMealStart(date, mealType).minusMinutes(cutoffMinutes);
    }

    public LocalDateTime getMealStart(LocalDate date, MealType mealType) {
        return date.atTime(mealStarts.getOrDefault(mealType, LocalTime.NOON));
    }

    public LocalDateTime getMealEnd(LocalDate date, MealType mealType) {
        return date.atTime(mealEnds.getOrDefault(mealType, LocalTime.of(14, 0)));
    }

    public LocalDateTime getScanWindowStart(LocalDate date, MealType mealType) {
        return getMealStart(date, mealType);
    }

    public LocalDateTime getScanWindowEnd(LocalDate date, MealType mealType) {
        return getMealEnd(date, mealType).plusMinutes(graceMinutes);
    }

    public boolean isWithinScanWindow(LocalDateTime dateTime, LocalDate date, MealType mealType) {
        return !dateTime.isBefore(getScanWindowStart(date, mealType))
                && !dateTime.isAfter(getScanWindowEnd(date, mealType));
    }

    public int secondsUntil(LocalDateTime target) {
        return (int) Math.max(0, Duration.between(LocalDateTime.now(), target).getSeconds());
    }

    public int getGraceMinutes() {
        return graceMinutes;
    }

    public MealType resolveCurrentMealType(LocalDateTime now) {
        for (MealType mealType : MealType.values()) {
            if (isWithinScanWindow(now, now.toLocalDate(), mealType)) {
                return mealType;
            }
        }
        int hour = now.getHour();
        if (hour < 11) {
            return MealType.BREAKFAST;
        }
        if (hour < 16) {
            return MealType.LUNCH;
        }
        if (hour < 19) {
            return MealType.SNACKS;
        }
        return MealType.DINNER;
    }
}
