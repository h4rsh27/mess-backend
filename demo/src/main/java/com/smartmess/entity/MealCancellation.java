package com.smartmess.entity;

import com.smartmess.enums.MealType;
import com.smartmess.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meal_cancellations", uniqueConstraints = {
        @UniqueConstraint(name = "uq_meal_cancel_user_item_date", columnNames = {"user_id", "menu_item_id", "date"})
}, indexes = {
        @Index(name = "idx_meal_cancel_mess_date", columnList = "mess_id, date"),
        @Index(name = "idx_meal_cancel_user_date", columnList = "user_id, date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealCancellation {

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

    @Column(length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "cancelled_at", updatable = false)
    private LocalDateTime cancelledAt;
}
