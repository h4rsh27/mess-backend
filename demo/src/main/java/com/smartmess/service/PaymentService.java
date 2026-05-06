package com.smartmess.service;

import com.smartmess.dto.PaymentDto;
import com.smartmess.dto.PaymentSummaryDto;
import com.smartmess.entity.Payment;
import com.smartmess.entity.User;
import com.smartmess.enums.PaymentStatus;
import com.smartmess.enums.PlanType;
import com.smartmess.exception.ResourceNotFoundException;
import com.smartmess.repository.AttendanceRepository;
import com.smartmess.repository.MealCancellationRepository;
import com.smartmess.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MealCancellationRepository cancellationRepository;
    private final UserService userService;
    private final MessService messService;

    @Transactional
    public void generateMonthlyBillsForMess(UUID messId) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        var mess = messService.findById(messId);

        userService.getStudentsByMess(messId).forEach(studentDto -> {
            if (studentDto.getPlanType() == null || studentDto.getPlanType() == PlanType.DAILY) {
                return;
            }

            long consumedMeals = attendanceRepository.countByUserIdAndDateBetween(studentDto.getId(), from, to);
            long cancellations = cancellationRepository.findByUserIdAndDateBetween(studentDto.getId(), from, to).size();
            long extraMeals = 0;
            BigDecimal amount = switch (studentDto.getPlanType()) {
                case MONTHLY -> mess.getMonthlyPrice();
                case WEEKLY -> mess.getWeeklyPrice() != null
                        ? mess.getWeeklyPrice().multiply(BigDecimal.valueOf(4))
                        : mess.getMonthlyPrice();
                default -> BigDecimal.ZERO;
            };

            Payment payment = paymentRepository.findByUserIdAndMessIdAndMonthAndYear(studentDto.getId(), messId, month, year)
                    .orElse(Payment.builder()
                            .userId(studentDto.getId())
                            .messId(messId)
                            .month(month)
                            .year(year)
                            .planType(studentDto.getPlanType())
                            .status(PaymentStatus.UNPAID)
                            .build());

            payment.setAmount(amount);
            payment.setPlanType(studentDto.getPlanType());
            payment.setNotes(String.format("Meals consumed: %d | Cancellations: %d | Extra meals: %d",
                    consumedMeals, cancellations, extraMeals));
            paymentRepository.save(payment);
        });

        log.info("Generated subscription bills for mess {} month={}/{}", messId, month, year);
    }

    @Transactional
    public PaymentDto markAsPaid(UUID ownerId, UUID paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId.toString()));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDate.now());
        payment.setTransactionId(transactionId);
        return toDto(paymentRepository.save(payment));
    }

    public PaymentDto getStudentCurrentBill(UUID userId, UUID messId) {
        User user = userService.findById(userId);
        if (user.getPlanType() == PlanType.DAILY) {
            throw new ResourceNotFoundException("Payment", "current month", "Daily users use the prepaid wallet instead of monthly bills.");
        }
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        return paymentRepository.findByUserIdAndMessIdAndMonthAndYear(userId, messId, month, year)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "current month", userId.toString()));
    }

    public List<PaymentDto> getStudentPaymentHistory(UUID userId) {
        return paymentRepository.findByUserIdOrderByYearDescMonthDesc(userId)
                .stream().map(this::toDto).toList();
    }

    public List<PaymentDto> getMessPayments(UUID messId) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        return paymentRepository.findByMessIdAndMonthAndYear(messId, month, year)
                .stream().map(this::toDto).toList();
    }

    public PaymentSummaryDto getPaymentSummary(UUID messId) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        List<Payment> payments = paymentRepository.findByMessIdAndMonthAndYear(messId, month, year);
        long paid = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PAID).count();
        long unpaid = payments.stream().filter(p -> p.getStatus() == PaymentStatus.UNPAID).count();
        long partial = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PARTIAL).count();
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = payments.stream()
                .filter(p -> p.getStatus() != PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return PaymentSummaryDto.builder()
                .totalStudents(payments.size())
                .paidCount(paid)
                .unpaidCount(unpaid)
                .partialCount(partial)
                .totalRevenue(totalRevenue)
                .pendingRevenue(pending)
                .month(month)
                .year(year)
                .build();
    }

    private PaymentDto toDto(Payment payment) {
        User user = userService.findById(payment.getUserId());
        var mess = messService.findById(payment.getMessId());
        return PaymentDto.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .messId(payment.getMessId())
                .messName(mess.getName())
                .month(payment.getMonth())
                .year(payment.getYear())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .planType(payment.getPlanType())
                .paidAt(payment.getPaidAt())
                .transactionId(payment.getTransactionId())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
