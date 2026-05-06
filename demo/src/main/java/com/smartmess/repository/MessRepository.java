package com.smartmess.repository;

import com.smartmess.entity.Mess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessRepository extends JpaRepository<Mess, UUID> {

    Optional<Mess> findByOwnerId(UUID ownerId);

    List<Mess> findByIsActiveTrue();

    List<Mess> findByIsActiveTrueAndIsApprovedTrue();

    @Query("SELECT m FROM Mess m WHERE m.isActive = true ORDER BY m.rating DESC")
    List<Mess> findAllActiveOrderByRating();
}
