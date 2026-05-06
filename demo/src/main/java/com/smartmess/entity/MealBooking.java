package com.smartmess.entity;

import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.MealType;
import com.smartmess.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meal_bookings", uniqueConstraints = {
    @UniqueConstraint(name = "uq_booking_user_item_date",
        columnNames = {"user_id", "menu_item_id", "date"})
}, indexes = {
    @Index(name = "idx_booking_user_date", columnList = "user_id, date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(name = "mess_id", nullable = false)
    private UUID messId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type")
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.BOOKED;

    @Column(name = "amount_charged", precision = 10, scale = 2)
    private java.math.BigDecimal amountCharged;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "no_show_at")
    private LocalDateTime noShowAt;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
