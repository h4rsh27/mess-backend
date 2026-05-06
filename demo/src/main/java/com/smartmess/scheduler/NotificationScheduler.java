package com.smartmess.scheduler;

import com.smartmess.entity.User;
import com.smartmess.enums.ApprovalStatus;
import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.PaymentStatus;
import com.smartmess.enums.PlanType;
import com.smartmess.enums.Role;
import com.smartmess.repository.MealBookingRepository;
import com.smartmess.repository.MessRepository;
import com.smartmess.repository.PaymentRepository;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.AnalyticsService;
import com.smartmess.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final MealBookingRepository mealBookingRepository;
    private final MessRepository messRepository;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;

    @Scheduled(cron = "0 0 9 25 * *")
    public void sendPaymentDueReminders() {
        log.info("Running payment due notification scheduler...");
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        messRepository.findByIsActiveTrue().forEach(mess -> {
            List<User> unpaidStudents = userRepository.findByMessIdAndRole(mess.getId(), Role.ROLE_STUDENT)
                    .stream()
                    .filter(user -> user.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .filter(user -> paymentRepository.findByUserIdAndMessIdAndMonthAndYear(
                                    user.getId(), mess.getId(), month, year)
                            .map(payment -> payment.getStatus() != PaymentStatus.PAID)
                            .orElse(user.getPlanType() != PlanType.DAILY))
                    .toList();

            unpaidStudents.forEach(student ->
                    notificationService.sendPaymentDueNotification(student.getId(), mess.getName(), month, year));
        });
    }

    @Scheduled(cron = "0 0 6,11,15,18 * * *")
    public void sendCutoffReminders() {
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ROLE_STUDENT)
                .filter(user -> user.getApprovalStatus() == ApprovalStatus.APPROVED)
                .filter(user -> user.getMessId() != null)
                .forEach(student -> {
                    String message = student.getPlanType() == PlanType.DAILY
                            ? "Book your upcoming meal before the cutoff, then scan the live QR at service time."
                            : "You can still cancel the upcoming meal before the cutoff if you will skip it.";
                    notificationService.sendNotification(
                            student.getId(),
                            "Upcoming meal reminder",
                            message,
                            com.smartmess.enums.NotificationType.MEAL_REMINDER
                    );
                });
    }

    @Scheduled(cron = "0 5 7,13,17,20 * * *")
    public void sendQrActiveNotifications() {
        userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ROLE_STUDENT)
                .filter(user -> user.getApprovalStatus() == ApprovalStatus.APPROVED)
                .filter(user -> user.getMessId() != null)
                .forEach(student -> notificationService.sendNotification(
                        student.getId(),
                        "Meal QR is now active",
                        "Your meal service window has started. Scan the live QR at the mess counter to consume your meal.",
                        com.smartmess.enums.NotificationType.QR_ACTIVE
                ));
    }

    @Scheduled(cron = "0 45 10,15,18,22 * * *")
    public void sendMissedMealAlerts() {
        mealBookingRepository.findByDateAndStatus(LocalDate.now(), BookingStatus.NO_SHOW)
                .forEach(booking -> notificationService.sendNotification(
                        booking.getUserId(),
                        "Missed meal recorded",
                        "Your booked meal was marked as a no-show because it was not scanned during the service window.",
                        com.smartmess.enums.NotificationType.MISSED_MEAL
                ));
    }

    @Scheduled(cron = "0 15 7,13,17,20 * * *")
    public void sendOwnerOperationalAlerts() {
        messRepository.findByIsActiveTrue().forEach(mess -> {
            var analytics = analyticsService.getOwnerAnalytics(mess.getId());
            if (analytics.getAlerts() == null || analytics.getAlerts().isEmpty()) {
                return;
            }
            analytics.getAlerts().forEach(alert -> notificationService.sendNotification(
                    mess.getOwnerId(),
                    alert.getTitle(),
                    alert.getMessage(),
                    "LOW_BOOKINGS".equals(alert.getType())
                            ? com.smartmess.enums.NotificationType.LOW_BOOKINGS
                            : com.smartmess.enums.NotificationType.HIGH_CANCELLATIONS
            ));
        });
    }
}
