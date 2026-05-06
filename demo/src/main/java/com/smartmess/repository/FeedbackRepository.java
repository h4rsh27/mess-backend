package com.smartmess.repository;

import com.smartmess.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByMenuItemId(UUID menuItemId);

    List<Feedback> findByMessIdOrderByCreatedAtDesc(UUID messId);

    List<Feedback> findByUserId(UUID userId);

    Optional<Feedback> findByAttendanceId(UUID attendanceId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.menuItemId = :menuItemId")
    Double avgRatingByMenuItemId(UUID menuItemId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.messId = :messId")
    Double avgRatingByMessId(UUID messId);

    @Query("SELECT f.menuItemId, AVG(f.rating), COUNT(f) FROM Feedback f WHERE f.messId = :messId AND f.menuItemId IS NOT NULL GROUP BY f.menuItemId ORDER BY AVG(f.rating) DESC")
    List<Object[]> getRankedMenuItems(UUID messId);

    long countByMenuItemId(UUID menuItemId);
}
