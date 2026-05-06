package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.AnalyticsService;
import com.smartmess.service.MessService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Food popularity, attendance trends, revenue, wastage")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final MessService messService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsDto>> getAnalytics(
            @AuthenticationPrincipal UserDetails userDetails) {
        MessDto mess = messService.getMessByOwner(getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOwnerAnalytics(mess.getId())));
    }

    private java.util.UUID getUserId(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
