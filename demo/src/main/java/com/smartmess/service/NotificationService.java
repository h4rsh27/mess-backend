package com.smartmess.service;

import com.smartmess.dto.NotificationDto;
import com.smartmess.entity.Notification;
import com.smartmess.enums.ComplaintStatus;
import com.smartmess.enums.NotificationType;
import com.smartmess.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@smartmess.com}")
    private String mailFrom;

    @Transactional
    public void sendNotification(UUID userId, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        log.debug("In-app notification sent to user {}: {}", userId, title);
    }

    public void sendEmail(String toEmail, String subject, String body) {
        if (!mailEnabled) {
            log.debug("Email disabled. Would send to {}: {}", toEmail, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendComplaintUpdateNotification(UUID userId, String complaintTitle, ComplaintStatus status) {
        String title = "Complaint Update";
        String message = String.format("Your complaint '%s' has been updated to: %s", complaintTitle, status.name());
        sendNotification(userId, title, message, NotificationType.COMPLAINT_UPDATE);
    }

    public void sendPaymentDueNotification(UUID userId, String messName, int month, int year) {
        String title = "Payment Due";
        String message = String.format("Your mess fee for %s - %d/%d is due. Please pay promptly.", messName, month, year);
        sendNotification(userId, title, message, NotificationType.PAYMENT_DUE);
    }

    public void sendMenuUpdateNotification(UUID userId, String messName) {
        sendNotification(userId, "Menu Updated",
                "The menu for " + messName + " has been updated. Check today's menu!", NotificationType.MENU_UPDATE);
    }

    public List<NotificationDto> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType()).isRead(n.isRead()).createdAt(n.getCreatedAt())
                .build();
    }
}
