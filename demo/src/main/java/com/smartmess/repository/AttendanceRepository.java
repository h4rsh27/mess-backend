package com.smartmess.repository;

import com.smartmess.entity.Attendance;
import com.smartmess.enums.PlanType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    boolean existsByUserIdAndMenuItemIdAndDate(UUID userId, UUID menuItemId, LocalDate date);

    List<Attendance> findByUserIdOrderByDateDesc(UUID userId);

    List<Attendance> findByUserIdAndDateBetweenOrderByDateDesc(UUID userId, LocalDate from, LocalDate to);

    List<Attendance> findByUserIdAndDateOrderByMarkedAtDesc(UUID userId, LocalDate date);

    long countByMessIdAndDate(UUID messId, LocalDate date);

    long countByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);

    List<Attendance> findByMessIdAndDate(UUID messId, LocalDate date);

    List<Attendance> findByMessIdAndDateBetweenOrderByDateDesc(UUID messId, LocalDate from, LocalDate to);

    @Query("SELECT a.date, COUNT(a) FROM Attendance a WHERE a.messId = :messId AND a.date BETWEEN :from AND :to GROUP BY a.date ORDER BY a.date")
    List<Object[]> countDailyAttendance(UUID messId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.menuItemId = :menuItemId AND a.date = :date")
    long countByMenuItemIdAndDate(UUID menuItemId, LocalDate date);

    boolean existsByUserIdAndMenuItemIdAndDateAndPlanType(UUID userId, UUID menuItemId, LocalDate date, PlanType planType);
}
