package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.AttendanceService;
import com.smartmess.service.MessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Mark and track meal attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final MessService messService;
    private final UserRepository userRepository;

    @PostMapping("/student/attendance/mark")
    @Operation(summary = "Mark attendance (click-based or QR)")
    public ResponseEntity<ApiResponse<AttendanceDto>> markAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MarkAttendanceRequest request) {
        UUID userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Attendance marked",
                attendanceService.markAttendance(userId, request)));
    }

    @PostMapping("/student/attendance/sync")
    @Operation(summary = "Sync offline QR scans captured while the device was offline")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> syncAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody List<MarkAttendanceRequest> requests) {
        UUID userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Offline scans synced",
                attendanceService.syncOfflineAttendance(userId, requests)));
    }

    @GetMapping("/student/attendance/history")
    @Operation(summary = "Get student's attendance history")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getMyHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getMyHistory(userId)));
    }

    @GetMapping("/owner/attendance/report")
    @Operation(summary = "Owner gets monthly attendance report")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month) {
        UUID ownerId = getUserId(userDetails);
        MessDto mess = messService.getMessByOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getMonthlyReport(mess.getId(), year, month)));
    }

    private UUID getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
