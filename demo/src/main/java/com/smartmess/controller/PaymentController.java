package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.entity.User;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.MessService;
import com.smartmess.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments & Billing", description = "Bills, payments, revenue tracking")
public class PaymentController {

    private final PaymentService paymentService;
    private final MessService messService;
    private final UserRepository userRepository;

    @GetMapping("/student/payment/bill")
    @Operation(summary = "Student views current month's bill")
    public ResponseEntity<ApiResponse<PaymentDto>> getMyBill(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getStudentCurrentBill(user.getId(), user.getMessId())));
    }

    @GetMapping("/student/payment/history")
    @Operation(summary = "Student views full payment history")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getMyHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getStudentPaymentHistory(getUserId(userDetails))));
    }

    @GetMapping("/owner/payment/summary")
    @Operation(summary = "Owner gets payment summary for current month")
    public ResponseEntity<ApiResponse<PaymentSummaryDto>> getSummary(@AuthenticationPrincipal UserDetails userDetails) {
        MessDto mess = messService.getMessByOwner(getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentSummary(mess.getId())));
    }

    @GetMapping("/owner/payment/list")
    @Operation(summary = "Owner gets all payments for current month")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getMessPayments(@AuthenticationPrincipal UserDetails userDetails) {
        MessDto mess = messService.getMessByOwner(getUserId(userDetails));
        return ResponseEntity.ok(ApiResponse.success(paymentService.getMessPayments(mess.getId())));
    }

    @PutMapping("/owner/payment/{paymentId}/mark-paid")
    @Operation(summary = "Owner marks a payment as paid")
    public ResponseEntity<ApiResponse<PaymentDto>> markPaid(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID paymentId,
            @RequestParam(required = false) String transactionId) {
        return ResponseEntity.ok(ApiResponse.success("Marked as paid",
                paymentService.markAsPaid(getUserId(userDetails), paymentId, transactionId)));
    }

    @PostMapping("/owner/payment/generate-bills")
    @Operation(summary = "Manually trigger monthly bill generation for owner's mess")
    public ResponseEntity<ApiResponse<Void>> generateBills(@AuthenticationPrincipal UserDetails userDetails) {
        MessDto mess = messService.getMessByOwner(getUserId(userDetails));
        paymentService.generateMonthlyBillsForMess(mess.getId());
        return ResponseEntity.ok(ApiResponse.success("Bills generated", null));
    }

    private UUID getUserId(UserDetails userDetails) {
        return getUser(userDetails).getId();
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
