package com.smartmess.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback", indexes = {
    @Index(name = "idx_feedback_menu_item", columnList = "menu_item_id"),
    @Index(name = "idx_feedback_mess", columnList = "mess_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "menu_item_id")
    private UUID menuItemId; // Can be null for mess-level feedback

    @Column(name = "attendance_id", unique = true)
    private UUID attendanceId;

    @Column(name = "mess_id", nullable = false)
    private UUID messId;

    @Column(nullable = false)
    private Integer rating; // 1–5

    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
