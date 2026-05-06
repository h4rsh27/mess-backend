package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.BookingService;
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
@RequestMapping("/api/student/booking")
@RequiredArgsConstructor
@Tag(name = "Meal Booking", description = "Pre-book meals (daily plan users)")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDto>> bookMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateBookingRequest request) {
        UUID userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Meal booked",
                bookingService.bookMeal(userId, request)));
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID bookingId) {
        bookingService.cancelBooking(getUserId(userDetails), bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", null));
    }

    @DeleteMapping("/cancel-meal")
    public ResponseEntity<ApiResponse<Void>> cancelMeal(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam UUID menuItemId,
            @RequestParam java.time.LocalDate date) {
        bookingService.cancelMeal(getUserId(userDetails), menuItemId, date);
        return ResponseEntity.ok(ApiResponse.success("Meal cancelled/opted-out", null));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<BookingDto>>> getTodayBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getTodayBookings(getUserId(userDetails))));
    }

    @GetMapping("/status/today")
    public ResponseEntity<ApiResponse<List<TodayMealStatusDto>>> getTodayMealStatuses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getTodayMealStatuses(getUserId(userDetails))));
    }

    private UUID getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
