package com.smartmess.repository;

import com.smartmess.entity.MealBooking;
import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealBookingRepository extends JpaRepository<MealBooking, UUID> {

    List<MealBooking> findByUserIdAndDate(UUID userId, LocalDate date);

    List<MealBooking> findByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);

    Optional<MealBooking> findByUserIdAndMenuItemIdAndDate(UUID userId, UUID menuItemId, LocalDate date);

    List<MealBooking> findByMenuItemIdAndDate(UUID menuItemId, LocalDate date);

    List<MealBooking> findByDateAndStatus(LocalDate date, BookingStatus status);

    long countByMenuItemIdAndStatusNot(UUID menuItemId, BookingStatus status);
    
    long countByMenuItemIdAndStatus(UUID menuItemId, BookingStatus status);

    boolean existsByUserIdAndMenuItemIdAndDate(UUID userId, UUID menuItemId, LocalDate date);

    @Query("SELECT COUNT(b) FROM MealBooking b WHERE b.menuItemId = :menuItemId AND b.date = :date AND b.planType = :planType AND b.status IN :statuses")
    long countByMenuItemIdAndDateAndPlanTypeAndStatuses(UUID menuItemId, LocalDate date, PlanType planType, List<BookingStatus> statuses);

    @Query("SELECT COUNT(b) FROM MealBooking b WHERE b.menuItemId = :menuItemId AND b.date = :date AND b.planType = :planType AND b.status = :status")
    long countByMenuItemIdAndDateAndPlanTypeAndStatus(UUID menuItemId, LocalDate date, PlanType planType, BookingStatus status);

    @Query("SELECT b FROM MealBooking b WHERE b.status = 'BOOKED' AND b.date <= :date AND b.planType = 'DAILY'")
    List<MealBooking> findDailyBookingsPendingConsumption(LocalDate date);
}
