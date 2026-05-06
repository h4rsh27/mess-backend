package com.smartmess.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.smartmess.config.AppProperties;
import com.smartmess.dto.QrCodeDto;
import com.smartmess.entity.MenuItem;
import com.smartmess.entity.QrToken;
import com.smartmess.enums.MealType;
import com.smartmess.exception.InvalidQrTokenException;
import com.smartmess.repository.QrTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrService {

    private final QrTokenRepository qrTokenRepository;
    private final MenuService menuService;
    private final MealScheduleService mealScheduleService;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    @Transactional
    public String generateQrCode(UUID messId, UUID menuItemId, MealType mealType) throws Exception {
        return generateQrCodeDetails(messId, menuItemId, mealType).getQrImage();
    }

    @Transactional
    public QrCodeDto generateQrCodeDetails(UUID messId, UUID menuItemId, MealType mealType) throws Exception {
        MenuItem item = menuService.findById(menuItemId);
        if (!item.getMessId().equals(messId)) {
            throw new SecurityException("Access denied: menu item does not belong to this mess.");
        }

        LocalDate today = LocalDate.now();
        MealType resolvedMealType = mealType != null ? mealType : item.getMealType();
        if (item.getDate() != null && !item.getDate().equals(today)) {
            throw new IllegalStateException("QR codes can only be generated for today's meal.");
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime validFrom = mealScheduleService.getScanWindowStart(today, resolvedMealType);
        LocalDateTime effectiveStart = issuedAt.isAfter(validFrom) ? issuedAt : validFrom;
        LocalDateTime validUntil = effectiveStart.plusMinutes(appProperties.getQr().getRefreshMinutes());
        LocalDateTime scanWindowEnd = mealScheduleService.getScanWindowEnd(today, resolvedMealType);
        if (validUntil.isAfter(scanWindowEnd)) {
            validUntil = scanWindowEnd;
        }
        if (validUntil.isBefore(validFrom)) {
            validUntil = validFrom;
        }

        String nonce = UUID.randomUUID().toString().replace("-", "");
        String token = buildSignedToken(menuItemId, messId, today, resolvedMealType, issuedAt, validUntil, nonce);

        qrTokenRepository.deactivateActiveTokens(menuItemId, today, resolvedMealType);
        qrTokenRepository.save(QrToken.builder()
                .messId(messId)
                .menuItemId(menuItemId)
                .date(today)
                .mealType(resolvedMealType)
                .token(token)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .issuedAt(issuedAt)
                .refreshIntervalMinutes(appProperties.getQr().getRefreshMinutes())
                .nonce(nonce)
                .isActive(true)
                .build());

        log.info("Generated rotating QR for meal {} item {} valid until {}", resolvedMealType, menuItemId, validUntil);
        return QrCodeDto.builder()
                .menuItemId(menuItemId)
                .mealType(resolvedMealType)
                .qrImage(generateQrBase64(token))
                .issuedAt(issuedAt)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .refreshIntervalMinutes(appProperties.getQr().getRefreshMinutes())
                .build();
    }

    public ValidatedQrToken validateQrToken(String token, UUID expectedMenuItemId) {
        if (token == null || token.isBlank()) {
            throw new InvalidQrTokenException("QR token is required for attendance.");
        }

        TokenClaims claims = parseAndVerifyToken(token);
        QrToken qrToken = qrTokenRepository.findByTokenAndIsActiveTrue(token)
                .orElseThrow(() -> new InvalidQrTokenException("This QR code is no longer active. Please scan the current live code."));

        if (!qrToken.getNonce().equals(claims.nonce())) {
            throw new InvalidQrTokenException("QR token validation failed.");
        }
        if (expectedMenuItemId != null && !claims.menuItemId().equals(expectedMenuItemId)) {
            throw new InvalidQrTokenException("QR token does not match this meal.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(qrToken.getValidFrom()) || now.isAfter(qrToken.getValidUntil())) {
            throw new InvalidQrTokenException("This QR code has expired. Please scan the refreshed code.");
        }
        if (!mealScheduleService.isWithinScanWindow(now, claims.date(), claims.mealType())) {
            throw new InvalidQrTokenException("This QR is only valid during the active meal window.");
        }

        return new ValidatedQrToken(qrToken, claims.menuItemId(), claims.mealType(), claims.date());
    }

    private String buildSignedToken(UUID menuItemId, UUID messId, LocalDate date, MealType mealType,
                                    LocalDateTime issuedAt, LocalDateTime validUntil, String nonce) throws Exception {
        Map<String, Object> payload = Map.of(
                "menuItemId", menuItemId.toString(),
                "messId", messId.toString(),
                "date", date.toString(),
                "mealType", mealType.name(),
                "issuedAt", issuedAt.toEpochSecond(ZoneOffset.UTC),
                "expiresAt", validUntil.toEpochSecond(ZoneOffset.UTC),
                "nonce", nonce
        );
        String payloadJson = objectMapper.writeValueAsString(payload);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return encodedPayload + "." + sign(encodedPayload);
    }

    private TokenClaims parseAndVerifyToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new InvalidQrTokenException("Malformed QR token.");
            }

            String encodedPayload = parts[0];
            String expectedSignature = sign(encodedPayload);
            if (!expectedSignature.equals(parts[1])) {
                throw new InvalidQrTokenException("Invalid QR token signature.");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            Map<?, ?> claims = objectMapper.readValue(payloadJson, Map.class);

            LocalDateTime expiresAt = LocalDateTime.ofEpochSecond(
                    ((Number) claims.get("expiresAt")).longValue(), 0, ZoneOffset.UTC);
            if (LocalDateTime.now().isAfter(expiresAt)) {
                throw new InvalidQrTokenException("This QR code has expired. Please scan the refreshed code.");
            }

            return new TokenClaims(
                    UUID.fromString((String) claims.get("menuItemId")),
                    UUID.fromString((String) claims.get("messId")),
                    LocalDate.parse((String) claims.get("date")),
                    MealType.valueOf((String) claims.get("mealType")),
                    (String) claims.get("nonce")
            );
        } catch (InvalidQrTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidQrTokenException("Unable to validate the scanned QR token.");
        }
    }

    private String sign(String encodedPayload) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(appProperties.getQr().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmac.init(key);
        byte[] digest = hmac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private String generateQrBase64(String content) throws Exception {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300,
                Map.of(EncodeHintType.MARGIN, 1));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public record ValidatedQrToken(QrToken qrToken, UUID menuItemId, MealType mealType, LocalDate date) {
    }

    private record TokenClaims(UUID menuItemId, UUID messId, LocalDate date, MealType mealType, String nonce) {
    }
}
