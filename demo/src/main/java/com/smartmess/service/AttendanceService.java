package com.smartmess.service;

import com.smartmess.dto.AttendanceDto;
import com.smartmess.dto.MarkAttendanceRequest;
import com.smartmess.entity.Attendance;
import com.smartmess.entity.MealBooking;
import com.smartmess.entity.MenuItem;
import com.smartmess.entity.User;
import com.smartmess.enums.ApprovalStatus;
import com.smartmess.enums.AttendanceMethod;
import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.PlanType;
import com.smartmess.exception.DuplicateAttendanceException;
import com.smartmess.repository.AttendanceRepository;
import com.smartmess.repository.FeedbackRepository;
import com.smartmess.repository.MealBookingRepository;
import com.smartmess.repository.MealCancellationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final MealBookingRepository bookingRepository;
    private final MealCancellationRepository cancellationRepository;
    private final UserService userService;
    private final MenuService menuService;
    private final QrService qrService;

    @Transactional
    public AttendanceDto markAttendance(UUID userId, MarkAttendanceRequest request) {
        User user = userService.findById(userId);
        validateMembership(user);

        AttendanceMethod method = request.getMethod() != null ? request.getMethod() : AttendanceMethod.QR;
        if (method != AttendanceMethod.QR) {
            throw new IllegalStateException("Attendance must be marked by scanning the live meal QR.");
        }

        QrService.ValidatedQrToken validatedQr = qrService.validateQrToken(request.getQrToken(), request.getMenuItemId());
        UUID menuItemId = validatedQr.menuItemId();
        LocalDate attendanceDate = validatedQr.date();
        MenuItem item = menuService.findById(menuItemId);

        if (!item.getMessId().equals(user.getMessId())) {
            throw new SecurityException("This QR belongs to a different mess.");
        }
        if (attendanceRepository.existsByUserIdAndMenuItemIdAndDate(userId, menuItemId, attendanceDate)) {
            throw new DuplicateAttendanceException("Attendance already marked for this meal.");
        }

        MealBooking booking = bookingRepository.findByUserIdAndMenuItemIdAndDate(userId, menuItemId, attendanceDate).orElse(null);
        if (user.getPlanType() == PlanType.DAILY) {
            if (booking == null || booking.getStatus() != BookingStatus.BOOKED) {
                throw new IllegalStateException("Daily users must book first and then scan the active QR to consume the meal.");
            }
        } else if (cancellationRepository.findByUserIdAndMenuItemIdAndDate(userId, menuItemId, attendanceDate).isPresent()
                || (booking != null && booking.getStatus() == BookingStatus.CANCELLED)) {
            throw new IllegalStateException("You have cancelled this meal and cannot scan it now.");
        }

        Attendance attendance = attendanceRepository.save(Attendance.builder()
                .userId(userId)
                .messId(user.getMessId())
                .menuItemId(menuItemId)
                .date(attendanceDate)
                .mealType(validatedQr.mealType())
                .markedVia(AttendanceMethod.QR)
                .planType(user.getPlanType())
                .receiptCode(buildReceiptCode(validatedQr.mealType()))
                .build());

        if (booking != null) {
            booking.setStatus(BookingStatus.CONSUMED);
            booking.setConsumedAt(LocalDateTime.now());
            booking.setNotes("Consumed after QR scan.");
            bookingRepository.save(booking);
        }

        log.info("Attendance consumed: user={} menuItem={} plan={} receipt={}",
                userId, menuItemId, user.getPlanType(), attendance.getReceiptCode());
        return toDto(attendance, user.getName(), item.getName());
    }

    @Transactional
    public List<AttendanceDto> syncOfflineAttendance(UUID userId, List<MarkAttendanceRequest> requests) {
        return requests.stream()
                .map(request -> {
                    try {
                        return markAttendance(userId, request);
                    } catch (DuplicateAttendanceException ex) {
                        log.info("Skipping duplicate offline attendance sync for user {} token {}", userId, request.getQrToken());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<AttendanceDto> getMyHistory(UUID userId) {
        User user = userService.findById(userId);
        return attendanceRepository.findByUserIdOrderByDateDesc(userId)
                .stream()
                .map(a -> toDto(a, user.getName(), menuService.findById(a.getMenuItemId()).getName()))
                .toList();
    }

    public List<AttendanceDto> getMonthlyReport(UUID messId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepository.findByMessIdAndDateBetweenOrderByDateDesc(messId, from, to)
                .stream()
                .map(a -> {
                    User user = userService.findById(a.getUserId());
                    return toDto(a, user.getName(), menuService.findById(a.getMenuItemId()).getName());
                })
                .toList();
    }

    private void validateMembership(User user) {
        if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Your mess membership is not yet approved.");
        }
        if (user.getMessId() == null) {
            throw new IllegalStateException("Join a mess before scanning meals.");
        }
    }

    private String buildReceiptCode(com.smartmess.enums.MealType mealType) {
        return "SM-" + mealType.name().substring(0, 1) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private AttendanceDto toDto(Attendance attendance, String userName, String itemName) {
        return AttendanceDto.builder()
                .id(attendance.getId())
                .userId(attendance.getUserId())
                .userName(userName)
                .messId(attendance.getMessId())
                .menuItemId(attendance.getMenuItemId())
                .menuItemName(itemName)
                .date(attendance.getDate())
                .mealType(attendance.getMealType())
                .markedVia(attendance.getMarkedVia())
                .planType(attendance.getPlanType())
                .receiptCode(attendance.getReceiptCode())
                .consumptionStatus("CONSUMED")
                .feedbackPending(feedbackRepository.findByAttendanceId(attendance.getId()).isEmpty())
                .markedAt(attendance.getMarkedAt())
                .build();
    }
}
