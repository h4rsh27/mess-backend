package com.smartmess.entity;

import com.smartmess.enums.MealType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "qr_tokens", indexes = {
    @Index(name = "idx_qr_token", columnList = "token"),
    @Index(name = "idx_qr_mess_date", columnList = "mess_id, date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mess_id", nullable = false)
    private UUID messId;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @Column(nullable = false, unique = true)
    private String token; // Secure random UUID string used in QR payload

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "refresh_interval_minutes")
    @Builder.Default
    private Integer refreshIntervalMinutes = 2;

    @Column(length = 64)
    private String nonce;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true; // Set false to invalidate/rotate
}
