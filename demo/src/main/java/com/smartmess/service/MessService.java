package com.smartmess.service;

import com.smartmess.dto.CreateMessRequest;
import com.smartmess.dto.MessDto;
import com.smartmess.entity.Mess;
import com.smartmess.entity.User;
import com.smartmess.exception.ResourceNotFoundException;
import com.smartmess.repository.MessRepository;
import com.smartmess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessService {

    private final MessRepository messRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessDto createMess(UUID ownerId, CreateMessRequest request) {
        Mess mess = Mess.builder()
                .name(request.getName())
                .location(request.getLocation())
                .description(request.getDescription())
                .monthlyPrice(request.getMonthlyPrice())
                .dailyPrice(request.getDailyPrice())
                .weeklyPrice(request.getWeeklyPrice())
                .imageUrl(request.getImageUrl())
                .ownerId(ownerId)
                .rating(BigDecimal.ZERO)
                .isActive(true)
                .isApproved(true)
                .build();
        mess = messRepository.save(mess);
        log.info("Mess created: {} by owner {}", mess.getName(), ownerId);
        return toMessDto(mess);
    }

    @Transactional
    public MessDto updateMess(UUID ownerId, UUID messId, CreateMessRequest request) {
        Mess mess = getMessOwnedBy(messId, ownerId);
        mess.setName(request.getName());
        mess.setLocation(request.getLocation());
        mess.setDescription(request.getDescription());
        mess.setMonthlyPrice(request.getMonthlyPrice());
        mess.setDailyPrice(request.getDailyPrice());
        if (request.getWeeklyPrice() != null) mess.setWeeklyPrice(request.getWeeklyPrice());
        if (request.getImageUrl() != null) mess.setImageUrl(request.getImageUrl());
        return toMessDto(messRepository.save(mess));
    }

    public MessDto getMessById(UUID messId) {
        return toMessDto(findById(messId));
    }

    public MessDto getMessByOwner(UUID ownerId) {
        return messRepository.findByOwnerId(ownerId)
                .map(this::toMessDto)
                .orElseThrow(() -> new ResourceNotFoundException("Mess", "ownerId", ownerId.toString()));
    }

    public List<MessDto> getAllPublicMesses() {
        return messRepository.findAllActiveOrderByRating()
                .stream().map(this::toMessDto).collect(Collectors.toList());
    }

    @Transactional
    public void updateRating(UUID messId, Double avgRating) {
        Mess mess = findById(messId);
        mess.setRating(BigDecimal.valueOf(avgRating));
        messRepository.save(mess);
    }

    public Mess findById(UUID messId) {
        return messRepository.findById(messId)
                .orElseThrow(() -> new ResourceNotFoundException("Mess", "id", messId.toString()));
    }

    public Mess getMessOwnedBy(UUID messId, UUID ownerId) {
        Mess mess = findById(messId);
        if (!mess.getOwnerId().equals(ownerId)) {
            throw new SecurityException("Access denied: you do not own this mess");
        }
        return mess;
    }

    private MessDto toMessDto(Mess mess) {
        long totalStudents = userRepository.countByMessIdAndRole(mess.getId(),
                com.smartmess.enums.Role.ROLE_STUDENT);
        String ownerName = userRepository.findById(mess.getOwnerId())
                .map(User::getName).orElse("Unknown");
        return MessDto.builder()
                .id(mess.getId())
                .name(mess.getName())
                .location(mess.getLocation())
                .description(mess.getDescription())
                .monthlyPrice(mess.getMonthlyPrice())
                .dailyPrice(mess.getDailyPrice())
                .weeklyPrice(mess.getWeeklyPrice())
                .rating(mess.getRating())
                .isActive(mess.isActive())
                .isApproved(mess.isApproved())
                .imageUrl(mess.getImageUrl())
                .ownerId(mess.getOwnerId())
                .ownerName(ownerName)
                .totalStudents((int) totalStudents)
                .createdAt(mess.getCreatedAt())
                .build();
    }
}
