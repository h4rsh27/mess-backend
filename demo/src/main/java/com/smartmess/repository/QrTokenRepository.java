package com.smartmess.repository;

import com.smartmess.entity.QrToken;
import com.smartmess.enums.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface QrTokenRepository extends JpaRepository<QrToken, UUID> {

    Optional<QrToken> findByTokenAndIsActiveTrue(String token);

    Optional<QrToken> findByMenuItemIdAndDateAndMealTypeAndIsActiveTrue(
            UUID menuItemId, LocalDate date, MealType mealType);

    @Modifying
    @Query("UPDATE QrToken q SET q.isActive = false WHERE q.menuItemId = :menuItemId AND q.date = :date AND q.mealType = :mealType AND q.isActive = true")
    int deactivateActiveTokens(UUID menuItemId, LocalDate date, MealType mealType);

    void deleteByMessIdAndDateBefore(UUID messId, LocalDate cutoffDate);
}
