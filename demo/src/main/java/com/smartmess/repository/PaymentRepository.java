package com.smartmess.repository;

import com.smartmess.entity.Payment;
import com.smartmess.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByUserIdAndMessIdAndMonthAndYear(UUID userId, UUID messId, int month, int year);

    List<Payment> findByMessIdAndMonthAndYear(UUID messId, int month, int year);

    List<Payment> findByUserIdOrderByYearDescMonthDesc(UUID userId);

    List<Payment> findByMessIdAndStatus(UUID messId, PaymentStatus status);

    long countByMessIdAndMonthAndYearAndStatus(UUID messId, int month, int year, PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.messId = :messId AND p.month = :month AND p.year = :year AND p.status = 'PAID'")
    Optional<BigDecimal> sumRevenueByMessAndMonth(UUID messId, int month, int year);

    @Query("SELECT p.month, p.year, SUM(p.amount) FROM Payment p WHERE p.messId = :messId AND p.status = 'PAID' GROUP BY p.year, p.month ORDER BY p.year, p.month")
    List<Object[]> getMonthlyRevenue(UUID messId);
}
