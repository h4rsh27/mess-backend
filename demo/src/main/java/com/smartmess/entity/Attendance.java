package com.smartmess.entity;

import com.smartmess.enums.AttendanceMethod;
import com.smartmess.enums.MealType;
import com.smartmess.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance", uniqueConstraints = {
    @UniqueConstraint(name = "uq_attendance_user_item_date",
        columnNames = {"user_id", "menu_item_id", "date"})
}, indexes = {
    @Index(name = "idx_attendance_user_date", columnList = "user_id, date"),
    @Index(name = "idx_attendance_mess_date", columnList = "mess_id, date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mess_id", nullable = false)
    private UUID messId;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AttendanceMethod markedVia = AttendanceMethod.CLICK;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type")
    private PlanType planType;

    @Column(name = "receipt_code", length = 64)
    private String receiptCode;

    @CreationTimestamp
    @Column(name = "marked_at", updatable = false)
    private LocalDateTime markedAt;
}
