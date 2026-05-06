package com.smartmess.controller;

import com.smartmess.dto.*;

import com.smartmess.repository.UserRepository;
import com.smartmess.service.MessService;
import com.smartmess.service.UserService;
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
@RequiredArgsConstructor
@Tag(name = "Mess & Students", description = "Mess CRUD, student join, approval")
public class MessController {

    private final MessService messService;
    private final UserService userService;
    private final UserRepository userRepository;

    // ===== PUBLIC =====
    @GetMapping("/api/mess/public")
    @Operation(summary = "List all active messes (public, for comparison)")
    public ResponseEntity<ApiResponse<List<MessDto>>> getPublicMesses() {
        return ResponseEntity.ok(ApiResponse.success(messService.getAllPublicMesses()));
    }

    @GetMapping("/api/mess/public/{messId}")
    public ResponseEntity<ApiResponse<MessDto>> getMessById(@PathVariable UUID messId) {
        return ResponseEntity.ok(ApiResponse.success(messService.getMessById(messId)));
    }

    // ===== OWNER =====
    @PostMapping("/api/owner/mess")
    @Operation(summary = "Owner creates their mess")
    public ResponseEntity<ApiResponse<MessDto>> createMess(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateMessRequest request) {
        UUID ownerId = getAuthUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Mess created", messService.createMess(ownerId, request)));
    }

    @PutMapping("/api/owner/mess/{messId}")
    public ResponseEntity<ApiResponse<MessDto>> updateMess(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID messId,
            @Valid @RequestBody CreateMessRequest request) {
        UUID ownerId = getAuthUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Mess updated", messService.updateMess(ownerId, messId, request)));
    }

    @GetMapping("/api/owner/mess")
    @Operation(summary = "Owner gets their mess details")
    public ResponseEntity<ApiResponse<MessDto>> getMyMess(@AuthenticationPrincipal UserDetails userDetails) {
        UUID ownerId = getAuthUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(messService.getMessByOwner(ownerId)));
    }

    @GetMapping("/api/owner/students")
    @Operation(summary = "Owner gets list of students in their mess")
    public ResponseEntity<ApiResponse<List<UserDto>>> getStudents(@AuthenticationPrincipal UserDetails userDetails) {
        UUID ownerId = getAuthUserId(userDetails);
        MessDto mess = messService.getMessByOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success(userService.getStudentsByMess(mess.getId())));
    }

    @PutMapping("/api/owner/students/{studentId}/approve")
    @Operation(summary = "Approve or reject student's mess join request")
    public ResponseEntity<ApiResponse<Void>> approveStudent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID studentId,
            @RequestParam boolean approve) {
        UUID ownerId = getAuthUserId(userDetails);
        userService.approveStudent(ownerId, studentId, approve);
        return ResponseEntity.ok(ApiResponse.success("Student " + (approve ? "approved" : "rejected"), null));
    }

    // ===== STUDENT =====
    @PostMapping("/api/student/join-mess/{messId}")
    @Operation(summary = "Student requests to join a mess")
    public ResponseEntity<ApiResponse<Void>> joinMess(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID messId) {
        UUID userId = getAuthUserId(userDetails);
        userService.joinMess(userId, messId);
        return ResponseEntity.ok(ApiResponse.success("Join request submitted. Awaiting owner approval.", null));
    }

    private UUID getAuthUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
