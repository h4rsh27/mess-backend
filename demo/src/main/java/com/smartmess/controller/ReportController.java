package com.smartmess.controller;

import com.smartmess.repository.UserRepository;
import com.smartmess.service.MessService;
import com.smartmess.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/owner/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Export CSV reports")
public class ReportController {

    private final ReportService reportService;
    private final MessService messService;
    private final UserRepository userRepository;

    @GetMapping("/attendance.csv")
    public ResponseEntity<byte[]> downloadAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month) {
        UUID ownerId = getUserId(userDetails);
        UUID messId = messService.getMessByOwner(ownerId).getId();
        byte[] csv = reportService.generateAttendanceReportCsv(messId, year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + year + "-" + month + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/revenue.csv")
    public ResponseEntity<byte[]> downloadRevenue(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month) {
        UUID ownerId = getUserId(userDetails);
        UUID messId = messService.getMessByOwner(ownerId).getId();
        byte[] csv = reportService.generatePaymentReportCsv(messId, year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=revenue-" + year + "-" + month + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private UUID getUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
