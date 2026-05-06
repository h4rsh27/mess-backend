package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Platform-wide management")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/messes")
    public ResponseEntity<ApiResponse<List<MessDto>>> getAllMesses() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllMesses()));
    }

    @GetMapping("/owners")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllOwners() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllOwners()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
    }

    @PutMapping("/mess/{messId}/approve")
    public ResponseEntity<ApiResponse<MessDto>> approveMess(
            @PathVariable UUID messId,
            @RequestParam boolean approve) {
        return ResponseEntity.ok(ApiResponse.success(
                approve ? "Mess approved" : "Mess rejected",
                adminService.approveMess(messId, approve)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPlatformStats() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalMesses", adminService.getTotalMesses(),
                "totalUsers", adminService.getTotalUsers()
        )));
    }

    @PutMapping("/users/{userId}/active")
    public ResponseEntity<ApiResponse<UserDto>> setUserActive(
            @PathVariable UUID userId,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                active ? "User unblocked" : "User blocked",
                adminService.setUserActive(userId, active)));
    }
}
