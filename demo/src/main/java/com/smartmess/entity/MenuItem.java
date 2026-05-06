package com.smartmess.entity;

import com.smartmess.enums.MealType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "menu_items", indexes = {
    @Index(name = "idx_menu_mess_date", columnList = "mess_id, date"),
    @Index(name = "idx_menu_mess_day", columnList = "mess_id, day_of_week")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mess_id", nullable = false)
    private UUID messId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    // For specific-date menu items (daily menu)
    private LocalDate date;

    // For recurring weekly menu (1=Monday, 7=Sunday per ISO)
    private Integer dayOfWeek;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    // Estimated quantity to be prepared (for wastage tracking)
    private Integer preparedQuantity;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
