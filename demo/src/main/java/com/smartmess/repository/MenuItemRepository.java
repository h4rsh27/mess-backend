package com.smartmess.repository;

import com.smartmess.entity.MenuItem;
import com.smartmess.enums.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByMessIdAndDate(UUID messId, LocalDate date);

    List<MenuItem> findByMessIdAndDayOfWeek(UUID messId, Integer dayOfWeek);

    List<MenuItem> findByMessId(UUID messId);

    @Query("SELECT m FROM MenuItem m WHERE m.messId = :messId AND (m.date = :date OR m.dayOfWeek = :dayOfWeek)")
    List<MenuItem> findTodayMenu(UUID messId, LocalDate date, Integer dayOfWeek);

    List<MenuItem> findByMessIdAndMealType(UUID messId, MealType mealType);

    List<MenuItem> findByMessIdAndDateAndMealType(UUID messId, LocalDate date, MealType mealType);
}
