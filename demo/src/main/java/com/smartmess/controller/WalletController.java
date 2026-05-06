package com.smartmess.controller;

import com.smartmess.dto.ApiResponse;
import com.smartmess.dto.TopUpWalletRequest;
import com.smartmess.dto.WalletDto;
import com.smartmess.dto.WalletTransactionDto;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletDto>> getWallet(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(getUserId(userDetails))));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionDto>>> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                walletService.getRecentTransactions(getUserId(userDetails))));
    }

    @PostMapping("/top-up")
    public ResponseEntity<ApiResponse<WalletDto>> topUp(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TopUpWalletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Wallet topped up",
                walletService.topUp(getUserId(userDetails), request.getAmount(), request.getReference())));
    }

    private UUID getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
