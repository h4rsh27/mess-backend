package com.smartmess.service;

import com.smartmess.dto.BookingDto;
import com.smartmess.dto.CreateBookingRequest;
import com.smartmess.dto.TodayMealStatusDto;
import com.smartmess.entity.Attendance;
import com.smartmess.entity.MealBooking;
import com.smartmess.entity.MealCancellation;
import com.smartmess.entity.MenuItem;
import com.smartmess.entity.User;
import com.smartmess.enums.BookingStatus;
import com.smartmess.enums.PlanType;
import com.smartmess.exception.BookingDeadlineException;
import com.smartmess.exception.ResourceNotFoundException;
import com.smartmess.repository.AttendanceRepository;
import com.smartmess.repository.FeedbackRepository;
import com.smartmess.repository.MealBookingRepository;
import com.smartmess.repository.MealCancellationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final MealBookingRepository bookingRepository;
    private final MealCancellationRepository cancellationRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserService userService;
    private final MenuService menuService;
    private final MessService messService;
    private final MealScheduleService mealScheduleService;
    private final WalletService walletService;

    @Transactional
    public BookingDto bookMeal(UUID userId, CreateBookingRequest request) {
        User user = validateEligibleStudent(userId);
        MenuItem item = validateMenuOwnership(user, request.getMenuItemId());
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        LocalDateTime cutoff = mealScheduleService.getBookingCutoff(date, item.getMealType());
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new BookingDeadlineException("Booking deadline passed. You cannot book or update this meal anymore.");
        }

        MealBooking booking = bookingRepository.findByUserIdAndMenuItemIdAndDate(userId, item.getId(), date)
                .orElse(MealBooking.builder()
                        .userId(userId)
                        .menuItemId(item.getId())
                        .messId(user.getMessId())
                        .date(date)
                        .mealType(item.getMealType())
                        .planType(user.getPlanType())
                        .build());

        if (user.getPlanType() == PlanType.DAILY) {
            if (booking.getId() != null && booking.getStatus() == BookingStatus.CONSUMED) {
                throw new IllegalStateException("This booked meal has already been consumed.");
            }
            if (booking.getId() != null && booking.getStatus() == BookingStatus.BOOKED) {
                throw new IllegalStateException("You have already booked this meal.");
            }

            BigDecimal dailyPrice = messService.findById(user.getMessId()).getDailyPrice();
            if (booking.getId() == null) {
                booking = bookingRepository.save(booking);
            }
            walletService.debit(userId, dailyPrice, booking.getId(), "Meal booking for " + item.getName());
            booking.setStatus(BookingStatus.BOOKED);
            booking.setAmountCharged(dailyPrice);
            booking.setCancelledAt(null);
            booking.setConsumedAt(null);
            booking.setNoShowAt(null);
            booking.setNotes("Wallet debited on booking.");
        } else {
            cancellationRepository.findByUserIdAndMenuItemIdAndDate(userId, item.getId(), date)
                    .ifPresent(cancellationRepository::delete);
            booking.setStatus(BookingStatus.BOOKED);
            booking.setAmountCharged(BigDecimal.ZERO);
            booking.setCancelledAt(null);
            booking.setConsumedAt(null);
            booking.setNoShowAt(null);
            booking.setNotes("Optional monthly/weekly opt-in restored.");
        }

        booking.setPlanType(user.getPlanType());
        booking = bookingRepository.save(booking);
        log.info("Meal booking updated: user={} item={} status={} plan={}", userId, item.getId(), booking.getStatus(), user.getPlanType());
        return toDto(booking, item.getName());
    }

    @Transactional
    public void cancelMeal(UUID userId, UUID menuItemId, LocalDate date) {
        User user = validateEligibleStudent(userId);
        MenuItem item = validateMenuOwnership(user, menuItemId);
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime cutoff = mealScheduleService.getBookingCutoff(targetDate, item.getMealType());
        if (LocalDateTime.now().isAfter(cutoff)) {
            throw new BookingDeadlineException("Cancellation deadline passed for this meal.");
        }

        MealBooking booking = bookingRepository.findByUserIdAndMenuItemIdAndDate(userId, menuItemId, targetDate)
                .orElse(MealBooking.builder()
                        .userId(userId)
                        .menuItemId(menuItemId)
                        .messId(user.getMessId())
                        .date(targetDate)
                        .mealType(item.getMealType())
                        .planType(user.getPlanType())
                        .build());

        if (booking.getStatus() == BookingStatus.CONSUMED) {
            throw new IllegalStateException("Consumed meals cannot be cancelled.");
        }
        if (booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new IllegalStateException("No-show meals cannot be cancelled.");
        }

        if (user.getPlanType() == PlanType.DAILY && booking.getId() == null) {
            throw new IllegalStateException("No booking found for this daily meal.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setPlanType(user.getPlanType());
        booking.setNotes(user.getPlanType() == PlanType.DAILY
                ? "Daily booking cancelled and refunded."
                : "Subscription meal opt-out.");
        bookingRepository.save(booking);

        cancellationRepository.findByUserIdAndMenuItemIdAndDate(userId, menuItemId, targetDate)
                .orElseGet(() -> cancellationRepository.save(MealCancellation.builder()
                        .userId(userId)
                        .menuItemId(menuItemId)
                        .messId(user.getMessId())
                        .date(targetDate)
                        .mealType(item.getMealType())
                        .planType(user.getPlanType())
                        .build()));

        if (user.getPlanType() == PlanType.DAILY && booking.getAmountCharged() != null && booking.getAmountCharged().signum() > 0) {
            walletService.refund(userId, booking.getAmountCharged(), booking.getId(), "Meal cancellation refund for " + item.getName());
        }

        log.info("Meal cancelled: user={} item={} date={} plan={}", userId, menuItemId, targetDate, user.getPlanType());
    }

    @Transactional
    public void cancelBooking(UUID userId, UUID bookingId) {
        MealBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId.toString()));
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: you cannot cancel another user's booking.");
        }
        cancelMeal(userId, booking.getMenuItemId(), booking.getDate());
    }

    public List<BookingDto> getTodayBookings(UUID userId) {
        return bookingRepository.findByUserIdAndDate(userId, LocalDate.now())
                .stream()
                .sorted(Comparator.comparing(MealBooking::getBookedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(b -> toDto(b, menuService.findById(b.getMenuItemId()).getName()))
                .toList();
    }

    public List<TodayMealStatusDto> getTodayMealStatuses(UUID userId) {
        User user = validateEligibleStudent(userId);
        LocalDate today = LocalDate.now();
        List<MenuItem> menuItems = menuService.getTodayMenuEntities(user.getMessId());
        Map<UUID, MealBooking> bookingMap = bookingRepository.findByUserIdAndDate(userId, today).stream()
                .collect(Collectors.toMap(MealBooking::getMenuItemId, Function.identity(), (left, right) -> left));
        Map<UUID, Attendance> attendanceMap = attendanceRepository.findByUserIdAndDateOrderByMarkedAtDesc(userId, today).stream()
                .collect(Collectors.toMap(Attendance::getMenuItemId, Function.identity(), (left, right) -> left));

        return menuItems.stream()
                .sorted(Comparator.comparing(MenuItem::getMealType))
                .map(item -> buildStatusDto(user, item, bookingMap.get(item.getId()), attendanceMap.get(item.getId()), today))
                .toList();
    }

    @Transactional
    public int markNoShowsForExpiredMeals() {
        int updated = 0;
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        for (MealBooking booking : bookingRepository.findDailyBookingsPendingConsumption(today)) {
            if (!attendanceRepository.existsByUserIdAndMenuItemIdAndDate(booking.getUserId(), booking.getMenuItemId(), booking.getDate())
                    && now.isAfter(mealScheduleService.getScanWindowEnd(booking.getDate(), booking.getMealType()))) {
                booking.setStatus(BookingStatus.NO_SHOW);
                booking.setNoShowAt(now);
                booking.setNotes("Automatically marked as no-show after meal window closed.");
                bookingRepository.save(booking);
                updated++;
            }
        }
        return updated;
    }

    private TodayMealStatusDto buildStatusDto(User user, MenuItem item, MealBooking booking, Attendance attendance, LocalDate today) {
        boolean consumed = attendance != null;
        boolean feedbackPending = attendance != null && feedbackRepository.findByAttendanceId(attendance.getId()).isEmpty();
        LocalDateTime cutoffAt = mealScheduleService.getBookingCutoff(today, item.getMealType());
        LocalDateTime qrActiveFrom = mealScheduleService.getScanWindowStart(today, item.getMealType());
        LocalDateTime qrActiveUntil = mealScheduleService.getScanWindowEnd(today, item.getMealType());
        boolean beforeCutoff = LocalDateTime.now().isBefore(cutoffAt);
        boolean withinScanWindow = mealScheduleService.isWithinScanWindow(LocalDateTime.now(), today, item.getMealType());

        boolean cancelled = booking != null && booking.getStatus() == BookingStatus.CANCELLED;
        boolean booked = booking != null && booking.getStatus() == BookingStatus.BOOKED;
        boolean noShow = booking != null && booking.getStatus() == BookingStatus.NO_SHOW;

        boolean canBook = user.getPlanType() == PlanType.DAILY
                ? !consumed && !booked && !noShow && beforeCutoff
                : cancelled && beforeCutoff;
        boolean canCancel = !consumed && beforeCutoff
                && ((user.getPlanType() == PlanType.DAILY && booked) || (user.getPlanType() != PlanType.DAILY && !cancelled));
        boolean canScan = !consumed && withinScanWindow
                && ((user.getPlanType() == PlanType.DAILY && booked) || (user.getPlanType() != PlanType.DAILY && !cancelled));

        String statusLabel;
        if (consumed) {
            statusLabel = "CONSUMED";
        } else if (noShow) {
            statusLabel = "NO_SHOW";
        } else if (cancelled) {
            statusLabel = "CANCELLED";
        } else if (user.getPlanType() == PlanType.DAILY && booked) {
            statusLabel = "BOOKED";
        } else if (user.getPlanType() == PlanType.DAILY) {
            statusLabel = "BOOKING_REQUIRED";
        } else {
            statusLabel = "READY_TO_SCAN";
        }

        LocalDateTime countdownTarget = canBook || canCancel ? cutoffAt : (withinScanWindow ? qrActiveUntil : qrActiveFrom);

        return TodayMealStatusDto.builder()
                .menuItemId(item.getId())
                .menuItemName(item.getName())
                .mealType(item.getMealType())
                .bookingStatus(booking != null ? booking.getStatus() : null)
                .consumed(consumed)
                .canBook(canBook)
                .canCancel(canCancel)
                .canScan(canScan)
                .feedbackPending(feedbackPending)
                .planType(user.getPlanType() != null ? user.getPlanType().name() : null)
                .statusLabel(statusLabel)
                .cutoffAt(cutoffAt)
                .qrActiveFrom(qrActiveFrom)
                .qrActiveUntil(qrActiveUntil)
                .countdownSeconds(mealScheduleService.secondsUntil(countdownTarget))
                .build();
    }

    private User validateEligibleStudent(UUID userId) {
        User user = userService.findById(userId);
        if (user.getMessId() == null) {
            throw new IllegalStateException("Join a mess before managing meals.");
        }
        if (user.getApprovalStatus() != com.smartmess.enums.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Your mess membership is not yet approved.");
        }
        return user;
    }

    private MenuItem validateMenuOwnership(User user, UUID menuItemId) {
        MenuItem item = menuService.findById(menuItemId);
        if (!item.getMessId().equals(user.getMessId())) {
            throw new SecurityException("This meal does not belong to your mess.");
        }
        return item;
    }

    private BookingDto toDto(MealBooking booking, String itemName) {
        return BookingDto.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .menuItemId(booking.getMenuItemId())
                .menuItemName(itemName)
                .messId(booking.getMessId())
                .date(booking.getDate())
                .mealType(booking.getMealType())
                .planType(booking.getPlanType())
                .status(booking.getStatus())
                .amountCharged(booking.getAmountCharged())
                .bookedAt(booking.getBookedAt())
                .cancelledAt(booking.getCancelledAt())
                .consumedAt(booking.getConsumedAt())
                .noShowAt(booking.getNoShowAt())
                .build();
    }
}
