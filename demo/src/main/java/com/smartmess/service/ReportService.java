package com.smartmess.service;

import com.smartmess.entity.Payment;
import com.smartmess.entity.User;

import com.smartmess.enums.Role;
import com.smartmess.repository.AttendanceRepository;
import com.smartmess.repository.PaymentRepository;
import com.smartmess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public byte[] generateAttendanceReportCsv(UUID messId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<User> students = userRepository.findByMessIdAndRole(messId, Role.ROLE_STUDENT);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println("Student Name,Email,Meals Attended,Month,Year");
            for (User student : students) {
                long meals = attendanceRepository.countByUserIdAndDateBetween(student.getId(), from, to);
                writer.printf("%s,%s,%d,%d,%d%n", student.getName(), student.getEmail(), meals, month, year);
            }
        }
        return baos.toByteArray();
    }

    public byte[] generatePaymentReportCsv(UUID messId, int year, int month) {
        List<Payment> payments = paymentRepository.findByMessIdAndMonthAndYear(messId, month, year);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(baos)) {
            writer.println("Student Name,Email,Amount,Status,Plan,Paid At,Transaction ID");
            for (Payment p : payments) {
                User user = userService.findById(p.getUserId());
                writer.printf("%s,%s,%.2f,%s,%s,%s,%s%n",
                        user.getName(), user.getEmail(), p.getAmount(),
                        p.getStatus(), p.getPlanType() != null ? p.getPlanType() : "",
                        p.getPaidAt() != null ? p.getPaidAt() : "Not Paid",
                        p.getTransactionId() != null ? p.getTransactionId() : "");
            }
        }
        return baos.toByteArray();
    }
}
