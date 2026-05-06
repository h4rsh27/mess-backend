package com.smartmess.repository;

import com.smartmess.entity.MealCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealCancellationRepository extends JpaRepository<MealCancellation, UUID> {

    Optional<MealCancellation> findByUserIdAndMenuItemIdAndDate(UUID userId, UUID menuItemId, LocalDate date);

    List<MealCancellation> findByUserIdAndDate(UUID userId, LocalDate date);

    List<MealCancellation> findByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);

    long countByMenuItemIdAndDate(UUID menuItemId, LocalDate date);
}
