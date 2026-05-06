package com.smartmess.controller;

import com.smartmess.dto.*;
import com.smartmess.enums.MealType;
import com.smartmess.repository.UserRepository;
import com.smartmess.service.MenuService;
import com.smartmess.service.MessService;
import com.smartmess.service.QrService;
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
@Tag(name = "Menu", description = "Menu management and display")
public class MenuController {

    private final MenuService menuService;
    private final MessService messService;
    private final QrService qrService;
    private final UserRepository userRepository;

    @GetMapping("/api/menu/today/{messId}")
    @Operation(summary = "Get today's menu for a mess (public)")
    public ResponseEntity<ApiResponse<List<MenuItemDto>>> getTodayMenu(@PathVariable UUID messId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getTodayMenu(messId)));
    }

    @GetMapping("/api/menu/weekly/{messId}")
    @Operation(summary = "Get weekly menu for a mess")
    public ResponseEntity<ApiResponse<List<MenuItemDto>>> getWeeklyMenu(@PathVariable UUID messId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getWeeklyMenu(messId)));
    }

    @PostMapping("/api/owner/menu")
    @Operation(summary = "Owner adds a menu item")
    public ResponseEntity<ApiResponse<MenuItemDto>> createMenuItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateMenuItemRequest request) {
        UUID ownerId = getOwnerId(userDetails);
        MessDto mess = messService.getMessByOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success("Menu item added",
                menuService.createMenuItem(mess.getId(), request)));
    }

    @PutMapping("/api/owner/menu/{itemId}")
    public ResponseEntity<ApiResponse<MenuItemDto>> updateMenuItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID itemId,
            @Valid @RequestBody CreateMenuItemRequest request) {
        UUID ownerId = getOwnerId(userDetails);
        MessDto mess = messService.getMessByOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success("Menu item updated",
                menuService.updateMenuItem(itemId, mess.getId(), request)));
    }

    @DeleteMapping("/api/owner/menu/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID itemId) {
        UUID ownerId = getOwnerId(userDetails);
        MessDto mess = messService.getMessByOwner(ownerId);
        menuService.deleteMenuItem(itemId, mess.getId());
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted", null));
    }

    @GetMapping("/api/owner/menu/qr/{menuItemId}")
    @Operation(summary = "Generate QR code for meal attendance")
    public ResponseEntity<ApiResponse<String>> generateQr(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID menuItemId,
            @RequestParam MealType mealType) throws Exception {
        UUID ownerId = getOwnerId(userDetails);
        MessDto mess = messService.getMessByOwner(ownerId);
        String qrBase64 = qrService.generateQrCode(mess.getId(), menuItemId, mealType);
        return ResponseEntity.ok(ApiResponse.success("QR generated", qrBase64));
    }

    @GetMapping("/api/owner/menu/qr-details/{menuItemId}")
    @Operation(summary = "Generate rotating QR code with validity metadata")
    public ResponseEntity<ApiResponse<QrCodeDto>> generateQrDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID menuItemId,
            @RequestParam MealType mealType) throws Exception {
        UUID ownerId = getOwnerId(userDetails);
        MessDto mess = messService.getMessByOwner(ownerId);
        return ResponseEntity.ok(ApiResponse.success("QR generated",
                qrService.generateQrCodeDetails(mess.getId(), menuItemId, mealType)));
    }

    private UUID getOwnerId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
