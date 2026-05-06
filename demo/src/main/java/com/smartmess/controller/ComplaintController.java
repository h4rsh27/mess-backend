package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.ComplaintService;
import com.smartmess.service.MessService;
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
@Tag(name = "Complaints", description = "Raise and manage complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final MessService messService;
    private final UserRepository userRepository;

    @PostMapping("/student/complaints")
    public ResponseEntity<ApiResponse<ComplaintDto>> createComplaint(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateComplaintRequest request) {
        com.smartmess.entity.User user = getUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Complaint raised",
                complaintService.createComplaint(user.getId(), user.getMessId(), request)));
    }

    @GetMapping("/student/complaints/my")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getMyComplaints(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.getMyComplaints(getUserId(userDetails))));
    }

    @GetMapping("/owner/complaints")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getMessComplaints(
            @AuthenticationPrincipal UserDetails userDetails) {
        MessDto mess = messService.getMessByOwner(getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(complaintService.getMessComplaints(mess.getId())));
    }

    @PutMapping("/owner/complaints/{complaintId}/status")
    public ResponseEntity<ApiResponse<ComplaintDto>> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID complaintId,
            @Valid @RequestBody UpdateComplaintRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Complaint updated",
                complaintService.updateStatus(getUserId(userDetails), complaintId, request)));
    }

    private UUID getUserId(UserDetails ud) { return getUser(ud).getId(); }
    private com.smartmess.entity.User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
