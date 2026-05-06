package com.smartmess.service;

import com.smartmess.dto.ComplaintDto;
import com.smartmess.dto.CreateComplaintRequest;
import com.smartmess.dto.UpdateComplaintRequest;
import com.smartmess.entity.Complaint;
import com.smartmess.entity.User;
import com.smartmess.enums.ComplaintStatus;
import com.smartmess.exception.ResourceNotFoundException;
import com.smartmess.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public ComplaintDto createComplaint(UUID userId, UUID messId, CreateComplaintRequest request) {
        Complaint complaint = Complaint.builder()
                .userId(userId)
                .messId(messId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ComplaintStatus.OPEN)
                .build();
        return toDto(complaintRepository.save(complaint));
    }

    @Transactional
    public ComplaintDto updateStatus(UUID ownerId, UUID complaintId, UpdateComplaintRequest request) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", "id", complaintId.toString()));
        complaint.setStatus(request.getStatus());
        complaint.setResolutionNote(request.getResolutionNote());
        if (request.getStatus() == ComplaintStatus.RESOLVED) {
            complaint.setResolvedBy(ownerId);
            complaint.setResolvedAt(LocalDateTime.now());
        }
        Complaint saved = complaintRepository.save(complaint);

        // Notify student
        notificationService.sendComplaintUpdateNotification(complaint.getUserId(), complaint.getTitle(), request.getStatus());

        return toDto(saved);
    }

    public List<ComplaintDto> getMyComplaints(UUID userId) {
        return complaintRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ComplaintDto> getMessComplaints(UUID messId) {
        return complaintRepository.findByMessIdOrderByCreatedAtDesc(messId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private ComplaintDto toDto(Complaint c) {
        User user = userService.findById(c.getUserId());
        return ComplaintDto.builder()
                .id(c.getId()).userId(c.getUserId()).userName(user.getName())
                .messId(c.getMessId()).title(c.getTitle()).description(c.getDescription())
                .status(c.getStatus()).resolutionNote(c.getResolutionNote())
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).resolvedAt(c.getResolvedAt())
                .build();
    }
}
