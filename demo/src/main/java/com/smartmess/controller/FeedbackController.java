package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.FeedbackService;

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
@Tag(name = "Feedback", description = "Meal and mess ratings")
public class FeedbackController {

    private final FeedbackService feedbackService;

    private final UserRepository userRepository;

    @PostMapping("/student/feedback")
    public ResponseEntity<ApiResponse<FeedbackDto>> createFeedback(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateFeedbackRequest request) {
        UUID userId = getUserId(userDetails);
        UUID messId = getUser(userDetails).getMessId();
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted",
                feedbackService.createFeedback(userId, messId, request)));
    }

    @GetMapping("/feedback/{messId}")
    public ResponseEntity<ApiResponse<List<FeedbackDto>>> getMessFeedback(@PathVariable UUID messId) {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getMessFeedback(messId)));
    }

    @GetMapping("/student/feedback/my")
    public ResponseEntity<ApiResponse<List<FeedbackDto>>> getMyFeedback(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(feedbackService.getMyFeedback(getUserId(userDetails))));
    }

    private UUID getUserId(UserDetails ud) { return getUser(ud).getId(); }
    private com.smartmess.entity.User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
